package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.AccessibilityServiceStatusPort
import com.shortsblockerkids.application.port.ProtectionActivationOperation
import com.shortsblockerkids.application.port.ProtectionActivationStore
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordSuccessfulProtectionActivationUseCaseTest {
    @Test
    fun allPrerequisitesActivateWithOneExactTimestamp() =
        runBlocking {
            val environment = environment()

            val result =
                environment.useCase(
                    ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION,
                )

            assertEquals(ProtectionActivationResult.ACTIVATED, result)
            assertEquals(1, environment.timeProvider.readCount)
            assertEquals(listOf(NOW_MILLIS), environment.activationStore.recordedTimestamps)
        }

    @Test
    fun missingAccessibilityDoesNotWriteActivation() =
        runBlocking {
            environment(isAccessibilityServiceEnabled = false)
                .assertNoActivation(ProtectionActivationResult.PREREQUISITES_NOT_MET)
        }

    @Test
    fun disabledProtectionDoesNotWriteActivation() =
        runBlocking {
            val settings =
                eligibleSettings().copy(
                    protectionConfiguration =
                        eligibleSettings().protectionConfiguration.copy(isEnabled = false),
                )

            environment(settings = settings)
                .assertNoActivation(ProtectionActivationResult.PREREQUISITES_NOT_MET)
        }

    @Test
    fun missingDisclosureDoesNotWriteActivation() =
        runBlocking {
            val settings =
                eligibleSettings().copy(
                    protectionConfiguration =
                        eligibleSettings()
                            .protectionConfiguration
                            .copy(isAccessibilityDisclosureAccepted = false),
                )

            environment(settings = settings)
                .assertNoActivation(ProtectionActivationResult.PREREQUISITES_NOT_MET)
        }

    @Test
    fun missingPinDoesNotWriteActivation() =
        runBlocking {
            val settings =
                eligibleSettings().copy(
                    protectionConfiguration =
                        eligibleSettings().protectionConfiguration.copy(isPinConfigured = false),
                )

            environment(settings = settings)
                .assertNoActivation(ProtectionActivationResult.PREREQUISITES_NOT_MET)
        }

    @Test
    fun repeatedIntentDoesNotReadClockOrWriteActivation() =
        runBlocking {
            val settings =
                eligibleSettings().copy(
                    entitlement = EntitlementSnapshot(freeTestStartedAtMillis = 1_000L),
                )

            environment(settings = settings)
                .assertNoActivation(ProtectionActivationResult.ALREADY_STARTED)
        }

    @Test
    fun repeatedIntentWithMissingPrerequisiteIsRejected() =
        runBlocking {
            val settings =
                eligibleSettings().copy(
                    entitlement = EntitlementSnapshot(freeTestStartedAtMillis = 1_000L),
                )

            environment(
                settings = settings,
                isAccessibilityServiceEnabled = false,
            ).assertNoActivation(ProtectionActivationResult.PREREQUISITES_NOT_MET)
        }

    @Test
    fun enableProtectionIntentSuppliesTheProtectionPrerequisite() =
        runBlocking {
            val settings =
                eligibleSettings().copy(
                    protectionConfiguration =
                        eligibleSettings().protectionConfiguration.copy(isEnabled = false),
                )
            val environment = environment(settings = settings)

            val result =
                environment.useCase(ProtectionActivationIntent.ENABLE_PROTECTION)

            assertEquals(ProtectionActivationResult.ACTIVATED, result)
            assertEquals(1, environment.timeProvider.readCount)
            assertEquals(listOf(NOW_MILLIS), environment.activationStore.recordedTimestamps)
        }

    private fun environment(
        settings: AppSettingsSnapshot = eligibleSettings(),
        isAccessibilityServiceEnabled: Boolean = true,
    ): TestEnvironment {
        val timeProvider = RecordingTimeProvider(NOW_MILLIS)
        val activationStore = RecordingProtectionActivationStore(settings)
        val useCase =
            RecordSuccessfulProtectionActivationUseCase(
                timeProvider = timeProvider,
                accessibilityServiceStatusPort =
                    AccessibilityServiceStatusPort { isAccessibilityServiceEnabled },
                protectionActivationStore = activationStore,
            )
        return TestEnvironment(
            useCase = useCase,
            timeProvider = timeProvider,
            activationStore = activationStore,
        )
    }

    private fun eligibleSettings(): AppSettingsSnapshot =
        AppSettingsSnapshot(
            protectionConfiguration =
                ProtectionConfiguration(
                    isEnabled = true,
                    isAccessibilityDisclosureAccepted = true,
                    isPinConfigured = true,
                ),
        )

    private data class TestEnvironment(
        val useCase: RecordSuccessfulProtectionActivationUseCase,
        val timeProvider: RecordingTimeProvider,
        val activationStore: RecordingProtectionActivationStore,
    ) {
        suspend fun assertNoActivation(expectedResult: ProtectionActivationResult) {
            val result =
                useCase(ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION)

            assertEquals(expectedResult, result)
            assertEquals(0, timeProvider.readCount)
            assertTrue(activationStore.recordedTimestamps.isEmpty())
        }
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

    private class RecordingProtectionActivationStore(
        private val settings: AppSettingsSnapshot,
    ) : ProtectionActivationStore {
        val recordedTimestamps = mutableListOf<Long>()

        override suspend fun completeProtectionActivation(
            decision: (AppSettingsSnapshot) -> ProtectionActivationOperation,
        ): ProtectionActivationOperation {
            val operation = decision(settings)
            if (operation is ProtectionActivationOperation.Record) {
                recordedTimestamps += operation.nowMillis
            }
            return operation
        }
    }

    private companion object {
        const val NOW_MILLIS = 12_345L
    }
}
