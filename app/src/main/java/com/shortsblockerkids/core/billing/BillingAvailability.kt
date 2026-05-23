package com.shortsblockerkids.core.billing

object BillingAvailability {
    const val BILLING_ENABLED = true
    const val MONTHLY_SUBSCRIPTION_PRODUCT_ID = "shorts_blocker_kids_monthly"
    const val DEFERRED_MESSAGE = "Subscription is managed by Google Play."
    const val OFFLINE_GRACE_MILLIS = 72L * 60L * 60L * 1_000L
}
