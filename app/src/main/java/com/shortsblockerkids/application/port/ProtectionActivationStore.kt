package com.shortsblockerkids.application.port

import com.shortsblockerkids.application.model.AppSettingsSnapshot

fun interface ProtectionActivationStore {
    suspend fun completeProtectionActivation(
        decision: (AppSettingsSnapshot) -> ProtectionActivationOperation,
    ): ProtectionActivationOperation
}

sealed interface ProtectionActivationOperation {
    data class Record(
        val nowMillis: Long,
    ) : ProtectionActivationOperation

    data object AlreadyStarted : ProtectionActivationOperation

    data object PrerequisitesNotMet : ProtectionActivationOperation
}
