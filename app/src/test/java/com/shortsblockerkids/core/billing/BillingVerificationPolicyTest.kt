package com.shortsblockerkids.core.billing

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
        assertEquals(
            BillingMessageCode.BACKEND_VERIFICATION_REQUIRED,
            policy.localPurchaseMessageCode(
                hasPurchasedSubscription = true,
                hasPendingSubscription = false,
            ),
        )
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
    fun localPurchasePresentationUsesStableCodes() {
        val policy =
            BillingVerificationPolicy(
                clientOnlyModeRequested = true,
                internalTestingBuild = true,
            )

        assertEquals(
            BillingMessageCode.SUBSCRIPTION_ACTIVE,
            policy.localPurchaseMessageCode(
                hasPurchasedSubscription = true,
                hasPendingSubscription = false,
            ),
        )
        assertEquals(
            BillingMessageCode.PURCHASE_PENDING,
            policy.localPurchaseMessageCode(
                hasPurchasedSubscription = false,
                hasPendingSubscription = true,
            ),
        )
        assertEquals(
            BillingMessageCode.NO_ACTIVE_SUBSCRIPTION,
            policy.localPurchaseMessageCode(
                hasPurchasedSubscription = false,
                hasPendingSubscription = false,
            ),
        )
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
}
