package com.shortsblockerkids.application.billing

import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState

class SyncBillingEntitlementUseCase {
    operator fun invoke(purchaseSummary: BillingPurchaseSummary): BillingSyncOutcome =
        BillingSyncOutcome(
            entitlementSnapshot =
                BillingEntitlementSnapshot(
                    state =
                        when {
                            purchaseSummary.hasPurchasedSubscription -> BillingEntitlementState.ACTIVE
                            purchaseSummary.hasPendingSubscription -> BillingEntitlementState.PENDING
                            else -> BillingEntitlementState.INACTIVE
                        },
                ),
            hasPendingSubscription = purchaseSummary.hasPendingSubscription,
            purchaseTokensToAcknowledge =
                if (purchaseSummary.hasPurchasedSubscription) {
                    purchaseSummary.unacknowledgedPurchasedSubscriptionTokens
                } else {
                    emptyList()
                },
        )
}
