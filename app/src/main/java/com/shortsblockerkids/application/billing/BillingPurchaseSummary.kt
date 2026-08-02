package com.shortsblockerkids.application.billing

data class BillingPurchaseSummary(
    val purchasedSubscriptionToken: String?,
    val hasPendingSubscription: Boolean = false,
    val unacknowledgedPurchasedSubscriptionTokens: List<String> = emptyList(),
) {
    val hasPurchasedSubscription: Boolean
        get() = purchasedSubscriptionToken != null
}
