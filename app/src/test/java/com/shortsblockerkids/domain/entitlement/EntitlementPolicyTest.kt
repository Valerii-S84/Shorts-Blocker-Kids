package com.shortsblockerkids.domain.entitlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementPolicyTest {
    @Test
    fun freeTestStatePreservesExactExpiryBoundary() {
        val snapshot = EntitlementSnapshot(freeTestStartedAtMillis = 0L)
        val expiry = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY

        assertEquals(FreeTestState.ACTIVE, EntitlementPolicy.freeTestState(snapshot, expiry - 1L))
        assertEquals(1, EntitlementPolicy.freeTestDaysRemaining(snapshot, expiry - 1L))
        assertEquals(FreeTestState.EXPIRED, EntitlementPolicy.freeTestState(snapshot, expiry))
        assertEquals(0, EntitlementPolicy.freeTestDaysRemaining(snapshot, expiry))
    }

    @Test
    fun missingFreeTestStartRemainsNotStarted() {
        val snapshot = EntitlementSnapshot()

        assertEquals(FreeTestState.NOT_STARTED, EntitlementPolicy.freeTestState(snapshot, 1_000L))
        assertEquals(null, EntitlementPolicy.freeTestDaysRemaining(snapshot, 1_000L))
        assertFalse(EntitlementPolicy.hasProtectionEntitlement(snapshot, 1_000L))
    }

    @Test
    fun paidEntitlementFollowsStoredPlayState() {
        val active = EntitlementSnapshot(isPaidProtectionAllowed = true)
        assertTrue(EntitlementPolicy.hasPaidEntitlement(active))
        assertFalse(EntitlementPolicy.hasPaidEntitlement(active.copy(isPaidProtectionAllowed = false)))
    }

    private companion object {
        const val ONE_DAY = 24L * 60L * 60L * 1_000L
    }
}
