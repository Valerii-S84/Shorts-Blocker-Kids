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
    private val clientOnlyModeRequested: Boolean = false,
    private val internalTestingBuild: Boolean = false,
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
    private val verificationPolicy =
        BillingVerificationPolicy(
            clientOnlyModeRequested = clientOnlyModeRequested,
            internalTestingBuild = internalTestingBuild,
        )

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
                message = BillingUiMessage(BillingMessageCode.CONNECTING),
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
                                message = BillingUiMessage(BillingMessageCode.CONNECTED),
                            )
                        }
                        queryProductDetails()
                        queryPurchases()
                    } else {
                        setBillingError(BillingMessageCode.BILLING_UNAVAILABLE, billingResult)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnecting = false
                    _uiState.update {
                        it.copy(
                            isReady = false,
                            isLoading = false,
                            canStartPurchase = false,
                            message = BillingUiMessage(BillingMessageCode.DISCONNECTED),
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
                    message = BillingUiMessage(BillingMessageCode.SUBSCRIPTION_NOT_READY),
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
                message = BillingUiMessage(BillingMessageCode.OPENING_PURCHASE_FLOW),
            )
        }
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            setBillingError(BillingMessageCode.OPEN_PURCHASE_FLOW_FAILED, result)
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
                    message =
                        BillingUiMessage(BillingMessageCode.MANAGE_SUBSCRIPTION_UNAVAILABLE),
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
                _uiState.update {
                    it.copy(message = BillingUiMessage(BillingMessageCode.PURCHASE_CANCELED))
                }
                queryPurchases()
            }

            else -> setBillingError(BillingMessageCode.PURCHASE_FAILED, billingResult)
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
                setBillingError(BillingMessageCode.LOAD_SUBSCRIPTION_FAILED, billingResult)
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
                    message =
                        BillingUiMessage(
                            if (details == null) {
                                BillingMessageCode.PRODUCT_UNAVAILABLE
                            } else {
                                BillingMessageCode.PRODUCT_LOADED
                            },
                        ),
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
                setBillingError(BillingMessageCode.RESTORE_PURCHASES_FAILED, billingResult)
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

        if (verificationPolicy.canUseClientOnlyEntitlement) {
            purchased
                .filterNot { it.isAcknowledged }
                .forEach(::acknowledgePurchase)
        }

        onEntitlementChanged(
            verificationPolicy.localPurchaseSnapshot(
                hasPurchasedSubscription = purchased.isNotEmpty(),
                checkedAtMillis = nowMillis(),
            ),
        )
        _uiState.update {
            it.copy(
                isLoading = false,
                message =
                    BillingUiMessage(
                        verificationPolicy.localPurchaseMessageCode(
                            hasPurchasedSubscription = purchased.isNotEmpty(),
                            hasPendingSubscription = hasPending,
                        ),
                    ),
            )
        }
    }

    private fun verifyPurchaseWithBackend(
        purchase: Purchase,
        hasPending: Boolean,
    ) {
        val currentInstallId = installId
        if (currentInstallId.isNullOrBlank()) {
            recordFailClosedEntitlement()
            _uiState.update {
                it.copy(
                    message =
                        BillingUiMessage(BillingMessageCode.BACKEND_VERIFICATION_NOT_READY),
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                message = BillingUiMessage(BillingMessageCode.VERIFYING_WITH_BACKEND),
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
                        message = BillingUiMessage(snapshot.messageCode(hasPending)),
                    )
                }
            }.onFailure {
                recordFailClosedEntitlement()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message =
                            BillingUiMessage(BillingMessageCode.VERIFY_WITH_BACKEND_FAILED),
                    )
                }
            }
        }
    }

    private fun refreshEntitlementFromBackend(hasPending: Boolean) {
        val currentInstallId = installId
        if (currentInstallId.isNullOrBlank()) {
            recordFailClosedEntitlement()
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
                            message =
                                BillingUiMessage(
                                    if (hasPending) {
                                        BillingMessageCode.PURCHASE_PENDING
                                    } else {
                                        BillingMessageCode.NO_ACTIVE_SUBSCRIPTION
                                    },
                                ),
                        )
                    }
                    return@onSuccess
                }
                onEntitlementChanged(snapshot)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = BillingUiMessage(snapshot.messageCode(hasPending)),
                    )
                }
            }.onFailure {
                recordFailClosedEntitlement()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = BillingUiMessage(BillingMessageCode.REFRESH_BACKEND_FAILED),
                    )
                }
            }
        }
    }

    private fun recordFailClosedEntitlement() {
        onEntitlementChanged(verificationPolicy.failClosedSnapshot(checkedAtMillis = nowMillis()))
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params =
            AcknowledgePurchaseParams
                .newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                setBillingError(BillingMessageCode.ACKNOWLEDGE_PURCHASE_FAILED, billingResult)
            }
        }
    }

    private fun BillingEntitlementSnapshot.messageCode(hasPending: Boolean): BillingMessageCode =
        when {
            state == BillingEntitlementState.ACTIVE -> BillingMessageCode.SUBSCRIPTION_ACTIVE
            state == BillingEntitlementState.CANCELED_ACTIVE ->
                BillingMessageCode.SUBSCRIPTION_CANCELED_ACTIVE
            state == BillingEntitlementState.IN_GRACE ->
                BillingMessageCode.SUBSCRIPTION_IN_GRACE
            state == BillingEntitlementState.PENDING || hasPending ->
                BillingMessageCode.PURCHASE_PENDING
            state == BillingEntitlementState.ON_HOLD ->
                BillingMessageCode.PAYMENT_ISSUE
            state == BillingEntitlementState.REVOKED -> BillingMessageCode.SUBSCRIPTION_REVOKED
            state == BillingEntitlementState.EXPIRED -> BillingMessageCode.NO_ACTIVE_SUBSCRIPTION
            else -> BillingMessageCode.VERIFICATION_UNAVAILABLE
        }

    private fun setBillingError(
        messageCode: BillingMessageCode,
        billingResult: BillingResult,
    ) {
        isConnecting = false
        _uiState.update {
            it.copy(
                isReady = billingClient.isReady,
                isLoading = false,
                isPurchaseInProgress = false,
                canStartPurchase = productDetails != null && subscriptionOfferToken != null,
                message =
                    BillingUiMessage(
                        code = messageCode,
                        responseCode = billingResult.responseCode,
                    ),
            )
        }
    }
}
