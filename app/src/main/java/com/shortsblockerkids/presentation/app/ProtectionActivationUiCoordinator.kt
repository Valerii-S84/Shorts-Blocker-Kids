package com.shortsblockerkids.presentation.app

import com.shortsblockerkids.application.protection.ProtectionActivationIntent
import com.shortsblockerkids.application.protection.ProtectionActivationResult

internal class ProtectionActivationUiCoordinator(
    private val recordActivation: suspend (ProtectionActivationIntent) -> ProtectionActivationResult,
    private val onStateChanged: () -> Unit,
) {
    suspend fun completeCurrentConfiguration() {
        val result =
            recordActivation(
                ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION,
            )
        if (result == ProtectionActivationResult.ACTIVATED) {
            onStateChanged()
        }
    }

    suspend fun enableProtection(onActivationAccepted: () -> Unit = {}) {
        val result = recordActivation(ProtectionActivationIntent.ENABLE_PROTECTION)
        onStateChanged()
        if (result != ProtectionActivationResult.PREREQUISITES_NOT_MET) {
            onActivationAccepted()
        }
    }
}
