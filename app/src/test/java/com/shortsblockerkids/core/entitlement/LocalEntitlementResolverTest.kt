package com.shortsblockerkids.core.entitlement

import com.shortsblockerkids.core.model.EntitlementState
import com.shortsblockerkids.core.storage.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalEntitlementResolverTest {
    @Test
    fun permissionMissingIsReportedBeforeProtectionIsActive() {
        val settings = activeSettings()

        val state =
            LocalEntitlementResolver.resolve(
                settings = settings,
                isProtectionPermissionGranted = false,
                nowMillis = 1_000L,
            )

        assertEquals(EntitlementState.PROTECTION_PERMISSION_MISSING, state)
    }

    @Test
    fun activeFreeTestAndPermissionMakeProtectionActive() {
        val settings = activeSettings()

        val state =
            LocalEntitlementResolver.resolve(
                settings = settings,
                isProtectionPermissionGranted = true,
                nowMillis = 1_000L,
            )

        assertEquals(EntitlementState.PROTECTION_ACTIVE, state)
    }

    @Test
    fun expiredFreeTestLocksProtection() {
        val settings = activeSettings()

        val state =
            LocalEntitlementResolver.resolve(
                settings = settings,
                isProtectionPermissionGranted = true,
                nowMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY,
            )

        assertEquals(EntitlementState.PROTECTION_LOCKED, state)
    }

    @Test
    fun activeSubscriptionKeepsProtectionAvailableAfterFreeTestExpiry() {
        val settings =
            activeSettings()
                .copy(
                    billingSubscriptionActive = true,
                    billingLastVerifiedAt = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY,
                )

        val state =
            LocalEntitlementResolver.resolve(
                settings = settings,
                isProtectionPermissionGranted = true,
                nowMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY,
            )

        assertEquals(EntitlementState.PROTECTION_ACTIVE, state)
    }

    @Test
    fun subscriptionActiveButProtectionOffIsReportedAsEntitledNotActive() {
        val settings =
            activeSettings()
                .copy(
                    protectionEnabled = false,
                    billingSubscriptionActive = true,
                    billingLastVerifiedAt = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY,
                )

        val state =
            LocalEntitlementResolver.resolve(
                settings = settings,
                isProtectionPermissionGranted = true,
                nowMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY,
            )

        assertEquals(EntitlementState.SUBSCRIPTION_ACTIVE, state)
    }

    private fun activeSettings(): AppSettings =
        AppSettings(
            protectionEnabled = true,
            accessibilityDisclosureAccepted = true,
            freeTestStartedAt = 0L,
            pinHash = "hash",
            pinSalt = "salt",
        )

    private companion object {
        const val ONE_DAY = 24L * 60L * 60L * 1_000L
    }
}
