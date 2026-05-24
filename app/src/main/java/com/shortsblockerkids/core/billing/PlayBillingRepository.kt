package com.shortsblockerkids.core.billing

import android.app.Activity
import android.content.ActivityNotFoundException
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayBillingRepository(
    context: Context,
    private val onEntitlementChanged: (BillingEntitlementSnapshot) -> Unit,
    private val billingBackendClient: BillingBackendClient = DisabledBillingBackendClient,
    private val installId: String? = null,
    private val appVersion: String = "",
    private val billingScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
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
    private val packageName = context.applicationContext.packageName

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
                it.copy(
                    statusMessage =
                        "Subscription is not ready yet. Check Google Play or restore purchases.",
                )
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
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            _uiState.update {
                it.copy(
                    statusMessage =
                        "Could not open Google Play subscription management on this device.",
                )
            }
        }
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

        if (billingBackendClient.isConfigured && purchased.isNotEmpty()) {
            verifyPurchaseWithBackend(purchased.first(), hasPending)
            return
        }

        if (billingBackendClient.isConfigured && purchased.isEmpty()) {
            refreshEntitlementFromBackend(hasPending)
            return
        }

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

    private fun verifyPurchaseWithBackend(
        purchase: Purchase,
        hasPending: Boolean,
    ) {
        val currentInstallId = installId
        if (currentInstallId.isNullOrBlank()) {
            _uiState.update {
                it.copy(statusMessage = "Billing backend verification is not ready yet.")
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                statusMessage = "Verifying subscription with secure backend.",
            )
        }
        billingScope.launch {
            runCatching {
                billingBackendClient.verifyPurchase(
                    BillingBackendPurchaseRequest(
                        installId = currentInstallId,
                        packageName = packageName,
                        productId = BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID,
                        purchaseToken = purchase.purchaseToken,
                        appVersion = appVersion,
                    ),
                )
            }.onSuccess { snapshot ->
                onEntitlementChanged(snapshot)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = snapshot.statusMessage(hasPending),
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage =
                            "Could not verify subscription with backend. Restore or try again.",
                    )
                }
            }
        }
    }

    private fun refreshEntitlementFromBackend(hasPending: Boolean) {
        val currentInstallId = installId
        if (currentInstallId.isNullOrBlank()) {
            onEntitlementChanged(
                BillingEntitlementSnapshot(
                    state = BillingEntitlementState.EXPIRED,
                    checkedAtMillis = nowMillis(),
                ),
            )
            return
        }

        billingScope.launch {
            runCatching {
                billingBackendClient.refreshEntitlement(currentInstallId)
            }.onSuccess { snapshot ->
                if (snapshot == null) {
                    onEntitlementChanged(
                        BillingEntitlementSnapshot(
                            state = BillingEntitlementState.EXPIRED,
                            checkedAtMillis = nowMillis(),
                        ),
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage =
                                if (hasPending) {
                                    "Purchase pending. Protection unlocks after payment completes."
                                } else {
                                    "No active Google Play subscription found."
                                },
                        )
                    }
                    return@onSuccess
                }
                onEntitlementChanged(snapshot)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = snapshot.statusMessage(hasPending),
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage =
                            "Could not refresh subscription with backend. Restore or try again.",
                    )
                }
            }
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

    private fun BillingEntitlementSnapshot.statusMessage(hasPending: Boolean): String =
        when {
            state == BillingEntitlementState.ACTIVE -> "Subscription active."
            state == BillingEntitlementState.CANCELED_ACTIVE ->
                "Subscription active until the paid period ends."
            state == BillingEntitlementState.IN_GRACE ->
                "Subscription active during Google Play grace period."
            state == BillingEntitlementState.PENDING || hasPending ->
                "Purchase pending. Protection unlocks after payment completes."
            state == BillingEntitlementState.ON_HOLD ->
                "Payment issue. Update the subscription in Google Play."
            state == BillingEntitlementState.REVOKED -> "Subscription revoked by Google Play."
            state == BillingEntitlementState.EXPIRED -> "No active Google Play subscription found."
            else -> "Subscription verification unavailable."
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
