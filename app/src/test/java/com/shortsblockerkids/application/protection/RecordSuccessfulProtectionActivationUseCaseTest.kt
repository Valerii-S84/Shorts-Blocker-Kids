package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.port.ProtectionActivationStore
import com.shortsblockerkids.application.port.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordSuccessfulProtectionActivationUseCaseTest {
    @Test
    fun readsTimeOnceAndPassesExactTimestampToStore() =
        runBlocking {
            val timeProvider = RecordingTimeProvider(timestampMillis = 12_345L)
            val activationStore = RecordingProtectionActivationStore()
            val useCase =
                RecordSuccessfulProtectionActivationUseCase(
                    timeProvider = timeProvider,
                    protectionActivationStore = activationStore,
                )

            useCase()

            assertEquals(1, timeProvider.readCount)
            assertEquals(listOf(12_345L), activationStore.recordedTimestamps)
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

    private class RecordingProtectionActivationStore : ProtectionActivationStore {
        val recordedTimestamps = mutableListOf<Long>()

        override suspend fun recordSuccessfulProtectionActivation(nowMillis: Long) {
            recordedTimestamps += nowMillis
        }
    }
}
