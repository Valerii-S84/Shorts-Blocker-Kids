package com.shortsblockerkids.presentation.billing

import com.shortsblockerkids.application.billing.BillingSyncOutcome
import com.shortsblockerkids.application.billing.BillingSyncStatus
import com.shortsblockerkids.application.billing.PlayBillingEvent
import com.shortsblockerkids.application.billing.PlayBillingOperation
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState

internal fun PlayBillingEvent.toBillingUiMessage(): BillingUiMessage? =
    when (this) {
        PlayBillingEvent.Connecting -> BillingUiMessage(BillingMessageCode.CONNECTING)
        PlayBillingEvent.Connected -> BillingUiMessage(BillingMessageCode.CONNECTED)
        PlayBillingEvent.Disconnected -> BillingUiMessage(BillingMessageCode.DISCONNECTED)
        PlayBillingEvent.SubscriptionNotReady ->
            BillingUiMessage(BillingMessageCode.SUBSCRIPTION_NOT_READY)
        PlayBillingEvent.OpeningPurchaseFlow ->
            BillingUiMessage(BillingMessageCode.OPENING_PURCHASE_FLOW)
        PlayBillingEvent.ManageSubscriptionUnavailable ->
            BillingUiMessage(BillingMessageCode.MANAGE_SUBSCRIPTION_UNAVAILABLE)
        PlayBillingEvent.PurchaseCanceled ->
            BillingUiMessage(BillingMessageCode.PURCHASE_CANCELED)
        is PlayBillingEvent.ProductDetailsLoaded ->
            BillingUiMessage(
                if (isProductAvailable) {
                    BillingMessageCode.PRODUCT_LOADED
                } else {
                    BillingMessageCode.PRODUCT_UNAVAILABLE
                },
            )
        is PlayBillingEvent.OperationFailed ->
            BillingUiMessage(
                code = operation.messageCode(),
                responseCode = responseCode,
            )
        PlayBillingEvent.ProductDetailsLoading,
        PlayBillingEvent.PurchaseFlowFinished,
        is PlayBillingEvent.PurchasesObserved,
        -> null
    }

internal fun BillingSyncOutcome.toBillingUiMessage(): BillingUiMessage? =
    when (status) {
        null -> null
        BillingSyncStatus.ENTITLEMENT_RESOLVED ->
            BillingUiMessage(resolvedEntitlementMessageCode())
        BillingSyncStatus.BACKEND_VERIFICATION_REQUIRED ->
            BillingUiMessage(BillingMessageCode.BACKEND_VERIFICATION_REQUIRED)
        BillingSyncStatus.BACKEND_INSTALLATION_ID_UNAVAILABLE ->
            BillingUiMessage(BillingMessageCode.BACKEND_VERIFICATION_NOT_READY)
        BillingSyncStatus.BACKEND_PURCHASE_VERIFICATION_FAILED ->
            BillingUiMessage(BillingMessageCode.VERIFY_WITH_BACKEND_FAILED)
        BillingSyncStatus.BACKEND_ENTITLEMENT_REFRESH_FAILED ->
            BillingUiMessage(BillingMessageCode.REFRESH_BACKEND_FAILED)
    }

internal fun billingVerificationProgressMessage(): BillingUiMessage = BillingUiMessage(BillingMessageCode.VERIFYING_WITH_BACKEND)

private fun PlayBillingOperation.messageCode(): BillingMessageCode =
    when (this) {
        PlayBillingOperation.SETUP -> BillingMessageCode.BILLING_UNAVAILABLE
        PlayBillingOperation.OPEN_PURCHASE_FLOW ->
            BillingMessageCode.OPEN_PURCHASE_FLOW_FAILED
        PlayBillingOperation.PURCHASE_UPDATE -> BillingMessageCode.PURCHASE_FAILED
        PlayBillingOperation.LOAD_PRODUCT_DETAILS ->
            BillingMessageCode.LOAD_SUBSCRIPTION_FAILED
        PlayBillingOperation.RESTORE_PURCHASES ->
            BillingMessageCode.RESTORE_PURCHASES_FAILED
        PlayBillingOperation.ACKNOWLEDGE_PURCHASE ->
            BillingMessageCode.ACKNOWLEDGE_PURCHASE_FAILED
    }

private fun BillingSyncOutcome.resolvedEntitlementMessageCode(): BillingMessageCode =
    when {
        entitlementSnapshot.state == BillingEntitlementState.ACTIVE ->
            BillingMessageCode.SUBSCRIPTION_ACTIVE
        entitlementSnapshot.state == BillingEntitlementState.CANCELED_ACTIVE ->
            BillingMessageCode.SUBSCRIPTION_CANCELED_ACTIVE
        entitlementSnapshot.state == BillingEntitlementState.IN_GRACE ->
            BillingMessageCode.SUBSCRIPTION_IN_GRACE
        entitlementSnapshot.state == BillingEntitlementState.PENDING || hasPendingSubscription ->
            BillingMessageCode.PURCHASE_PENDING
        entitlementSnapshot.state == BillingEntitlementState.ON_HOLD ->
            BillingMessageCode.PAYMENT_ISSUE
        entitlementSnapshot.state == BillingEntitlementState.REVOKED ->
            BillingMessageCode.SUBSCRIPTION_REVOKED
        entitlementSnapshot.state == BillingEntitlementState.EXPIRED ->
            BillingMessageCode.NO_ACTIVE_SUBSCRIPTION
        else -> BillingMessageCode.VERIFICATION_UNAVAILABLE
    }
