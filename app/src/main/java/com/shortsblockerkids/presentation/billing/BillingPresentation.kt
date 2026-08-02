package com.shortsblockerkids.presentation.billing

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shortsblockerkids.R

@Composable
fun billingMessageText(message: BillingUiMessage): String {
    val messageText = stringResource(message.code.stringRes())
    val responseCode = message.responseCode ?: return messageText

    return stringResource(
        R.string.billing_error_with_response_code,
        messageText,
        responseCode,
    )
}

@Composable
fun billingSubscriptionStatusText(
    hasBillingEntitlement: Boolean,
    billingUiState: BillingUiState,
): String =
    when {
        hasBillingEntitlement -> stringResource(R.string.status_active)
        billingUiState.isPurchaseInProgress ->
            stringResource(R.string.billing_status_purchase_in_progress)
        billingUiState.productPrice != null ->
            stringResource(
                R.string.billing_status_available_price,
                billingUiState.productPrice,
            )
        else -> billingMessageText(billingUiState.message)
    }

@Composable
fun billingSubscriptionTermsText(productPrice: String?): String =
    if (productPrice == null) {
        stringResource(R.string.subscription_terms_price_pending)
    } else {
        stringResource(R.string.subscription_terms_with_price, productPrice)
    }

@StringRes
private fun BillingMessageCode.stringRes(): Int =
    when (this) {
        BillingMessageCode.CONNECTING -> R.string.billing_status_connecting
        BillingMessageCode.CONNECTED -> R.string.billing_status_connected
        BillingMessageCode.BILLING_UNAVAILABLE -> R.string.billing_error_unavailable
        BillingMessageCode.DISCONNECTED -> R.string.billing_status_disconnected
        BillingMessageCode.SUBSCRIPTION_NOT_READY ->
            R.string.billing_status_subscription_not_ready
        BillingMessageCode.OPENING_PURCHASE_FLOW ->
            R.string.billing_status_opening_purchase_flow
        BillingMessageCode.OPEN_PURCHASE_FLOW_FAILED ->
            R.string.billing_error_open_purchase_flow
        BillingMessageCode.MANAGE_SUBSCRIPTION_UNAVAILABLE ->
            R.string.billing_error_manage_subscription_unavailable
        BillingMessageCode.PURCHASE_CANCELED -> R.string.billing_status_purchase_canceled
        BillingMessageCode.PURCHASE_FAILED -> R.string.billing_error_purchase_failed
        BillingMessageCode.LOAD_SUBSCRIPTION_FAILED ->
            R.string.billing_error_load_subscription
        BillingMessageCode.PRODUCT_UNAVAILABLE -> R.string.billing_status_product_unavailable
        BillingMessageCode.PRODUCT_LOADED -> R.string.billing_status_product_loaded
        BillingMessageCode.RESTORE_PURCHASES_FAILED ->
            R.string.billing_error_restore_purchases
        BillingMessageCode.SUBSCRIPTION_ACTIVE -> R.string.billing_status_subscription_active
        BillingMessageCode.BACKEND_VERIFICATION_REQUIRED ->
            R.string.billing_status_backend_verification_required
        BillingMessageCode.PURCHASE_PENDING -> R.string.billing_status_purchase_pending
        BillingMessageCode.NO_ACTIVE_SUBSCRIPTION ->
            R.string.billing_status_no_active_subscription
        BillingMessageCode.BACKEND_VERIFICATION_NOT_READY ->
            R.string.billing_status_backend_verification_not_ready
        BillingMessageCode.VERIFYING_WITH_BACKEND ->
            R.string.billing_status_verifying_with_backend
        BillingMessageCode.VERIFY_WITH_BACKEND_FAILED ->
            R.string.billing_error_verify_with_backend
        BillingMessageCode.SUBSCRIPTION_CANCELED_ACTIVE ->
            R.string.billing_status_subscription_canceled_active
        BillingMessageCode.SUBSCRIPTION_IN_GRACE ->
            R.string.billing_status_subscription_in_grace
        BillingMessageCode.PAYMENT_ISSUE -> R.string.billing_status_payment_issue
        BillingMessageCode.SUBSCRIPTION_REVOKED ->
            R.string.billing_status_subscription_revoked
        BillingMessageCode.VERIFICATION_UNAVAILABLE ->
            R.string.billing_status_verification_unavailable
        BillingMessageCode.REFRESH_BACKEND_FAILED ->
            R.string.billing_error_refresh_backend
        BillingMessageCode.ACKNOWLEDGE_PURCHASE_FAILED ->
            R.string.billing_error_acknowledge_purchase
    }
