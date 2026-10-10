package com.shortsblockerkids.accessibility

import com.shortsblockerkids.platform.accessibility.CurrentWindowRecheckScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentWindowRecheckSchedulerTest {
    @Test
    fun initialUnavailableRootRetriesWithoutEvent() =
        runBlocking {
            val fixture = Fixture(this)
            fixture.scheduler.recheckNow()
            yield()

            assertEquals(1, fixture.attempts)
            assertEquals(listOf(500L), fixture.delays)

            fixture.rootAvailable = true
            fixture.wake()
            yield()
            assertEquals(2, fixture.attempts)
            assertEquals(listOf(500L), fixture.delays)
        }

    @Test
    fun repeatedUnavailableRootRetriesEvery500MillisUntilSuccess() =
        runBlocking {
            val fixture = Fixture(this)
            fixture.scheduler.recheckNow()
            yield()
            repeat(3) {
                fixture.wake()
                yield()
                yield()
            }

            assertEquals(4, fixture.attempts)
            assertEquals(listOf(500L, 500L, 500L, 500L), fixture.delays)

            fixture.rootAvailable = true
            fixture.wake()
            yield()
            assertEquals(5, fixture.attempts)
            assertEquals(4, fixture.delays.size)
        }

    @Test
    fun successfulImmediateRecheckCancelsPendingRetry() =
        runBlocking {
            val fixture = Fixture(this)
            fixture.scheduler.recheckNow()
            yield()

            fixture.rootAvailable = true
            fixture.scheduler.recheckNow()
            fixture.wake()
            yield()

            assertEquals(2, fixture.attempts)
            assertEquals(listOf(500L), fixture.delays)
        }

    @Test
    fun newerScheduledRequestReplacesPendingCallback() =
        runBlocking {
            val fixture = Fixture(this)
            fixture.rootAvailable = true
            fixture.scheduler.schedule(400L)
            yield()
            val obsoleteWake = fixture.pending.last()
            fixture.scheduler.schedule(100L)
            yield()

            obsoleteWake.complete(Unit)
            yield()
            assertEquals(0, fixture.attempts)
            assertEquals(listOf(400L, 100L), fixture.delays)

            fixture.wake()
            yield()
            assertEquals(1, fixture.attempts)
        }

    @Test
    fun cancelPreventsPendingRetryCallback() =
        runBlocking {
            val fixture = Fixture(this)
            fixture.scheduler.recheckNow()
            yield()

            fixture.scheduler.cancel()
            fixture.wake()
            yield()

            assertEquals(1, fixture.attempts)
            assertEquals(listOf(500L), fixture.delays)
        }

    @Test
    fun parentScopeCancellationStopsRetry() =
        runBlocking {
            val scope = CoroutineScope(coroutineContext + Job(coroutineContext[Job]))
            val fixture = Fixture(scope)
            fixture.scheduler.recheckNow()
            yield()

            scope.cancel()
            fixture.wake()
            yield()

            assertEquals(1, fixture.attempts)
            assertEquals(listOf(500L), fixture.delays)
        }

    private class Fixture(
        scope: CoroutineScope,
    ) {
        var rootAvailable = false
        var attempts = 0
        val delays = mutableListOf<Long>()
        val pending = mutableListOf<CompletableDeferred<Unit>>()
        val scheduler =
            CurrentWindowRecheckScheduler(
                scope = scope,
                tryRoute = {
                    attempts++
                    rootAvailable
                },
                awaitDelay = { millis ->
                    delays += millis
                    val wake = CompletableDeferred<Unit>()
                    pending += wake
                    wake.await()
                },
            )

        fun wake() {
            pending.last().complete(Unit)
        }
    }
}
