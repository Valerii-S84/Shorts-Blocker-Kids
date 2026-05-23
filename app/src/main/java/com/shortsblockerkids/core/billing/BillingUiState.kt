package com.shortsblockerkids.core.billing

data class BillingUiState(
    val isReady: Boolean = false,
    val isLoading: Boolean = false,
    val isPurchaseInProgress: Boolean = false,
    val productPrice: String? = null,
    val statusMessage: String = "Connecting to Google Play Billing.",
    val canStartPurchase: Boolean = false,
) {
    val priceLabel: String
        get() = productPrice ?: "Monthly subscription"
}
