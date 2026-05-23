package com.shortsblockerkids.core.billing

import android.app.Activity
import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PlayBillingRepository(
    context: Context,
    private val onEntitlementChanged: (BillingEntitlementSnapshot) -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : PurchasesUpdatedListener {
    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState

    private val billingClient =
        BillingClient
            .newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            ).enableAutoServiceReconnection()
            .build()

    private var isConnecting = false
    private var productDetails: ProductDetails? = null
    private var subscriptionOfferToken: String? = null

    fun start() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryPurchases()
            return
        }
        if (isConnecting) {
            return
        }

        isConnecting = true
        _uiState.update {
            it.copy(
                isLoading = true,
                statusMessage = "Connecting to Google Play Billing.",
            )
        }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnecting = false
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _uiState.update {
                            it.copy(
                                isReady = true,
                                isLoading = false,
                                statusMessage = "Google Play Billing connected.",
                            )
                        }
                        queryProductDetails()
                        queryPurchases()
                    } else {
                        setBillingError("Billing unavailable", billingResult)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnecting = false
                    _uiState.update {
                        it.copy(
                            isReady = false,
                            isLoading = false,
                            canStartPurchase = false,
                            statusMessage = "Google Play Billing disconnected.",
                        )
                    }
                }
            },
        )
    }

    fun stop() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        isConnecting = false
    }

    fun refreshPurchases() {
        if (!billingClient.isReady) {
            start()
            return
        }
        queryPurchases()
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails
        val offerToken = subscriptionOfferToken
        if (!billingClient.isReady || details == null || offerToken == null) {
            _uiState.update {
                it.copy(statusMessage = "Subscription is not ready yet. Try restore first.")
            }
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

        _uiState.update {
            it.copy(
                isPurchaseInProgress = true,
                statusMessage = "Opening Google Play purchase flow.",
            )
        }
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            setBillingError("Could not open purchase flow", result)
        }
    }

    fun openManageSubscription(activity: Activity) {
        val uri =
            Uri
                .parse("https://play.google.com/store/account/subscriptions")
                .buildUpon()
                .appendQueryParameter("sku", BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID)
                .appendQueryParameter("package", activity.packageName)
                .build()
        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        _uiState.update { it.copy(isPurchaseInProgress = false) }
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _uiState.update { it.copy(statusMessage = "Purchase canceled.") }
                queryPurchases()
            }

            else -> setBillingError("Purchase failed", billingResult)
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
                            .setProductId(BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                    ),
                ).build()

        _uiState.update { it.copy(isLoading = true) }
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                setBillingError("Could not load subscription", billingResult)
                return@queryProductDetailsAsync
            }

            val details = productDetailsResult.productDetailsList.firstOrNull()
            productDetails = details
            subscriptionOfferToken =
                details
                    ?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.offerToken
            val price =
                details
                    ?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice
            _uiState.update {
                it.copy(
                    isLoading = false,
                    productPrice = price,
                    canStartPurchase = details != null && subscriptionOfferToken != null,
                    statusMessage =
                        if (details == null) {
                            "Subscription product is not available yet."
                        } else {
                            "Subscription loaded from Google Play."
                        },
                )
            }
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
                processPurchases(purchases)
            } else {
                setBillingError("Could not restore purchases", billingResult)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val subscriptionPurchases =
            purchases.filter { purchase ->
                BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID in purchase.products
            }
        val purchased =
            subscriptionPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val hasPending =
            subscriptionPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }

        purchased
            .filterNot { it.isAcknowledged }
            .forEach(::acknowledgePurchase)

        onEntitlementChanged(
            BillingEntitlementSnapshot(
                isActive = purchased.isNotEmpty(),
                checkedAtMillis = nowMillis(),
            ),
        )
        _uiState.update {
            it.copy(
                isLoading = false,
                statusMessage =
                    when {
                        purchased.isNotEmpty() -> "Subscription active."
                        hasPending -> "Purchase pending. Protection unlocks after payment completes."
                        else -> "No active Google Play subscription found."
                    },
            )
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params =
            AcknowledgePurchaseParams
                .newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                setBillingError("Could not acknowledge purchase", billingResult)
            }
        }
    }

    private fun setBillingError(
        prefix: String,
        billingResult: BillingResult,
    ) {
        isConnecting = false
        _uiState.update {
            it.copy(
                isReady = billingClient.isReady,
                isLoading = false,
                isPurchaseInProgress = false,
                canStartPurchase = productDetails != null && subscriptionOfferToken != null,
                statusMessage = "$prefix (${billingResult.responseCode}).",
            )
        }
    }
}
