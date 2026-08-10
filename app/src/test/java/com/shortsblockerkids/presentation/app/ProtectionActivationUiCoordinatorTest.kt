package com.shortsblockerkids.presentation.app

import com.shortsblockerkids.application.protection.ProtectionActivationIntent
import com.shortsblockerkids.application.protection.ProtectionActivationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionActivationUiCoordinatorTest {
    @Test
    fun completedConfigurationRefreshesStateOnlyAfterNewActivation() =
        runBlocking {
            val activated = environment(ProtectionActivationResult.ACTIVATED)

            activated.coordinator.completeCurrentConfiguration()

            assertEquals(
                listOf(ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION),
                activated.recordedIntents,
            )
            assertEquals(1, activated.stateChangeCount)

            listOf(
                ProtectionActivationResult.ALREADY_STARTED,
                ProtectionActivationResult.PREREQUISITES_NOT_MET,
            ).forEach { result ->
                val unchanged = environment(result)

                unchanged.coordinator.completeCurrentConfiguration()

                assertEquals(0, unchanged.stateChangeCount)
            }
        }

    @Test
    fun acceptedEnablementRefreshesStateAndCompletesNavigation() =
        runBlocking {
            listOf(
                ProtectionActivationResult.ACTIVATED,
                ProtectionActivationResult.ALREADY_STARTED,
            ).forEach { result ->
                val environment = environment(result)
                var completionCount = 0

                environment.coordinator.enableProtection {
                    completionCount += 1
                }

                assertEquals(listOf(ProtectionActivationIntent.ENABLE_PROTECTION), environment.recordedIntents)
                assertEquals(1, environment.stateChangeCount)
                assertEquals(1, completionCount)
            }
        }

    @Test
    fun rejectedEnablementRefreshesStateWithoutCompletingNavigation() =
        runBlocking {
            val environment = environment(ProtectionActivationResult.PREREQUISITES_NOT_MET)
            var completionCount = 0

            environment.coordinator.enableProtection {
                completionCount += 1
            }

            assertEquals(1, environment.stateChangeCount)
            assertEquals(0, completionCount)
        }

    @Test
    fun dashboardEnablementUsesTheSameCoveredFlowWithoutNavigationCallback() =
        runBlocking {
            val environment = environment(ProtectionActivationResult.ACTIVATED)

            environment.coordinator.enableProtection()

            assertEquals(listOf(ProtectionActivationIntent.ENABLE_PROTECTION), environment.recordedIntents)
            assertEquals(1, environment.stateChangeCount)
        }

    private fun environment(result: ProtectionActivationResult): TestEnvironment = TestEnvironment(result)

    private class TestEnvironment(
        result: ProtectionActivationResult,
    ) {
        val recordedIntents = mutableListOf<ProtectionActivationIntent>()
        var stateChangeCount = 0
            private set
        val coordinator =
            ProtectionActivationUiCoordinator(
                recordActivation = { intent ->
                    recordedIntents += intent
                    result
                },
                onStateChanged = { stateChangeCount += 1 },
            )
    }
}
