package com.shortsblockerkids.core.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingAvailabilityTest {
    @Test
    fun billingUsesGooglePlaySubscriptionProduct() {
        assertTrue(BillingAvailability.BILLING_ENABLED)
        assertEquals("shorts_blocker_kids_monthly", BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID)
        assertEquals("Subscription is managed by Google Play.", BillingAvailability.DEFERRED_MESSAGE)
    }
}
