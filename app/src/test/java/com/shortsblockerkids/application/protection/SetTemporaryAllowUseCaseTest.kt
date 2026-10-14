package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.port.PinLockoutClock
import com.shortsblockerkids.application.port.PinLockoutTime
import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.protection.TemporaryAllowDuration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SetTemporaryAllowUseCaseTest {
    @Test
    fun supportedDurationsStoreExactAbsoluteExpiryUsingInjectedTime() =
        runBlocking {
            val timeProvider = RecordingTimeProvider(timestampMillis = 1_234L)
            val temporaryAllowStore = RecordingTemporaryAllowStore()
            val useCase =
                SetTemporaryAllowUseCase(
                    timeProvider = timeProvider,
                    temporaryAllowStore = temporaryAllowStore,
                )
            val expectedExpiries =
                listOf(
                    TemporaryAllowDuration.FIFTEEN_MINUTES to 901_234L,
                    TemporaryAllowDuration.THIRTY_MINUTES to 1_801_234L,
                    TemporaryAllowDuration.SIXTY_MINUTES to 3_601_234L,
                )

            expectedExpiries.forEach { (duration, expectedExpiry) ->
                useCase(duration)
                assertEquals(expectedExpiry, temporaryAllowStore.storedExpiries.last())
            }

            assertEquals(expectedExpiries.size, timeProvider.readCount)
        }

    @Test
    fun supportedDurationsStoreExactWallAndMonotonicDeadlinesUsingInjectedClocks() =
        runBlocking {
            val timeProvider = RecordingTimeProvider(timestampMillis = 1_234L)
            val temporaryAllowStore = RecordingTemporaryAllowStore()
            val useCase =
                SetTemporaryAllowUseCase(
                    timeProvider = timeProvider,
                    temporaryAllowStore = temporaryAllowStore,
                    deadlineClock = PinLockoutClock { PinLockoutTime(elapsedRealtimeMillis = 5_678L, bootCount = 7) },
                )
            val expectedDeadlines =
                listOf(
                    TemporaryAllowDuration.FIFTEEN_MINUTES to Triple(901_234L, 905_678L, 7),
                    TemporaryAllowDuration.THIRTY_MINUTES to Triple(1_801_234L, 1_805_678L, 7),
                    TemporaryAllowDuration.SIXTY_MINUTES to Triple(3_601_234L, 3_605_678L, 7),
                )

            expectedDeadlines.forEach { (duration, expectedDeadline) ->
                useCase(duration)
                assertEquals(expectedDeadline, temporaryAllowStore.storedDeadlines.last())
            }

            assertEquals(expectedDeadlines.size, timeProvider.readCount)
            assertEquals(emptyList<Long?>(), temporaryAllowStore.storedExpiries)
        }

    @Test
    fun durationRepresentationContainsOnlyFifteenThirtyAndSixtyMinutes() {
        assertEquals(
            listOf(15, 30, 60),
            TemporaryAllowDuration.entries.map { it.minutes },
        )
    }

    private class RecordingTimeProvider(
        private val timestampMillis: Long,
    ) : TimeProvider {
        var readCount: Int = 0
            private set

        override fun currentTimeMillis(): Long {
            readCount += 1
            return timestampMillis
        }
    }

    private class RecordingTemporaryAllowStore : TemporaryAllowStore {
        val storedExpiries = mutableListOf<Long?>()
        val storedDeadlines = mutableListOf<Triple<Long, Long, Int>>()

        override suspend fun setTemporaryAllowUntil(allowUntilMillis: Long?) {
            storedExpiries += allowUntilMillis
        }

        override suspend fun setTemporaryAllowDeadline(
            allowUntilMillis: Long,
            elapsedRealtimeDeadlineMillis: Long,
            bootCount: Int,
        ) {
            storedDeadlines += Triple(allowUntilMillis, elapsedRealtimeDeadlineMillis, bootCount)
        }

        override suspend fun removeTemporaryAllowIf(shouldRemove: (Long) -> Boolean): Boolean = false
    }
}
