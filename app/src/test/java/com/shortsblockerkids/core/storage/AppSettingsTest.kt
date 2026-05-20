package com.shortsblockerkids.core.storage

import com.shortsblockerkids.core.model.SubscriptionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun canProtectOnlyWhenLocalSetupIsComplete() {
        val settings =
            AppSettings(
                protectionEnabled = true,
                accessibilityDisclosureAccepted = true,
                subscriptionStateCached = SubscriptionState.ACTIVE,
                pinHash = "hash",
                pinSalt = "salt",
            )

        assertTrue(settings.canProtect(nowMillis = 1_000L))
        assertFalse(settings.copy(protectionEnabled = false).canProtect(nowMillis = 1_000L))
        assertFalse(
            settings.copy(accessibilityDisclosureAccepted = false).canProtect(nowMillis = 1_000L),
        )
        assertFalse(settings.copy(pinHash = null).canProtect(nowMillis = 1_000L))
    }

    @Test
    fun expiredOrOnHoldSubscriptionDisablesProtection() {
        val settings =
            AppSettings(
                protectionEnabled = true,
                accessibilityDisclosureAccepted = true,
                pinHash = "hash",
                pinSalt = "salt",
            )

        assertFalse(
            settings
                .copy(subscriptionStateCached = SubscriptionState.EXPIRED)
                .canProtect(nowMillis = 1_000L),
        )
        assertFalse(
            settings
                .copy(subscriptionStateCached = SubscriptionState.ON_HOLD)
                .canProtect(nowMillis = 1_000L),
        )
        assertTrue(
            settings
                .copy(subscriptionStateCached = SubscriptionState.ACTIVE)
                .canProtect(nowMillis = 1_000L),
        )
    }

    @Test
    fun temporaryAllowDisablesProtectionUntilItExpires() {
        val settings =
            AppSettings(
                protectionEnabled = true,
                accessibilityDisclosureAccepted = true,
                subscriptionStateCached = SubscriptionState.ACTIVE,
                temporaryAllowUntil = 2_000L,
                pinHash = "hash",
                pinSalt = "salt",
            )

        assertFalse(settings.canProtect(nowMillis = 1_500L))
        assertTrue(settings.canProtect(nowMillis = 2_500L))
    }
}
