package com.shortsblockerkids.core.storage

import com.shortsblockerkids.core.billing.BillingAvailability
import com.shortsblockerkids.core.billing.BillingEntitlementState
import com.shortsblockerkids.core.billing.allowsPaidProtection
import com.shortsblockerkids.core.entitlement.FreeTestPolicy
import com.shortsblockerkids.core.model.EntitlementState
import com.shortsblockerkids.core.model.ProtectionMode

data class AppSettings(
    val protectionEnabled: Boolean = true,
    val accessibilityDisclosureAccepted: Boolean = false,
    val selectedMode: ProtectionMode = ProtectionMode.BLOCK_SHORTS,
    val enabledPlatformIds: Set<String> = DEFAULT_ENABLED_PLATFORM_IDS,
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

    val hasEnabledPlatforms: Boolean
        get() = enabledPlatformIds.isNotEmpty()

    fun isPlatformEnabled(platformId: String): Boolean = platformId in enabledPlatformIds

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

    fun hasBillingEntitlement(nowMillis: Long): Boolean {
        val verifiedAt = billingLastVerifiedAt ?: return false
        if (verifiedAt > nowMillis) {
            return false
        }
        val stateAllowsProtection =
            billingEntitlementState.allowsPaidProtection() ||
                (billingEntitlementState == BillingEntitlementState.UNKNOWN && billingSubscriptionActive)
        if (!stateAllowsProtection) {
            return false
        }
        val offlineGraceUntil = verifiedAt + BillingAvailability.OFFLINE_GRACE_MILLIS
        val activeUntil = billingActiveUntilMillis ?: offlineGraceUntil
        return nowMillis <= activeUntil && nowMillis <= offlineGraceUntil
    }

    fun hasProtectionEntitlement(nowMillis: Long): Boolean =
        freeTestState(nowMillis) == EntitlementState.FREE_TEST_ACTIVE ||
            hasBillingEntitlement(nowMillis)

    fun canProtect(nowMillis: Long): Boolean =
        protectionEnabled &&
            accessibilityDisclosureAccepted &&
            selectedMode == ProtectionMode.BLOCK_SHORTS &&
            hasEnabledPlatforms &&
            hasProtectionEntitlement(nowMillis) &&
            isPinCreated &&
            !isTemporarilyAllowed(nowMillis)

    companion object {
        const val YOUTUBE_SHORTS_PLATFORM_ID = "youtube_shorts"
        const val TIKTOK_PLATFORM_ID = "tiktok"
        const val INSTAGRAM_REELS_PLATFORM_ID = "instagram_reels"
        const val FACEBOOK_REELS_PLATFORM_ID = "facebook_reels"

        val DEFAULT_ENABLED_PLATFORM_IDS =
            setOf(
                YOUTUBE_SHORTS_PLATFORM_ID,
                TIKTOK_PLATFORM_ID,
                INSTAGRAM_REELS_PLATFORM_ID,
                FACEBOOK_REELS_PLATFORM_ID,
            )
    }
}
