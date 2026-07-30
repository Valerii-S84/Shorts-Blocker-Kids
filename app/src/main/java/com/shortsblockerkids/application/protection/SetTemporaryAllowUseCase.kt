package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.protection.TemporaryAllowDuration

class SetTemporaryAllowUseCase(
    private val timeProvider: TimeProvider,
    private val temporaryAllowStore: TemporaryAllowStore,
) {
    suspend operator fun invoke(duration: TemporaryAllowDuration) {
        val nowMillis = timeProvider.currentTimeMillis()
        val allowUntilMillis = nowMillis + duration.minutes * MILLIS_PER_MINUTE
        temporaryAllowStore.setTemporaryAllowUntil(allowUntilMillis)
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
