package com.shortsblockerkids.core.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class BillingCopyTest {
    @Test
    fun subscriptionTermsIncludeLoadedPriceAndGooglePlayCancellationPath() {
        assertEquals(
            "Monthly auto-renewing subscription: EUR 2.20. " +
                "Required after the free test to keep short-video blocking active. " +
                "Charged by Google Play; manage or cancel anytime in Google Play.",
            BillingCopy.subscriptionTermsText("EUR 2.20"),
        )
    }

    @Test
    fun subscriptionTermsRemainExplicitBeforePriceLoads() {
        assertEquals(
            "Monthly auto-renewing subscription. " +
                "Required after the free test to keep short-video blocking active. " +
                "Charged by Google Play; manage or cancel anytime in Google Play.",
            BillingCopy.subscriptionTermsText(null),
        )
    }

    @Test
    fun statusLabelPrioritizesActiveEntitlement() {
        val label =
            BillingCopy.subscriptionStatusLabel(
                hasBillingEntitlement = true,
                billingUiState =
                    BillingUiState(
                        isPurchaseInProgress = true,
                        productPrice = "EUR 2.20",
                        statusMessage = "Loaded.",
                    ),
            )

        assertEquals("active", label)
    }

    @Test
    fun statusLabelShowsPurchaseInProgressBeforePriceAvailability() {
        val label =
            BillingCopy.subscriptionStatusLabel(
                hasBillingEntitlement = false,
                billingUiState =
                    BillingUiState(
                        isPurchaseInProgress = true,
                        productPrice = "EUR 2.20",
                        statusMessage = "Loaded.",
                    ),
            )

        assertEquals("purchase in progress", label)
    }

    @Test
    fun statusLabelShowsPriceWhenProductIsAvailable() {
        val label =
            BillingCopy.subscriptionStatusLabel(
                hasBillingEntitlement = false,
                billingUiState =
                    BillingUiState(
                        productPrice = "EUR 2.20",
                        statusMessage = "Loaded.",
                    ),
            )

        assertEquals("available: EUR 2.20", label)
    }

    @Test
    fun statusLabelFallsBackToBillingMessage() {
        val label =
            BillingCopy.subscriptionStatusLabel(
                hasBillingEntitlement = false,
                billingUiState = BillingUiState(statusMessage = "Billing unavailable."),
            )

        assertEquals("Billing unavailable.", label)
    }
}
