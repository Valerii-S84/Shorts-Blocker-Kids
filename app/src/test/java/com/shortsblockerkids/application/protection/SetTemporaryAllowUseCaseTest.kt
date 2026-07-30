package com.shortsblockerkids.application.protection

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
                    TemporaryAllowDuration.FIVE_MINUTES to 301_234L,
                    TemporaryAllowDuration.TEN_MINUTES to 601_234L,
                    TemporaryAllowDuration.FIFTEEN_MINUTES to 901_234L,
                )

            expectedExpiries.forEach { (duration, expectedExpiry) ->
                useCase(duration)
                assertEquals(expectedExpiry, temporaryAllowStore.storedExpiries.last())
            }

            assertEquals(expectedExpiries.size, timeProvider.readCount)
        }

    @Test
    fun durationRepresentationContainsOnlyFiveTenAndFifteenMinutes() {
        assertEquals(
            listOf(5, 10, 15),
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

        override suspend fun setTemporaryAllowUntil(allowUntilMillis: Long?) {
            storedExpiries += allowUntilMillis
        }

        override suspend fun removeTemporaryAllowIf(shouldRemove: (Long) -> Boolean): Boolean = false
    }
}
