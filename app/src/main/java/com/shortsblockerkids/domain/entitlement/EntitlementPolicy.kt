package com.shortsblockerkids.domain.entitlement

object EntitlementPolicy {
    fun freeTestState(
        snapshot: EntitlementSnapshot,
        nowMillis: Long,
    ): FreeTestState {
        val startedAt = snapshot.freeTestStartedAtMillis ?: return FreeTestState.NOT_STARTED
        return if (
            FreeTestPolicy.isActive(
                startedAtMillis = startedAt,
                durationDays = snapshot.freeTestDurationDays,
                nowMillis = nowMillis,
            )
        ) {
            FreeTestState.ACTIVE
        } else {
            FreeTestState.EXPIRED
        }
    }

    fun freeTestDaysRemaining(
        snapshot: EntitlementSnapshot,
        nowMillis: Long,
    ): Int? =
        FreeTestPolicy.daysRemaining(
            startedAtMillis = snapshot.freeTestStartedAtMillis,
            durationDays = snapshot.freeTestDurationDays,
            nowMillis = nowMillis,
        )

    fun hasPaidEntitlement(snapshot: EntitlementSnapshot): Boolean = snapshot.isPaidProtectionAllowed

    fun hasProtectionEntitlement(
        snapshot: EntitlementSnapshot,
        nowMillis: Long,
    ): Boolean =
        freeTestState(snapshot, nowMillis) == FreeTestState.ACTIVE ||
            hasPaidEntitlement(snapshot)
}
