package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.application.port.PinLockoutClock

class ClearExpiredTemporaryAllowUseCase(
    private val timeProvider: TimeProvider,
    private val temporaryAllowStore: TemporaryAllowStore,
    private val deadlineClock: PinLockoutClock? = null,
) {
    suspend operator fun invoke(): Boolean {
        val nowMillis = timeProvider.currentTimeMillis()
        val clock = deadlineClock?.read()
        if (clock != null) {
            return temporaryAllowStore.removeTemporaryAllowDeadlineIf { _, elapsedUntil, bootCount ->
                elapsedUntil == null || bootCount != clock.bootCount || elapsedUntil <= clock.elapsedRealtimeMillis
            }
        }
        return temporaryAllowStore.removeTemporaryAllowIf { allowUntilMillis ->
            allowUntilMillis <= nowMillis
        }
    }
}
