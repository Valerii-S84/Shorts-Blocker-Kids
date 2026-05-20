package com.shortsblockerkids.core.billing

import com.shortsblockerkids.core.model.SubscriptionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementResolverTest {
    @Test
    fun activeSubscriptionAllowsProtection() {
        val state =
            EntitlementResolver.resolve(
                listOf(
                    BillingPurchaseSnapshot(
                        productIds = listOf(BillingProductIds.MONTHLY_SUBSCRIPTION),
                        status = BillingPurchaseStatus.PURCHASED,
                        isAutoRenewing = true,
                    ),
                ),
            )

        assertEquals(SubscriptionState.ACTIVE, state)
        assertTrue(EntitlementResolver.canUseProtection(state))
    }

    @Test
    fun canceledButStillPurchasedAllowsProtectionUntilPeriodEnds() {
        val state =
            EntitlementResolver.resolve(
                listOf(
                    BillingPurchaseSnapshot(
                        productIds = listOf(BillingProductIds.YEARLY_SUBSCRIPTION),
                        status = BillingPurchaseStatus.PURCHASED,
                        isAutoRenewing = false,
                    ),
                ),
            )

        assertEquals(SubscriptionState.CANCELED_BUT_ACTIVE_UNTIL_END, state)
        assertTrue(EntitlementResolver.canUseProtection(state))
    }

    @Test
    fun pendingPurchaseDoesNotAllowProtection() {
        val state =
            EntitlementResolver.resolve(
                listOf(
                    BillingPurchaseSnapshot(
                        productIds = listOf(BillingProductIds.MONTHLY_SUBSCRIPTION),
                        status = BillingPurchaseStatus.PENDING,
                        isAutoRenewing = false,
                    ),
                ),
            )

        assertEquals(SubscriptionState.ON_HOLD, state)
        assertFalse(EntitlementResolver.canUseProtection(state))
    }

    @Test
    fun missingPurchaseIsExpired() {
        val state = EntitlementResolver.resolve(emptyList())

        assertEquals(SubscriptionState.EXPIRED, state)
        assertFalse(EntitlementResolver.canUseProtection(state))
    }

    @Test
    fun unknownSubscriptionDoesNotAllowProtection() {
        assertFalse(EntitlementResolver.canUseProtection(SubscriptionState.UNKNOWN))
    }
}
