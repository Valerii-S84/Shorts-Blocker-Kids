package com.shortsblockerkids.core.billing

object BillingCopy {
    fun subscriptionTermsText(productPrice: String?): String {
        val price =
            productPrice?.let { loadedPrice ->
                "Monthly auto-renewing subscription: $loadedPrice"
            } ?: "Monthly auto-renewing subscription"

        return "$price. Required after the free test to keep short-video blocking active. " +
            "Charged by Google Play; manage or cancel anytime in Google Play."
    }

    fun subscriptionStatusLabel(
        hasBillingEntitlement: Boolean,
        billingUiState: BillingUiState,
    ): String =
        when {
            hasBillingEntitlement -> "active"
            billingUiState.isPurchaseInProgress -> "purchase in progress"
            billingUiState.productPrice != null -> "available: ${billingUiState.productPrice}"
            else -> billingUiState.statusMessage
        }
}
