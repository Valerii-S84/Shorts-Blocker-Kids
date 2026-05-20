package com.shortsblockerkids.core.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BillingAvailabilityTest {
    @Test
    fun billingRemainsDisabledAndDeferred() {
        assertFalse(BillingAvailability.BILLING_ENABLED)
        assertEquals("Subscription will be available soon.", BillingAvailability.DEFERRED_MESSAGE)
    }
}
