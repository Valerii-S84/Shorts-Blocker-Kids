package com.shortsblockerkids.core.storage

import com.shortsblockerkids.core.entitlement.FreeTestPolicy
import com.shortsblockerkids.core.model.EntitlementState
import com.shortsblockerkids.core.model.ProtectionMode

data class AppSettings(
    val protectionEnabled: Boolean = true,
    val accessibilityDisclosureAccepted: Boolean = false,
    val selectedMode: ProtectionMode = ProtectionMode.BLOCK_SHORTS,
    val temporaryAllowUntil: Long? = null,
    val freeTestStartedAt: Long? = null,
    val freeTestDurationDays: Int = FreeTestPolicy.DEFAULT_DURATION_DAYS,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val pinHashVersion: Int = 1,
    val failedPinAttempts: Int = 0,
    val pinLockoutUntil: Long? = null,
) {
    val isPinCreated: Boolean
        get() = !pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()

    fun isTemporarilyAllowed(nowMillis: Long): Boolean = activeTemporaryAllowUntil(nowMillis) != null

    fun activeTemporaryAllowUntil(nowMillis: Long): Long? {
        val allowUntil = temporaryAllowUntil ?: return null
        return allowUntil.takeIf { it > nowMillis }
    }

    fun hasExpiredTemporaryAllow(nowMillis: Long): Boolean {
        val allowUntil = temporaryAllowUntil ?: return false
        return allowUntil <= nowMillis
    }

    fun freeTestState(nowMillis: Long): EntitlementState {
        val startedAt = freeTestStartedAt ?: return EntitlementState.FREE_TEST_NOT_STARTED
        return if (FreeTestPolicy.isActive(startedAt, freeTestDurationDays, nowMillis)) {
            EntitlementState.FREE_TEST_ACTIVE
        } else {
            EntitlementState.FREE_TEST_EXPIRED
        }
    }

    fun freeTestDaysRemaining(nowMillis: Long): Int? =
        FreeTestPolicy.daysRemaining(
            startedAtMillis = freeTestStartedAt,
            durationDays = freeTestDurationDays,
            nowMillis = nowMillis,
        )

    fun canProtect(nowMillis: Long): Boolean =
        protectionEnabled &&
            accessibilityDisclosureAccepted &&
            selectedMode == ProtectionMode.BLOCK_SHORTS &&
            freeTestState(nowMillis) == EntitlementState.FREE_TEST_ACTIVE &&
            isPinCreated &&
            !isTemporarilyAllowed(nowMillis)
}
