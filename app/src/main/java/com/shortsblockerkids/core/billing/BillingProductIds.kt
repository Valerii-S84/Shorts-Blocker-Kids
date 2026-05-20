package com.shortsblockerkids.core.billing

object BillingProductIds {
    const val MONTHLY_SUBSCRIPTION = "shorts_blocker_kids_monthly"
    const val YEARLY_SUBSCRIPTION = "shorts_blocker_kids_yearly"

    val subscriptions: List<String> =
        listOf(
            MONTHLY_SUBSCRIPTION,
            YEARLY_SUBSCRIPTION,
        )
}
