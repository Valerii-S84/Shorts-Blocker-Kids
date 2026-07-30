package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.application.port.TimeProvider

class ClearExpiredTemporaryAllowUseCase(
    private val timeProvider: TimeProvider,
    private val temporaryAllowStore: TemporaryAllowStore,
) {
    suspend operator fun invoke(): Boolean {
        val nowMillis = timeProvider.currentTimeMillis()
        return temporaryAllowStore.removeTemporaryAllowIf { allowUntilMillis ->
            allowUntilMillis <= nowMillis
        }
    }
}
