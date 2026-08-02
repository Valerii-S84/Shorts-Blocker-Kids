package com.shortsblockerkids.infrastructure.billing

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.shortsblockerkids.application.billing.BillingPurchaseSummary
import com.shortsblockerkids.application.billing.PlayBillingEvent
import com.shortsblockerkids.application.billing.PlayBillingGateway
import com.shortsblockerkids.application.billing.PlayBillingOperation

class GooglePlayBillingGateway(
    private val activity: Activity,
) : PlayBillingGateway,
    PurchasesUpdatedListener {
    private val billingClient =
        BillingClient
            .newBuilder(activity.applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            ).enableAutoServiceReconnection()
            .build()

    private var eventListener: (PlayBillingEvent) -> Unit = {}
    private var isConnecting = false
    private var productDetails: ProductDetails? = null
    private var subscriptionOfferToken: String? = null

    override fun setEventListener(listener: (PlayBillingEvent) -> Unit) {
        eventListener = listener
    }

    override fun start() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryPurchases()
            return
        }
        if (isConnecting) {
            return
        }

        isConnecting = true
        emit(PlayBillingEvent.Connecting)
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnecting = false
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        emit(PlayBillingEvent.Connected)
                        queryProductDetails()
                        queryPurchases()
                    } else {
                        emitFailure(PlayBillingOperation.SETUP, billingResult)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnecting = false
                    emit(PlayBillingEvent.Disconnected)
                }
            },
        )
    }

    override fun stop() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        isConnecting = false
    }

    override fun refreshPurchases() {
        if (!billingClient.isReady) {
            start()
            return
        }
        queryPurchases()
    }

    override fun launchPurchase() {
        val details = productDetails
        val offerToken = subscriptionOfferToken
        if (!billingClient.isReady || details == null || offerToken == null) {
            emit(PlayBillingEvent.SubscriptionNotReady)
            start()
            return
        }

        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        val billingFlowParams =
            BillingFlowParams
                .newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

        emit(PlayBillingEvent.OpeningPurchaseFlow)
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            emitFailure(PlayBillingOperation.OPEN_PURCHASE_FLOW, result)
        }
    }

    override fun openManageSubscription() {
        val uri =
            Uri
                .parse(PlayBillingConfig.MANAGE_SUBSCRIPTIONS_URL)
                .buildUpon()
                .appendQueryParameter("sku", PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID)
                .appendQueryParameter("package", activity.packageName)
                .build()
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            emit(PlayBillingEvent.ManageSubscriptionUnavailable)
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        emit(PlayBillingEvent.PurchaseFlowFinished)
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> emitPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                emit(PlayBillingEvent.PurchaseCanceled)
                queryPurchases()
            }

            else -> emitFailure(PlayBillingOperation.PURCHASE_UPDATE, billingResult)
        }
    }

    override fun acknowledgePurchase(purchaseToken: String) {
        val params =
            AcknowledgePurchaseParams
                .newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                emitFailure(PlayBillingOperation.ACKNOWLEDGE_PURCHASE, billingResult)
            }
        }
    }

    private fun queryProductDetails() {
        val params =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product
                            .newBuilder()
                            .setProductId(PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                    ),
                ).build()

        emit(PlayBillingEvent.ProductDetailsLoading)
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                emitFailure(PlayBillingOperation.LOAD_PRODUCT_DETAILS, billingResult)
                return@queryProductDetailsAsync
            }

            val details = productDetailsResult.productDetailsList.firstOrNull()
            productDetails = details
            subscriptionOfferToken = details.firstOfferToken()
            emit(
                PlayBillingEvent.ProductDetailsLoaded(
                    productPrice = details.firstFormattedPrice(),
                    isProductAvailable = details != null,
                    canStartPurchase = details != null && subscriptionOfferToken != null,
                ),
            )
        }
    }

    private fun queryPurchases() {
        val params =
            QueryPurchasesParams
                .newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                emitPurchases(purchases)
            } else {
                emitFailure(PlayBillingOperation.RESTORE_PURCHASES, billingResult)
            }
        }
    }

    private fun emitPurchases(purchases: List<Purchase>) {
        emit(PlayBillingEvent.PurchasesObserved(purchases.toBillingPurchaseSummary()))
    }

    private fun emitFailure(
        operation: PlayBillingOperation,
        billingResult: BillingResult,
    ) {
        isConnecting = false
        emit(
            PlayBillingEvent.OperationFailed(
                operation = operation,
                responseCode = billingResult.responseCode,
                isReady = billingClient.isReady,
                canStartPurchase = productDetails != null && subscriptionOfferToken != null,
            ),
        )
    }

    private fun emit(event: PlayBillingEvent) {
        eventListener(event)
    }

    private fun ProductDetails?.firstOfferToken(): String? =
        this
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken

    private fun ProductDetails?.firstFormattedPrice(): String? =
        this
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice

    private fun List<Purchase>.toBillingPurchaseSummary(): BillingPurchaseSummary {
        val subscriptionPurchases =
            filter { purchase ->
                PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID in purchase.products
            }
        val purchasedSubscriptions =
            subscriptionPurchases.filter { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

        return BillingPurchaseSummary(
            purchasedSubscriptionToken = purchasedSubscriptions.firstOrNull()?.purchaseToken,
            hasPendingSubscription =
                subscriptionPurchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PENDING
                },
            unacknowledgedPurchasedSubscriptionTokens =
                purchasedSubscriptions
                    .filterNot { purchase -> purchase.isAcknowledged }
                    .map { purchase -> purchase.purchaseToken },
        )
    }
}
