package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.AccessibilityServiceStatusPort
import com.shortsblockerkids.application.port.ProtectionActivationOperation
import com.shortsblockerkids.application.port.ProtectionActivationStore
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.protection.ProtectionActivationPolicy

class RecordSuccessfulProtectionActivationUseCase(
    private val timeProvider: TimeProvider,
    private val accessibilityServiceStatusPort: AccessibilityServiceStatusPort,
    private val protectionActivationStore: ProtectionActivationStore,
) {
    suspend operator fun invoke(intent: ProtectionActivationIntent): ProtectionActivationResult {
        val operation =
            protectionActivationStore.completeProtectionActivation { settings ->
                activationOperation(settings, intent)
            }
        return when (operation) {
            is ProtectionActivationOperation.Record -> ProtectionActivationResult.ACTIVATED
            ProtectionActivationOperation.AlreadyStarted ->
                ProtectionActivationResult.ALREADY_STARTED
            ProtectionActivationOperation.PrerequisitesNotMet ->
                ProtectionActivationResult.PREREQUISITES_NOT_MET
        }
    }

    private fun activationOperation(
        settings: AppSettingsSnapshot,
        intent: ProtectionActivationIntent,
    ): ProtectionActivationOperation {
        val isAccessibilityServiceEnabled =
            accessibilityServiceStatusPort.isAccessibilityServiceEnabled()
        val isFreeTestAlreadyStarted = settings.entitlement.freeTestStartedAtMillis != null
        val shouldStartFreeTest =
            shouldStartFreeTest(
                settings = settings,
                isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                isFreeTestAlreadyStarted = isFreeTestAlreadyStarted,
                intent = intent,
            )
        if (!shouldStartFreeTest) {
            val otherwiseEligible =
                shouldStartFreeTest(
                    settings = settings,
                    isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                    isFreeTestAlreadyStarted = false,
                    intent = intent,
                )
            return if (isFreeTestAlreadyStarted && otherwiseEligible) {
                ProtectionActivationOperation.AlreadyStarted
            } else {
                ProtectionActivationOperation.PrerequisitesNotMet
            }
        }

        return ProtectionActivationOperation.Record(timeProvider.currentTimeMillis())
    }

    private fun shouldStartFreeTest(
        settings: AppSettingsSnapshot,
        isAccessibilityServiceEnabled: Boolean,
        isFreeTestAlreadyStarted: Boolean,
        intent: ProtectionActivationIntent,
    ): Boolean {
        val configuration = settings.protectionConfiguration
        return ProtectionActivationPolicy.shouldStartFreeTest(
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
            isProtectionEnabled =
                configuration.isEnabled || intent == ProtectionActivationIntent.ENABLE_PROTECTION,
            isAccessibilityDisclosureAccepted =
                configuration.isAccessibilityDisclosureAccepted,
            isPinConfigured = configuration.isPinConfigured,
            isFreeTestAlreadyStarted = isFreeTestAlreadyStarted,
        )
    }
}
