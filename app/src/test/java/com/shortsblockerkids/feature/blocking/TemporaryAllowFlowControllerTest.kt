package com.shortsblockerkids.feature.blocking

import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.application.protection.SetTemporaryAllowUseCase
import com.shortsblockerkids.domain.protection.TemporaryAllowDuration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TemporaryAllowFlowControllerTest {
    @Test
    fun durationSelectionDelegatesToUseCaseAndReturnsToForegroundApp() =
        runBlocking {
            val timeProvider = RecordingTimeProvider(timestampMillis = 1_000L)
            val temporaryAllowStore = RecordingTemporaryAllowStore()
            val controller = createController(timeProvider, temporaryAllowStore)

            val completion = controller.selectDuration(TemporaryAllowDuration.FIVE_MINUTES)

            assertEquals(listOf(301_000L), temporaryAllowStore.storedExpiries)
            assertEquals(1, timeProvider.readCount)
            assertEquals(TemporaryAllowCompletion.ReturnToForegroundApp, completion)
        }

    @Test
    fun cancelDoesNotInvokeUseCaseAndReturnsToForegroundApp() {
        val timeProvider = RecordingTimeProvider(timestampMillis = 1_000L)
        val temporaryAllowStore = RecordingTemporaryAllowStore()
        val controller = createController(timeProvider, temporaryAllowStore)

        val completion = controller.cancel()

        assertEquals(emptyList<Long>(), temporaryAllowStore.storedExpiries)
        assertEquals(0, timeProvider.readCount)
        assertEquals(TemporaryAllowCompletion.ReturnToForegroundApp, completion)
    }

    private fun createController(
        timeProvider: TimeProvider,
        temporaryAllowStore: TemporaryAllowStore,
    ): TemporaryAllowFlowController =
        TemporaryAllowFlowController(
            SetTemporaryAllowUseCase(
                timeProvider = timeProvider,
                temporaryAllowStore = temporaryAllowStore,
            ),
        )

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
