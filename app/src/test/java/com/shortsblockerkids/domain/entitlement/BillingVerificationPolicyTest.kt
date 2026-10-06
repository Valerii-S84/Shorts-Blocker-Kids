package com.shortsblockerkids.domain.entitlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingVerificationPolicyTest {
    @Test
    fun localPurchaseDoesNotGrantPremiumWithoutInternalTestingMode() {
        val policy =
            BillingVerificationPolicy(
                clientOnlyModeRequested = false,
                internalTestingBuild = true,
            )

        val snapshot =
            policy.localPurchaseSnapshot(
                hasPurchasedSubscription = true,
                checkedAtMillis = 1_000L,
            )

        assertFalse(policy.canUseClientOnlyEntitlement)
        assertEquals(BillingEntitlementState.EXPIRED, snapshot.state)
        assertFalse(snapshot.isActive)
    }

    @Test
    fun requestedClientOnlyModeDoesNotGrantPremiumOutsideInternalTestingBuilds() {
        val policy =
            BillingVerificationPolicy(
                clientOnlyModeRequested = true,
                internalTestingBuild = false,
            )

        val snapshot =
            policy.localPurchaseSnapshot(
                hasPurchasedSubscription = true,
                checkedAtMillis = 1_000L,
            )

        assertFalse(policy.canUseClientOnlyEntitlement)
        assertEquals(BillingEntitlementState.EXPIRED, snapshot.state)
    }

    @Test
    fun clientOnlyModeCanGrantPremiumOnlyForInternalTestingBuilds() {
        val policy =
            BillingVerificationPolicy(
                clientOnlyModeRequested = true,
                internalTestingBuild = true,
            )

        val snapshot =
            policy.localPurchaseSnapshot(
                hasPurchasedSubscription = true,
                checkedAtMillis = 1_000L,
            )

        assertTrue(policy.canUseClientOnlyEntitlement)
        assertEquals(BillingEntitlementState.ACTIVE, snapshot.state)
        assertTrue(snapshot.isActive)
    }

    @Test
    fun missingLocalPurchaseNeverGrantsPremium() {
        val policy =
            BillingVerificationPolicy(
                clientOnlyModeRequested = true,
                internalTestingBuild = true,
            )

        val snapshot =
            policy.localPurchaseSnapshot(
                hasPurchasedSubscription = false,
                checkedAtMillis = 1_000L,
            )

        assertEquals(BillingEntitlementState.EXPIRED, snapshot.state)
        assertFalse(snapshot.isActive)
    }

    @Test
    fun failClosedSnapshotNeverGrantsPremiumEntitlement() {
        val policy =
            BillingVerificationPolicy(
                clientOnlyModeRequested = true,
                internalTestingBuild = true,
            )

        val snapshot = policy.failClosedSnapshot(checkedAtMillis = 2_000L)

        assertEquals(BillingEntitlementState.UNKNOWN, snapshot.state)
        assertEquals(2_000L, snapshot.checkedAtMillis)
        assertFalse(snapshot.isActive)
    }

    @Test
    fun defaultsKeepClientOnlyEntitlementDisabled() {
        val policy = BillingVerificationPolicy()

        assertFalse(policy.clientOnlyModeRequested)
        assertFalse(policy.internalTestingBuild)
        assertFalse(policy.canUseClientOnlyEntitlement)
    }

    @Test
    fun dataClassCopyAndEqualityKeepPolicyFlagsVisible() {
        val policy =
            BillingVerificationPolicy(
                clientOnlyModeRequested = true,
                internalTestingBuild = false,
            )

        val internalTestingPolicy = policy.copy(internalTestingBuild = true)

        assertEquals(policy, policy.copy())
        assertEquals(policy.hashCode(), policy.copy().hashCode())
        assertEquals(
            BillingVerificationPolicy(
                clientOnlyModeRequested = true,
                internalTestingBuild = true,
            ),
            internalTestingPolicy,
        )
        assertTrue(internalTestingPolicy.canUseClientOnlyEntitlement)
        assertTrue(policy.toString().contains("clientOnlyModeRequested=true"))
    }
}
