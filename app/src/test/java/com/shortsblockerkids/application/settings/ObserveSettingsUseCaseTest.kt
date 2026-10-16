package com.shortsblockerkids.application.settings

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.SettingsStatePort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ObserveSettingsUseCaseTest {
    @Test(expected = IllegalArgumentException::class)
    fun zeroInitialReadTimeoutIsRejected() {
        ObserveSettingsUseCase(SettingsStatePort { flow { emit(AppSettingsSnapshot()) } }, initialReadTimeoutMillis = 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroRetryDelayIsRejected() {
        ObserveSettingsUseCase(SettingsStatePort { flow { emit(AppSettingsSnapshot()) } }, retryDelaysMillis = listOf(1L, 0L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroRecoveryDelayIsRejected() {
        ObserveSettingsUseCase(SettingsStatePort { flow { emit(AppSettingsSnapshot()) } }, recoveryDelayMillis = 0L)
    }

    @Test
    @OptIn(FlowPreview::class)
    fun slowConsumerReceivesEverySnapshotInOrderWithoutStorageFailure() =
        runBlocking {
            withTimeout(5_000L) {
                val first = AppSettingsSnapshot()
                val second = first.copy(billingEntitlementStateName = "ACTIVE")
                val firstEmitted = CompletableDeferred<Unit>()
                val source =
                    ObserveSettingsUseCase(
                        SettingsStatePort {
                            flow {
                                emit(first)
                                firstEmitted.complete(Unit)
                                emit(second)
                            }
                        },
                    )
                val states = source().buffer(0).produceIn(this)
                try {
                    assertEquals(SettingsReadState.Loading, states.receive())
                    firstEmitted.await()
                    yield()
                    assertEquals(SettingsReadState.Ready(first), states.receive())
                    yield()
                    assertEquals(SettingsReadState.Ready(second), states.receive())
                    assertTrue(states.receiveCatching().isClosed)
                } finally {
                    states.cancel()
                }
            }
        }

    @Test
    fun transientInitialFailureRecoversWithoutSuccessfulDefaults() =
        runBlocking {
            var reads = 0
            val expected = AppSettingsSnapshot(billingEntitlementStateName = "ACTIVE")
            val states =
                observer {
                    if (reads++ == 0) throw IOException("read failed")
                    emit(expected)
                }().toList()

            assertEquals(2, reads)
            assertEquals(SettingsReadState.Loading, states.first())
            assertEquals(SettingsReadState.Unavailable(null, true), states[1])
            assertEquals(listOf(SettingsReadState.Ready(expected)), states.filterIsInstance<SettingsReadState.Ready>())
        }

    @Test
    fun failureAfterSuccessfulSnapshotIsExplicitAndCollectorRecovers() =
        runBlocking {
            var reads = 0
            val first = AppSettingsSnapshot(billingEntitlementStateName = "ACTIVE")
            val recovered = first.copy(billingEntitlementStateName = "INACTIVE")
            val states =
                observer {
                    if (reads++ == 0) {
                        emit(first)
                        throw IOException("collector failed")
                    }
                    emit(recovered)
                }().toList()

            assertTrue(states.contains(SettingsReadState.Unavailable(first, true)))
            assertEquals(SettingsReadState.Ready(recovered), states.last())
        }

    @Test
    fun permanentFailureHasExactlyThreeAttemptsAndNoDefaultSnapshot() =
        runBlocking {
            var reads = 0
            val states =
                observer {
                    reads++
                    throw IOException("read failed")
                }().take(4).toList()

            assertEquals(3, reads)
            assertEquals(SettingsReadState.Unavailable(null, false), states.last())
            assertTrue(states.none { it is SettingsReadState.Ready })
        }

    @Test
    fun hungInitialReadTimesOutAndCancelsEachAttempt() =
        runBlocking {
            var reads = 0
            var cancellations = 0
            val states =
                withTimeout(2_000L) {
                    observer {
                        reads++
                        try {
                            awaitCancellation()
                        } finally {
                            cancellations++
                        }
                    }().take(4).toList()
                }

            assertEquals(3, reads)
            assertEquals(reads, cancellations)
            assertEquals(SettingsReadState.Unavailable(null, false), states.last())
        }

    @Test
    fun lifecycleCancellationCancelsReadWithoutRetryOrFailureState() =
        runBlocking {
            val entered = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            var reads = 0
            val states = mutableListOf<SettingsReadState>()
            val job =
                launch {
                    observer {
                        reads++
                        entered.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled.complete(Unit)
                        }
                    }().toList(states)
                }
            entered.await()
            job.cancelAndJoin()

            assertTrue(cancelled.isCompleted)
            assertEquals(1, reads)
            assertFalse(states.any { it is SettingsReadState.Unavailable })
        }

    @Test
    fun lifecycleCancellationDuringBackoffPreventsRetry() =
        runBlocking {
            var reads = 0
            val unavailable = CompletableDeferred<Unit>()
            val observer =
                ObserveSettingsUseCase(
                    SettingsStatePort {
                        flow {
                            reads++
                            throw IOException("read failed")
                        }
                    },
                    retryDelaysMillis = listOf(60_000L),
                )
            val job =
                launch {
                    observer().collect {
                        if (it is SettingsReadState.Unavailable) unavailable.complete(Unit)
                    }
                }
            withTimeout(2_000L) { unavailable.await() }
            job.cancelAndJoin()
            assertEquals(1, reads)
        }

    @Test
    fun healthyIdleCollectorHasNoReadTimeout() =
        runBlocking {
            val initial = AppSettingsSnapshot()
            val second = initial.copy(billingEntitlementStateName = "ACTIVE")
            val observer =
                observer {
                    emit(initial)
                    kotlinx.coroutines.delay(150L)
                    emit(second)
                }
            val states = observer().toList()
            assertTrue(states.none { it is SettingsReadState.Unavailable })
            assertEquals(SettingsReadState.Ready(second), states.last())
        }

    @Test
    fun exhaustedRetriesRecoverWithoutRestartingObserver() =
        runBlocking {
            var reads = 0
            val recovered = AppSettingsSnapshot(billingEntitlementStateName = "ACTIVE")
            val states =
                observer {
                    if (reads++ < 3) throw IOException("storage temporarily unavailable")
                    emit(recovered)
                }().toList()

            assertEquals(4, reads)
            assertTrue(states.contains(SettingsReadState.Unavailable(null, false)))
            assertEquals(SettingsReadState.Ready(recovered), states.last())
            assertEquals(1, states.filterIsInstance<SettingsReadState.Ready>().size)
        }

    @Test
    fun recoveryAfterExhaustionKeepsOnlySessionSuccessfulSnapshot() =
        runBlocking {
            var reads = 0
            val prior = AppSettingsSnapshot(billingEntitlementStateName = "ACTIVE")
            val recovered = prior.copy(billingEntitlementStateName = "INACTIVE")
            val states =
                observer {
                    when (reads++) {
                        0 -> {
                            emit(prior)
                            throw IOException("lost storage")
                        }
                        1, 2 -> throw IOException("still unavailable")
                        else -> emit(recovered)
                    }
                }().toList()

            assertEquals(SettingsReadState.Unavailable(prior, false), states[4])
            assertEquals(SettingsReadState.Ready(recovered), states.last())
            assertTrue(
                states
                    .filterIsInstance<SettingsReadState.Unavailable>()
                    .all { it.lastSuccessfulSnapshot == prior },
            )
        }

    @Test
    fun lifecycleCancellationDuringExhaustedCooldownPreventsAnotherRead() =
        runBlocking {
            var reads = 0
            val exhausted = CompletableDeferred<Unit>()
            val observer =
                ObserveSettingsUseCase(
                    SettingsStatePort {
                        flow {
                            reads++
                            throw IOException("read failed")
                        }
                    },
                    retryDelaysMillis = emptyList(),
                    recoveryDelayMillis = 60_000L,
                )
            val job =
                launch {
                    observer().collect {
                        if (it == SettingsReadState.Unavailable(null, false)) exhausted.complete(Unit)
                    }
                }
            withTimeout(2_000L) { exhausted.await() }
            job.cancelAndJoin()

            assertEquals(1, reads)
        }

    private fun observer(block: suspend kotlinx.coroutines.flow.FlowCollector<AppSettingsSnapshot>.() -> Unit) =
        ObserveSettingsUseCase(
            SettingsStatePort { flow(block) },
            initialReadTimeoutMillis = 30L,
            retryDelaysMillis = listOf(1L, 2L),
            recoveryDelayMillis = 1L,
        )
}
