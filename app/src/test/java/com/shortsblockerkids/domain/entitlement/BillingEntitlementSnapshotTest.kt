package com.shortsblockerkids.domain.entitlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingEntitlementSnapshotTest {
    @Test
    fun booleanConstructorMapsToActiveOrInactiveStates() {
        assertEquals(
            BillingEntitlementState.ACTIVE,
            BillingEntitlementSnapshot(isActive = true).state,
        )
        assertEquals(
            BillingEntitlementState.INACTIVE,
            BillingEntitlementSnapshot(isActive = false).state,
        )
    }

    @Test
    fun paidProtectionStatesAreExplicit() {
        assertTrue(BillingEntitlementState.ACTIVE.allowsPaidProtection())
        assertFalse(BillingEntitlementState.PENDING.allowsPaidProtection())
        assertFalse(BillingEntitlementState.INACTIVE.allowsPaidProtection())
    }
}
