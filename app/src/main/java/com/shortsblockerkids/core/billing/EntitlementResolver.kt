package com.shortsblockerkids.core.billing

import com.shortsblockerkids.core.model.SubscriptionState

object EntitlementResolver {
    fun canUseProtection(state: SubscriptionState): Boolean =
        when (state) {
            SubscriptionState.ACTIVE,
            SubscriptionState.TRIAL,
            SubscriptionState.GRACE_PERIOD,
            SubscriptionState.CANCELED_BUT_ACTIVE_UNTIL_END,
            -> true

            SubscriptionState.UNKNOWN,
            SubscriptionState.ON_HOLD,
            SubscriptionState.EXPIRED,
            -> false
        }

    fun resolve(purchases: List<BillingPurchaseSnapshot>): SubscriptionState {
        if (purchases.isEmpty()) {
            return SubscriptionState.EXPIRED
        }

        val activePurchase = purchases.firstOrNull { it.status == BillingPurchaseStatus.PURCHASED }
        if (activePurchase != null) {
            return if (activePurchase.isAutoRenewing) {
                SubscriptionState.ACTIVE
            } else {
                SubscriptionState.CANCELED_BUT_ACTIVE_UNTIL_END
            }
        }

        if (purchases.any { it.status == BillingPurchaseStatus.PENDING }) {
            return SubscriptionState.ON_HOLD
        }

        return SubscriptionState.EXPIRED
    }
}

data class BillingPurchaseSnapshot(
    val productIds: List<String>,
    val status: BillingPurchaseStatus,
    val isAutoRenewing: Boolean,
)

enum class BillingPurchaseStatus {
    PURCHASED,
    PENDING,
    UNSPECIFIED,
}
