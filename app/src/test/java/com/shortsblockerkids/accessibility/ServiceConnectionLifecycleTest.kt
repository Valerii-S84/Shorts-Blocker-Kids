package com.shortsblockerkids.accessibility

import com.shortsblockerkids.application.port.MonotonicTimeProvider
import com.shortsblockerkids.platform.accessibility.CurrentWindowRecheckScheduler
import com.shortsblockerkids.platform.accessibility.ServiceConnectionLifecycle
import com.shortsblockerkids.platform.accessibility.TemporaryAllowDeadlineScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceConnectionLifecycleTest {
    @Test
    fun replacementCancelsOldCollectorAndClosesRouterBeforeStartingNewConnection() =
        runBlocking {
            val lifecycle = ServiceConnectionLifecycle()
            val settings = MutableStateFlow(0)
            val observations = mutableListOf<String>()
            val transitions = mutableListOf<String>()
            lateinit var oldCollector: Job
            var oldOverlayVisible = true

            lifecycle.replace {
                transitions += "old started"
                oldCollector = launch { settings.collect { observations += "old:$it" } }
                val cleanup: () -> Unit = {
                    oldCollector.cancel()
                    oldOverlayVisible = false
                    transitions += "old closed"
                }
                cleanup
            }
            yield()

            try {
                lifecycle.replace {
                    assertTrue(oldCollector.isCancelled)
                    assertEquals(false, oldOverlayVisible)
                    transitions += "new started"
                    val collector = launch { settings.collect { observations += "new:$it" } }
                    val cleanup: () -> Unit = { collector.cancel() }
                    cleanup
                }
                yield()
                settings.value = 1
                yield()

                assertEquals(listOf("old started", "old closed", "new started"), transitions)
                assertEquals(listOf("old:0", "new:0", "new:1"), observations)
            } finally {
                lifecycle.close()
            }
        }

    @Test
    fun reconnectAfterInterruptionRearmsInitialScanAndDeadlineWithoutOldCallbacks() =
        runBlocking {
            val lifecycle = ServiceConnectionLifecycle()
            val old = DeferredCallbacks(this)
            val replacement = DeferredCallbacks(this)
            lifecycle.replace(old::start)
            yield()
            old.cancelCallbacks()

            try {
                lifecycle.replace(replacement::start)
                yield()
                old.wakeAll()
                yield()
                assertEquals(listOf(1, 0, 0), old.counts())
                assertEquals(listOf(1, 0, 0), replacement.counts())

                replacement.wakeAll()
                yield()
                assertEquals(listOf(2, 1, 1), replacement.counts())
                assertEquals(listOf(1, 0, 0), old.counts())
            } finally {
                lifecycle.close()
            }
        }

    @Test
    fun closeCancelsLatestConnectionExactlyOnce() =
        runBlocking {
            val lifecycle = ServiceConnectionLifecycle()
            val jobs = mutableListOf<Job>()
            val cleanupCalls = mutableListOf<Int>()
            repeat(2) { connection ->
                lifecycle.replace {
                    val job = launch { awaitCancellation() }
                    jobs += job
                    val cleanup: () -> Unit = {
                        cleanupCalls += connection
                        job.cancel()
                    }
                    cleanup
                }
                yield()
            }
            lifecycle.close()
            lifecycle.close()

            assertEquals(listOf(0, 1), cleanupCalls)
            assertTrue(jobs.all { it.isCancelled })
        }

    private class DeferredCallbacks(
        private val scope: CoroutineScope,
    ) {
        private var nowMillis = 0L
        private var scans = 0
        private var deadlines = 0
        private var pinChecks = 0
        private val scanWake = CompletableDeferred<Unit>()
        private val deadlineWake = CompletableDeferred<Unit>()
        private val pinWake = CompletableDeferred<Unit>()
        private var pinJob: Job? = null
        private val scanScheduler =
            CurrentWindowRecheckScheduler(
                scope = scope,
                tryRoute = {
                    scans++
                    true
                },
                awaitDelay = { scanWake.await() },
            )
        private val deadlineScheduler =
            TemporaryAllowDeadlineScheduler(
                scope = scope,
                monotonicTimeProvider = MonotonicTimeProvider { nowMillis },
                onDeadline = { deadlines++ },
                awaitDelay = { deadlineWake.await() },
            )

        fun start(): () -> Unit {
            scanScheduler.recheckNow()
            scanScheduler.schedule(500L)
            deadlineScheduler.schedule(1_000L)
            pinJob =
                scope.launch {
                    pinWake.await()
                    pinChecks++
                }
            return ::cancelCallbacks
        }

        fun cancelCallbacks() {
            scanScheduler.cancel()
            deadlineScheduler.cancel()
            pinJob?.cancel()
        }

        fun wakeAll() {
            nowMillis = 1_000L
            scanWake.complete(Unit)
            deadlineWake.complete(Unit)
            pinWake.complete(Unit)
        }

        fun counts(): List<Int> = listOf(scans, deadlines, pinChecks)
    }
}
