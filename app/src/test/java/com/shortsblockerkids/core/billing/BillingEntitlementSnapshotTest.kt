package com.shortsblockerkids.core.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingEntitlementSnapshotTest {
    @Test
    fun legacyBooleanConstructorMapsToActiveOrExpiredStates() {
        assertEquals(
            BillingEntitlementState.ACTIVE,
            BillingEntitlementSnapshot(isActive = true, checkedAtMillis = 1_000L).state,
        )
        assertEquals(
            BillingEntitlementState.EXPIRED,
            BillingEntitlementSnapshot(isActive = false, checkedAtMillis = 1_000L).state,
        )
    }

    @Test
    fun paidProtectionStatesAreExplicit() {
        assertTrue(BillingEntitlementState.ACTIVE.allowsPaidProtection())
        assertTrue(BillingEntitlementState.CANCELED_ACTIVE.allowsPaidProtection())
        assertTrue(BillingEntitlementState.IN_GRACE.allowsPaidProtection())
        assertFalse(BillingEntitlementState.PENDING.allowsPaidProtection())
        assertFalse(BillingEntitlementState.ON_HOLD.allowsPaidProtection())
        assertFalse(BillingEntitlementState.EXPIRED.allowsPaidProtection())
        assertFalse(BillingEntitlementState.REVOKED.allowsPaidProtection())
        assertFalse(BillingEntitlementState.UNKNOWN.allowsPaidProtection())
    }
}
