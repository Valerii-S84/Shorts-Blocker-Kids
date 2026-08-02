package com.shortsblockerkids.core.billing

import com.shortsblockerkids.domain.entitlement.EntitlementPolicy

object BillingAvailability {
    const val BILLING_ENABLED = true
    const val MONTHLY_SUBSCRIPTION_PRODUCT_ID = "shorts_blocker_kids_monthly"
    const val OFFLINE_GRACE_MILLIS = EntitlementPolicy.PAID_OFFLINE_GRACE_MILLIS
}
