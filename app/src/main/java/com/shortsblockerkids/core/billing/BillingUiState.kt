package com.shortsblockerkids.core.billing

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
    BACKEND_VERIFICATION_REQUIRED,
    PURCHASE_PENDING,
    NO_ACTIVE_SUBSCRIPTION,
    BACKEND_VERIFICATION_NOT_READY,
    VERIFYING_WITH_BACKEND,
    VERIFY_WITH_BACKEND_FAILED,
    SUBSCRIPTION_CANCELED_ACTIVE,
    SUBSCRIPTION_IN_GRACE,
    PAYMENT_ISSUE,
    SUBSCRIPTION_REVOKED,
    VERIFICATION_UNAVAILABLE,
    REFRESH_BACKEND_FAILED,
    ACKNOWLEDGE_PURCHASE_FAILED,
}
