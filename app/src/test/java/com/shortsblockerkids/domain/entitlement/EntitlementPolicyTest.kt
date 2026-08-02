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
    fun paidEntitlementRequiresAllowedStateAndNonFutureVerification() {
        val allowed =
            EntitlementSnapshot(
                isPaidProtectionAllowed = true,
                paidLastVerifiedAtMillis = 1_000L,
            )

        assertTrue(EntitlementPolicy.hasPaidEntitlement(allowed, 1_000L))
        assertFalse(
            EntitlementPolicy.hasPaidEntitlement(
                allowed.copy(isPaidProtectionAllowed = false),
                1_000L,
            ),
        )
        assertFalse(
            EntitlementPolicy.hasPaidEntitlement(
                allowed.copy(paidLastVerifiedAtMillis = 1_001L),
                1_000L,
            ),
        )
    }

    @Test
    fun paidEntitlementHonorsActiveUntilAndOfflineGraceWindows() {
        val verifiedAt = 1_000L
        val snapshot =
            EntitlementSnapshot(
                isPaidProtectionAllowed = true,
                paidLastVerifiedAtMillis = verifiedAt,
                paidActiveUntilMillis = verifiedAt + ONE_DAY,
            )

        assertTrue(EntitlementPolicy.hasPaidEntitlement(snapshot, verifiedAt + ONE_DAY))
        assertFalse(EntitlementPolicy.hasPaidEntitlement(snapshot, verifiedAt + ONE_DAY + 1L))
        assertFalse(
            EntitlementPolicy.hasPaidEntitlement(
                snapshot.copy(paidActiveUntilMillis = verifiedAt + 7L * ONE_DAY),
                verifiedAt + EntitlementPolicy.PAID_OFFLINE_GRACE_MILLIS + 1L,
            ),
        )
    }

    private companion object {
        const val ONE_DAY = 24L * 60L * 60L * 1_000L
    }
}
