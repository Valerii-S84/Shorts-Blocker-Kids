package com.shortsblockerkids.presentation.billing

data class BillingUiState(
    val isReady: Boolean = false,
    val isLoading: Boolean = false,
    val isPurchaseInProgress: Boolean = false,
    val productPrice: String? = null,
    val message: BillingUiMessage = BillingUiMessage(BillingMessageCode.CONNECTING),
    val canStartPurchase: Boolean = false,
)

data class BillingUiMessage(
    val code: BillingMessageCode,
    val responseCode: Int? = null,
)

enum class BillingMessageCode {
    CONNECTING,
    CONNECTED,
    BILLING_UNAVAILABLE,
    DISCONNECTED,
    SUBSCRIPTION_NOT_READY,
    OPENING_PURCHASE_FLOW,
    OPEN_PURCHASE_FLOW_FAILED,
    MANAGE_SUBSCRIPTION_UNAVAILABLE,
    PURCHASE_CANCELED,
    PURCHASE_FAILED,
    LOAD_SUBSCRIPTION_FAILED,
    PRODUCT_UNAVAILABLE,
    PRODUCT_LOADED,
    RESTORE_PURCHASES_FAILED,
    SUBSCRIPTION_ACTIVE,
    PURCHASE_PENDING,
    NO_ACTIVE_SUBSCRIPTION,
    ACKNOWLEDGE_PURCHASE_FAILED,
    ENTITLEMENT_SAVE_FAILED,
}
