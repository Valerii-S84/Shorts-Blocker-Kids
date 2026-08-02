package com.shortsblockerkids.application.billing

interface PlayBillingGateway {
    fun setEventListener(listener: (PlayBillingEvent) -> Unit)

    fun start()

    fun stop()

    fun refreshPurchases()

    fun launchPurchase()

    fun openManageSubscription()

    fun acknowledgePurchase(purchaseToken: String)
}

sealed interface PlayBillingEvent {
    data object Connecting : PlayBillingEvent

    data object Connected : PlayBillingEvent

    data object Disconnected : PlayBillingEvent

    data object ProductDetailsLoading : PlayBillingEvent

    data object SubscriptionNotReady : PlayBillingEvent

    data object OpeningPurchaseFlow : PlayBillingEvent

    data object PurchaseFlowFinished : PlayBillingEvent

    data object ManageSubscriptionUnavailable : PlayBillingEvent

    data object PurchaseCanceled : PlayBillingEvent

    data class ProductDetailsLoaded(
        val productPrice: String?,
        val isProductAvailable: Boolean,
        val canStartPurchase: Boolean,
    ) : PlayBillingEvent

    data class PurchasesObserved(
        val summary: BillingPurchaseSummary,
    ) : PlayBillingEvent

    data class OperationFailed(
        val operation: PlayBillingOperation,
        val responseCode: Int,
        val isReady: Boolean,
        val canStartPurchase: Boolean,
    ) : PlayBillingEvent
}

enum class PlayBillingOperation {
    SETUP,
    OPEN_PURCHASE_FLOW,
    PURCHASE_UPDATE,
    LOAD_PRODUCT_DETAILS,
    RESTORE_PURCHASES,
    ACKNOWLEDGE_PURCHASE,
}
