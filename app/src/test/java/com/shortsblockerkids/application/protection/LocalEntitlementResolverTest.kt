package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.model.EntitlementState
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalEntitlementResolverTest {
    @Test
    fun permissionMissingIsReportedBeforeProtectionIsActive() {
        val state = LocalEntitlementResolver.resolve(activeInput(permissionGranted = false))

        assertEquals(EntitlementState.PROTECTION_PERMISSION_MISSING, state)
    }

    @Test
    fun activeFreeTestAndPermissionMakeProtectionActive() {
        val state = LocalEntitlementResolver.resolve(activeInput(permissionGranted = true))

        assertEquals(EntitlementState.PROTECTION_ACTIVE, state)
    }

    @Test
    fun expiredFreeTestLocksProtection() {
        val state =
            LocalEntitlementResolver.resolve(
                activeInput(
                    permissionGranted = true,
                    nowMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY,
                ),
            )

        assertEquals(EntitlementState.PROTECTION_LOCKED, state)
    }

    @Test
    fun activeSubscriptionKeepsProtectionAvailableAfterFreeTestExpiry() {
        val nowMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY
        val input =
            activeInput(permissionGranted = true, nowMillis = nowMillis)
                .copy(
                    entitlement =
                        activeInput(permissionGranted = true).entitlement.copy(
                            isPaidProtectionAllowed = true,
                            paidLastVerifiedAtMillis = nowMillis,
                        ),
                )

        val state = LocalEntitlementResolver.resolve(input)

        assertEquals(EntitlementState.PROTECTION_ACTIVE, state)
    }

    @Test
    fun subscriptionActiveButProtectionOffIsReportedAsEntitledNotActive() {
        val nowMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY
        val input =
            activeInput(permissionGranted = true, nowMillis = nowMillis)
                .copy(
                    protectionConfiguration =
                        ProtectionConfiguration(
                            isEnabled = false,
                            isAccessibilityDisclosureAccepted = true,
                            isPinConfigured = true,
                        ),
                    entitlement =
                        EntitlementSnapshot(
                            freeTestStartedAtMillis = 0L,
                            isPaidProtectionAllowed = true,
                            paidLastVerifiedAtMillis = nowMillis,
                        ),
                )

        val state = LocalEntitlementResolver.resolve(input)

        assertEquals(EntitlementState.SUBSCRIPTION_ACTIVE, state)
    }

    @Test
    fun unconfirmedLocalBillingFlagDoesNotUnlockAfterFreeTestExpiry() {
        val nowMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY
        val input =
            activeInput(permissionGranted = true, nowMillis = nowMillis)
                .copy(
                    entitlement =
                        EntitlementSnapshot(
                            freeTestStartedAtMillis = 0L,
                            isPaidProtectionAllowed = false,
                            paidLastVerifiedAtMillis = nowMillis,
                        ),
                )

        val state = LocalEntitlementResolver.resolve(input)

        assertEquals(EntitlementState.PROTECTION_LOCKED, state)
    }

    @Test
    fun freeTestNotStartedIsReportedWhenProtectionCannotStart() {
        val input =
            activeInput(permissionGranted = true)
                .copy(entitlement = EntitlementSnapshot())

        val state = LocalEntitlementResolver.resolve(input)

        assertEquals(EntitlementState.FREE_TEST_NOT_STARTED, state)
    }

    @Test
    fun activeFreeTestIsReportedWhenProtectionIsDisabled() {
        val input =
            activeInput(permissionGranted = true)
                .copy(
                    protectionConfiguration =
                        ProtectionConfiguration(
                            isEnabled = false,
                            isAccessibilityDisclosureAccepted = true,
                            isPinConfigured = true,
                        ),
                )

        val state = LocalEntitlementResolver.resolve(input)

        assertEquals(EntitlementState.FREE_TEST_ACTIVE, state)
    }

    private fun activeInput(
        permissionGranted: Boolean,
        nowMillis: Long = 1_000L,
    ): LocalEntitlementInput =
        LocalEntitlementInput(
            protectionConfiguration =
                ProtectionConfiguration(
                    isEnabled = true,
                    isAccessibilityDisclosureAccepted = true,
                    isPinConfigured = true,
                ),
            entitlement = EntitlementSnapshot(freeTestStartedAtMillis = 0L),
            isProtectionPermissionGranted = permissionGranted,
            nowMillis = nowMillis,
        )

    private companion object {
        const val ONE_DAY = 24L * 60L * 60L * 1_000L
    }
}
