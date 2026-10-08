package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.application.port.PinLockoutClock
import com.shortsblockerkids.domain.protection.TemporaryAllowDuration

class SetTemporaryAllowUseCase(
    private val timeProvider: TimeProvider,
    private val temporaryAllowStore: TemporaryAllowStore,
    private val deadlineClock: PinLockoutClock? = null,
) {
    suspend operator fun invoke(duration: TemporaryAllowDuration) {
        val nowMillis = timeProvider.currentTimeMillis()
        val allowUntilMillis = nowMillis + duration.minutes * MILLIS_PER_MINUTE
        val clock = deadlineClock?.read()
        if (clock == null) {
            temporaryAllowStore.setTemporaryAllowUntil(allowUntilMillis)
        } else {
            temporaryAllowStore.setTemporaryAllowDeadline(
                allowUntilMillis,
                clock.elapsedRealtimeMillis + duration.minutes * MILLIS_PER_MINUTE,
                clock.bootCount,
            )
        }
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
