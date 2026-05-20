package com.shortsblockerkids.core.billing

import android.app.Activity
import android.content.Context
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
import com.shortsblockerkids.core.model.SubscriptionState

class BillingClientGateway(
    context: Context,
    private val onEntitlementChanged: (SubscriptionState) -> Unit,
    private val onBillingMessage: (String) -> Unit,
    private val productIds: List<String> = BillingProductIds.subscriptions,
) : PurchasesUpdatedListener {
    private val billingClient =
        BillingClient
            .newBuilder(context.applicationContext)
            .setListener(this)
            .enableAutoServiceReconnection()
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            ).build()

    fun start() {
        if (billingClient.isReady) {
            refreshBillingState()
            return
        }

        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTING) {
            return
        }

        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        refreshBillingState()
                    } else {
                        onBillingMessage("Google Play Billing unavailable.")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    onBillingMessage("Google Play Billing disconnected.")
                }
            },
        )
    }

    fun refreshBillingState() {
        if (!billingClient.isReady) {
            start()
            return
        }

        if (!subscriptionsAreSupported()) {
            return
        }

        queryProductDetails(productIds = productIds)
        queryPurchases()
    }

    fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
    ) {
        if (!billingClient.isReady) {
            start()
            onBillingMessage("Google Play Billing is connecting.")
            return
        }

        if (!subscriptionsAreSupported()) {
            return
        }

        queryProductDetails(productIds = listOf(productId)) { productDetails ->
            val selectedProduct = productDetails.firstOrNull { it.productId == productId }
            if (selectedProduct == null) {
                onBillingMessage("Subscription product is unavailable.")
                return@queryProductDetails
            }

            launchSubscriptionPurchase(activity, selectedProduct)
        }
    }

    private fun launchSubscriptionPurchase(
        activity: Activity,
        productDetails: ProductDetails,
    ) {
        val offerToken = productDetails.selectSubscriptionOfferToken()
        if (offerToken.isNullOrBlank()) {
            onBillingMessage("Subscription offer is unavailable.")
            return
        }

        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        val billingFlowParams =
            BillingFlowParams
                .newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        handleBillingFlowResult(result)
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            onBillingMessage("Purchase was not completed.")
            return
        }

        processPurchases(purchases.orEmpty())
    }

    private fun queryProductDetails(
        productIds: List<String>,
        onProductDetailsLoaded: (List<ProductDetails>) -> Unit = {},
    ) {
        val products =
            productIds.map { productId ->
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
        val params =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(products)
                .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onBillingMessage("Subscription products unavailable.")
                return@queryProductDetailsAsync
            }

            val fetchedProductIds = result.productDetailsList.mapTo(mutableSetOf()) { it.productId }
            if (productIds.any { it !in fetchedProductIds }) {
                onBillingMessage("Some subscription products are unavailable.")
            }
            onProductDetailsLoaded(result.productDetailsList)
        }
    }

    private fun queryPurchases() {
        val params =
            QueryPurchasesParams
                .newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onBillingMessage("Subscription status unavailable.")
                return@queryPurchasesAsync
            }

            processPurchases(purchases)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val subscriptionPurchases =
            purchases.filter { purchase ->
                purchase.products.any(productIds::contains)
            }

        subscriptionPurchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .filterNot(Purchase::isAcknowledged)
            .forEach(::acknowledgePurchase)

        onEntitlementChanged(
            EntitlementResolver.resolve(
                subscriptionPurchases.map(Purchase::toSnapshot),
            ),
        )
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params =
            AcknowledgePurchaseParams
                .newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onBillingMessage("Subscription acknowledgement pending.")
            }
        }
    }

    private fun subscriptionsAreSupported(): Boolean {
        val result = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            return true
        }

        onBillingMessage("Google Play subscriptions are unavailable on this device.")
        return false
    }

    private fun handleBillingFlowResult(result: BillingResult) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                onBillingMessage("Opening Google Play purchase screen.")

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                onBillingMessage("Subscription already active.")
                queryPurchases()
            }

            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                onBillingMessage("Google Play Billing unavailable.")

            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                onBillingMessage("Subscription product unavailable.")

            else ->
                onBillingMessage("Purchase flow could not be opened.")
        }
    }
}

private fun Purchase.toSnapshot(): BillingPurchaseSnapshot =
    BillingPurchaseSnapshot(
        productIds = products,
        status = toBillingPurchaseStatus(),
        isAutoRenewing = isAutoRenewing,
    )

private fun Purchase.toBillingPurchaseStatus(): BillingPurchaseStatus =
    when (purchaseState) {
        Purchase.PurchaseState.PURCHASED -> BillingPurchaseStatus.PURCHASED
        Purchase.PurchaseState.PENDING -> BillingPurchaseStatus.PENDING
        else -> BillingPurchaseStatus.UNSPECIFIED
    }

private fun ProductDetails.selectSubscriptionOfferToken(): String? =
    subscriptionOfferDetails
        ?.filter { it.offerToken.isNotBlank() }
        ?.sortedWith(
            compareByDescending<ProductDetails.SubscriptionOfferDetails> { it.hasFreeTrialPhase() }
                .thenBy { it.basePlanId }
                .thenBy { it.offerId.orEmpty() },
        )?.firstOrNull()
        ?.offerToken

private fun ProductDetails.SubscriptionOfferDetails.hasFreeTrialPhase(): Boolean =
    pricingPhases.pricingPhaseList.any { pricingPhase ->
        pricingPhase.priceAmountMicros == 0L && pricingPhase.billingCycleCount > 0
    }
