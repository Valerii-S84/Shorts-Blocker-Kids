package com.shortsblockerkids.presentation.billing

import com.shortsblockerkids.application.billing.BillingSyncOutcome
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
                code = operation.toBillingMessageCode(),
                responseCode = responseCode,
            )
        PlayBillingEvent.ProductDetailsLoading,
        PlayBillingEvent.PurchaseFlowFinished,
        is PlayBillingEvent.PurchasesObserved,
        -> null
    }

internal fun BillingSyncOutcome.toBillingUiMessage(): BillingUiMessage = BillingUiMessage(resolvedEntitlementMessageCode())

internal fun PlayBillingOperation.toBillingMessageCode(): BillingMessageCode =
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
        entitlementSnapshot.isActive ->
            BillingMessageCode.SUBSCRIPTION_ACTIVE
        entitlementSnapshot.state == BillingEntitlementState.PENDING ||
            hasPendingSubscription ->
            BillingMessageCode.PURCHASE_PENDING
        else -> BillingMessageCode.NO_ACTIVE_SUBSCRIPTION
    }
