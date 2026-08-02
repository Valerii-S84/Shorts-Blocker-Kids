package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.model.EntitlementState
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsSnapshotQueriesTest {
    @Test
    fun snapshotBuildsNeutralResolverInput() {
        val snapshot = entitledSnapshot()

        val input =
            snapshot.toLocalEntitlementInput(
                isProtectionPermissionGranted = true,
                nowMillis = NOW_MILLIS,
            )

        assertEquals(snapshot.protectionConfiguration, input.protectionConfiguration)
        assertEquals(snapshot.entitlement, input.entitlement)
        assertTrue(input.isProtectionPermissionGranted)
        assertEquals(NOW_MILLIS, input.nowMillis)
    }

    @Test
    fun snapshotQueriesExposeAllFreeTestStatesAndDaysRemaining() {
        val notStarted = AppSettingsSnapshot()
        val active =
            AppSettingsSnapshot(
                entitlement = EntitlementSnapshot(freeTestStartedAtMillis = 0L),
            )
        val expiryMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY_MILLIS

        assertEquals(EntitlementState.FREE_TEST_NOT_STARTED, notStarted.freeTestState(NOW_MILLIS))
        assertNull(notStarted.freeTestDaysRemaining(NOW_MILLIS))
        assertEquals(EntitlementState.FREE_TEST_ACTIVE, active.freeTestState(expiryMillis - 1L))
        assertEquals(1, active.freeTestDaysRemaining(expiryMillis - 1L))
        assertEquals(EntitlementState.FREE_TEST_EXPIRED, active.freeTestState(expiryMillis))
    }

    @Test
    fun snapshotQueriesPreserveEntitlementAndProtectionGates() {
        val snapshot = entitledSnapshot()

        assertTrue(snapshot.hasBillingEntitlement(NOW_MILLIS))
        assertTrue(snapshot.canProtect(NOW_MILLIS))
        assertFalse(
            snapshot
                .copy(
                    protectionConfiguration =
                        snapshot.protectionConfiguration.copy(isEnabled = false),
                ).canProtect(NOW_MILLIS),
        )
        assertFalse(AppSettingsSnapshot().hasBillingEntitlement(NOW_MILLIS))
    }

    @Test
    fun temporaryAllowUsesExactExpiryBoundary() {
        val snapshot =
            AppSettingsSnapshot(
                protectionConfiguration =
                    ProtectionConfiguration(temporaryAllowUntilMillis = NOW_MILLIS),
            )

        assertTrue(snapshot.isTemporarilyAllowed(NOW_MILLIS - 1L))
        assertFalse(snapshot.isTemporarilyAllowed(NOW_MILLIS))
    }

    private fun entitledSnapshot(): AppSettingsSnapshot =
        AppSettingsSnapshot(
            protectionConfiguration =
                ProtectionConfiguration(
                    isEnabled = true,
                    isAccessibilityDisclosureAccepted = true,
                    isPinConfigured = true,
                ),
            entitlement =
                EntitlementSnapshot(
                    isPaidProtectionAllowed = true,
                    paidLastVerifiedAtMillis = NOW_MILLIS,
                ),
        )

    private companion object {
        const val NOW_MILLIS = 1_000L
        const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
