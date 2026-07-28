package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.port.ProtectionActivationStore
import com.shortsblockerkids.application.port.TimeProvider

class RecordSuccessfulProtectionActivationUseCase(
    private val timeProvider: TimeProvider,
    private val protectionActivationStore: ProtectionActivationStore,
) {
    suspend operator fun invoke() {
        val nowMillis = timeProvider.currentTimeMillis()
        protectionActivationStore.recordSuccessfulProtectionActivation(nowMillis)
    }
}
