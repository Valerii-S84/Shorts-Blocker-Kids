package com.shortsblockerkids.infrastructure.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayBillingConfigTest {
    @Test
    fun billingUsesGooglePlaySubscriptionProduct() {
        assertTrue(PlayBillingConfig.BILLING_ENABLED)
        assertEquals("shorts_blocker_kids_monthly", PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID)
    }
}
