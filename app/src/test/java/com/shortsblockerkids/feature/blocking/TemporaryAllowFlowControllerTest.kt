package com.shortsblockerkids.feature.blocking

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TemporaryAllowFlowControllerTest {
    @Test
    fun durationSelectionStoresUnlockAndReturnsToForegroundApp() =
        runBlocking {
            val storedDurations = mutableListOf<Int>()
            val controller =
                TemporaryAllowFlowController { minutes ->
                    storedDurations += minutes
                }

            val completion = controller.selectDuration(5)

            assertEquals(listOf(5), storedDurations)
            assertEquals(TemporaryAllowCompletion.ReturnToForegroundApp, completion)
        }

    @Test
    fun allTemporaryAllowDurationsReturnToForegroundApp() =
        runBlocking {
            val storedDurations = mutableListOf<Int>()
            val controller =
                TemporaryAllowFlowController { minutes ->
                    storedDurations += minutes
                }

            val completions = listOf(5, 10, 15).map { controller.selectDuration(it) }

            assertEquals(listOf(5, 10, 15), storedDurations)
            assertEquals(
                listOf(
                    TemporaryAllowCompletion.ReturnToForegroundApp,
                    TemporaryAllowCompletion.ReturnToForegroundApp,
                    TemporaryAllowCompletion.ReturnToForegroundApp,
                ),
                completions,
            )
        }

    @Test
    fun cancelDoesNotStoreUnlockAndReturnsToForegroundApp() {
        val storedDurations = mutableListOf<Int>()
        val controller =
            TemporaryAllowFlowController { minutes ->
                storedDurations += minutes
            }

        val completion = controller.cancel()

        assertEquals(emptyList<Int>(), storedDurations)
        assertEquals(TemporaryAllowCompletion.ReturnToForegroundApp, completion)
    }

    @Test
    fun unsupportedDurationIsRejectedAndNotStored() {
        val storedDurations = mutableListOf<Int>()
        val controller =
            TemporaryAllowFlowController { minutes ->
                storedDurations += minutes
            }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                controller.selectDuration(30)
            }
        }
        assertEquals(emptyList<Int>(), storedDurations)
    }
}
