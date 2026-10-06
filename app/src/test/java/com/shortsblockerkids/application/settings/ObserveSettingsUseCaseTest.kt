package com.shortsblockerkids.application.settings

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.SettingsStatePort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ObserveSettingsUseCaseTest {
    @Test
    fun transientInitialFailureRecoversWithoutSuccessfulDefaults() = runBlocking {
        var reads = 0
        val expected = AppSettingsSnapshot(billingEntitlementStateName = "ACTIVE")
        val states = observer {
            if (reads++ == 0) throw IOException("read failed")
            emit(expected)
        }().toList()

        assertEquals(2, reads)
        assertEquals(SettingsReadState.Loading, states.first())
        assertEquals(SettingsReadState.Unavailable(null, true), states[1])
        assertEquals(listOf(SettingsReadState.Ready(expected)), states.filterIsInstance<SettingsReadState.Ready>())
    }

    @Test
    fun failureAfterSuccessfulSnapshotIsExplicitAndCollectorRecovers() = runBlocking {
        var reads = 0
        val first = AppSettingsSnapshot(billingEntitlementStateName = "ACTIVE")
        val recovered = first.copy(billingEntitlementStateName = "EXPIRED")
        val states = observer {
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
    fun permanentFailureHasExactlyThreeAttemptsAndNoDefaultSnapshot() = runBlocking {
        var reads = 0
        val states = observer {
            reads++
            throw IOException("read failed")
        }().toList()

        assertEquals(3, reads)
        assertEquals(SettingsReadState.Unavailable(null, false), states.last())
        assertTrue(states.none { it is SettingsReadState.Ready })
    }

    @Test
    fun hungInitialReadTimesOutAndCancelsEachAttempt() = runBlocking {
        var reads = 0
        var cancellations = 0
        val states = withTimeout(2_000L) {
            observer {
                reads++
                try {
                    awaitCancellation()
                } finally {
                    cancellations++
                }
            }().toList()
        }

        assertEquals(3, reads)
        assertEquals(reads, cancellations)
        assertEquals(SettingsReadState.Unavailable(null, false), states.last())
    }

    @Test
    fun lifecycleCancellationCancelsReadWithoutRetryOrFailureState() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        var reads = 0
        val states = mutableListOf<SettingsReadState>()
        val job = launch {
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
    fun lifecycleCancellationDuringBackoffPreventsRetry() = runBlocking {
        var reads = 0
        val unavailable = CompletableDeferred<Unit>()
        val observer = ObserveSettingsUseCase(
            SettingsStatePort { flow { reads++; throw IOException("read failed") } },
            retryDelaysMillis = listOf(60_000L),
        )
        val job = launch {
            observer().collect {
                if (it is SettingsReadState.Unavailable) unavailable.complete(Unit)
            }
        }
        withTimeout(2_000L) { unavailable.await() }
        job.cancelAndJoin()
        assertEquals(1, reads)
    }

    @Test
    fun healthyIdleCollectorHasNoReadTimeout() = runBlocking {
        val initial = AppSettingsSnapshot()
        val second = initial.copy(billingEntitlementStateName = "ACTIVE")
        val observer = observer {
            emit(initial)
            kotlinx.coroutines.delay(150L)
            emit(second)
        }
        val states = observer().toList()
        assertTrue(states.none { it is SettingsReadState.Unavailable })
        assertEquals(SettingsReadState.Ready(second), states.last())
    }

    private fun observer(block: suspend kotlinx.coroutines.flow.FlowCollector<AppSettingsSnapshot>.() -> Unit) =
        ObserveSettingsUseCase(
            SettingsStatePort { flow(block) },
            initialReadTimeoutMillis = 30L,
            retryDelaysMillis = listOf(1L, 2L),
        )
}
