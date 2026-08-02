package com.shortsblockerkids.core.storage

import com.shortsblockerkids.core.billing.BillingEntitlementState
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionMode

internal data class AppSettings(
    val protectionEnabled: Boolean = true,
    val accessibilityDisclosureAccepted: Boolean = false,
    val selectedMode: ProtectionMode = ProtectionMode.BLOCK_SHORTS,
    val enabledPlatformIds: Set<String> = ProtectionConfiguration.DEFAULT_ENABLED_PLATFORM_IDS,
    val temporaryAllowUntil: Long? = null,
    val freeTestStartedAt: Long? = null,
    val freeTestDurationDays: Int = FreeTestPolicy.DEFAULT_DURATION_DAYS,
    val billingInstallationId: String? = null,
    val billingEntitlementState: BillingEntitlementState = BillingEntitlementState.UNKNOWN,
    val billingSubscriptionActive: Boolean = false,
    val billingLastVerifiedAt: Long? = null,
    val billingActiveUntilMillis: Long? = null,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val pinHashVersion: Int = 1,
    val failedPinAttempts: Int = 0,
    val pinLockoutUntil: Long? = null,
) {
    val isPinCreated: Boolean
        get() = !pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()
}
