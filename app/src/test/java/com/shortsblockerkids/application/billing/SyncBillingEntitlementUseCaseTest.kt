package com.shortsblockerkids.application.billing

import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncBillingEntitlementUseCaseTest {
    private val useCase = SyncBillingEntitlementUseCase()

    @Test
    fun purchasedSubscriptionGrantsAccessAndPassesUnacknowledgedTokens() {
        val outcome =
            useCase(
                BillingPurchaseSummary(
                    purchasedSubscriptionToken = "purchase-token",
                    hasPendingSubscription = true,
                    unacknowledgedPurchasedSubscriptionTokens = listOf("purchase-token"),
                ),
            )

        assertEquals(BillingEntitlementState.ACTIVE, outcome.entitlementSnapshot.state)
        assertTrue(outcome.entitlementSnapshot.isActive)
        assertTrue(outcome.hasPendingSubscription)
        assertEquals(listOf("purchase-token"), outcome.purchaseTokensToAcknowledge)
    }

    @Test
    fun alreadyAcknowledgedPurchaseGrantsAccessWithoutAcknowledgingAgain() {
        val outcome =
            useCase(
                BillingPurchaseSummary(
                    purchasedSubscriptionToken = "purchase-token",
                    unacknowledgedPurchasedSubscriptionTokens = emptyList(),
                ),
            )

        assertEquals(BillingEntitlementState.ACTIVE, outcome.entitlementSnapshot.state)
        assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
    }

    @Test
    fun pendingOnlyPurchaseDoesNotGrantAccessOrAcknowledge() {
        val outcome =
            useCase(
                BillingPurchaseSummary(
                    purchasedSubscriptionToken = null,
                    hasPendingSubscription = true,
                    unacknowledgedPurchasedSubscriptionTokens = listOf("pending-token"),
                ),
            )

        assertEquals(BillingEntitlementState.PENDING, outcome.entitlementSnapshot.state)
        assertFalse(outcome.entitlementSnapshot.isActive)
        assertTrue(outcome.hasPendingSubscription)
        assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
    }

    @Test
    fun successfulEmptyPurchaseListClearsPaidAccess() {
        val outcome = useCase(BillingPurchaseSummary(purchasedSubscriptionToken = null))

        assertEquals(BillingEntitlementState.INACTIVE, outcome.entitlementSnapshot.state)
        assertFalse(outcome.entitlementSnapshot.isActive)
        assertFalse(outcome.hasPendingSubscription)
        assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
    }
}
