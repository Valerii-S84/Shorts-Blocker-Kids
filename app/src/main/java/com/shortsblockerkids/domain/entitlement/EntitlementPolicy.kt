package com.shortsblockerkids.domain.entitlement

object EntitlementPolicy {
    const val PAID_OFFLINE_GRACE_MILLIS = 72L * 60L * 60L * 1_000L

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

    fun hasPaidEntitlement(
        snapshot: EntitlementSnapshot,
        nowMillis: Long,
    ): Boolean {
        val verifiedAt = snapshot.paidLastVerifiedAtMillis ?: return false
        if (verifiedAt > nowMillis || !snapshot.isPaidProtectionAllowed) {
            return false
        }
        val offlineGraceUntil = verifiedAt + PAID_OFFLINE_GRACE_MILLIS
        val activeUntil = snapshot.paidActiveUntilMillis ?: offlineGraceUntil
        return nowMillis <= activeUntil && nowMillis <= offlineGraceUntil
    }

    fun hasProtectionEntitlement(
        snapshot: EntitlementSnapshot,
        nowMillis: Long,
    ): Boolean =
        freeTestState(snapshot, nowMillis) == FreeTestState.ACTIVE ||
            hasPaidEntitlement(snapshot, nowMillis)
}
