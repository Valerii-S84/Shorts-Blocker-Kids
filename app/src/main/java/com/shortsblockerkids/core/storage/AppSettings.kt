package com.shortsblockerkids.core.storage

import com.shortsblockerkids.core.billing.EntitlementResolver
import com.shortsblockerkids.core.model.ProtectionMode
import com.shortsblockerkids.core.model.SubscriptionState

data class AppSettings(
    val protectionEnabled: Boolean = true,
    val accessibilityDisclosureAccepted: Boolean = false,
    val selectedMode: ProtectionMode = ProtectionMode.BLOCK_SHORTS,
    val temporaryAllowUntil: Long? = null,
    val subscriptionStateCached: SubscriptionState = SubscriptionState.UNKNOWN,
    val lastBillingCheckAt: Long? = null,
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

    fun canProtect(nowMillis: Long): Boolean =
        protectionEnabled &&
            accessibilityDisclosureAccepted &&
            selectedMode == ProtectionMode.BLOCK_SHORTS &&
            EntitlementResolver.canUseProtection(subscriptionStateCached) &&
            isPinCreated &&
            !isTemporarilyAllowed(nowMillis)
}
