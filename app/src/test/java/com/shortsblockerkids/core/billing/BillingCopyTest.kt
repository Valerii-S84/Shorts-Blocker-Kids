package com.shortsblockerkids.core.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class BillingCopyTest {
    @Test
    fun subscriptionTermsIncludeLoadedPriceAndGooglePlayCancellationPath() {
        assertEquals(
            "Monthly auto-renewing subscription: localized Play price. " +
                "Required after the free test to keep short-video blocking active. " +
                "Charged by Google Play; manage or cancel anytime in Google Play.",
            BillingCopy.subscriptionTermsText("localized Play price"),
        )
    }

    @Test
    fun subscriptionTermsRemainExplicitBeforePriceLoads() {
        assertEquals(
            "Monthly auto-renewing subscription. " +
                "Subscription price is shown by Google Play before purchase. " +
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
                        productPrice = "localized Play price",
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
                        productPrice = "localized Play price",
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
                        productPrice = "localized Play price",
                        statusMessage = "Loaded.",
                    ),
            )

        assertEquals("available: localized Play price", label)
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

    @Test
    fun billingUiStatePriceLabelUsesLoadedPriceOrFallback() {
        assertEquals("Price shown in Google Play", BillingUiState().priceLabel)
        assertEquals(
            "localized Play price",
            BillingUiState(productPrice = "localized Play price").priceLabel,
        )
    }

    @Test
    fun billingEntitlementSnapshotStoresPlayBillingState() {
        val snapshot = BillingEntitlementSnapshot(isActive = true, checkedAtMillis = 4_000L)

        assertEquals(true, snapshot.isActive)
        assertEquals(4_000L, snapshot.checkedAtMillis)
    }
}
