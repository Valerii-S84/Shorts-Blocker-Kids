package com.shortsblockerkids.presentation.billing

import com.shortsblockerkids.application.billing.BillingSyncOutcome
import com.shortsblockerkids.application.billing.PlayBillingEvent
import com.shortsblockerkids.application.billing.PlayBillingGateway
import com.shortsblockerkids.application.billing.SyncBillingEntitlementUseCase
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillingCoordinator(
    private val billingGateway: PlayBillingGateway,
    private val syncBillingEntitlementUseCase: SyncBillingEntitlementUseCase,
    private val onEntitlementChanged: suspend (BillingEntitlementSnapshot) -> Unit,
    private val billingScope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState

    init {
        billingGateway.setEventListener(::onBillingEvent)
    }

    fun start() {
        billingGateway.start()
    }

    fun stop() {
        billingGateway.stop()
    }

    fun refreshPurchases() {
        billingGateway.refreshPurchases()
    }

    fun launchPurchase() {
        billingGateway.launchPurchase()
    }

    fun openManageSubscription() {
        billingGateway.openManageSubscription()
    }

    private fun onBillingEvent(event: PlayBillingEvent) {
        when (event) {
            PlayBillingEvent.Connecting -> updateConnecting()
            PlayBillingEvent.Connected -> updateConnected()
            PlayBillingEvent.Disconnected -> updateDisconnected()
            PlayBillingEvent.ProductDetailsLoading ->
                _uiState.update { it.copy(isLoading = true) }
            PlayBillingEvent.OpeningPurchaseFlow -> updateOpeningPurchaseFlow()
            PlayBillingEvent.PurchaseFlowFinished ->
                _uiState.update { it.copy(isPurchaseInProgress = false) }
            is PlayBillingEvent.ProductDetailsLoaded -> updateProductDetails(event)
            is PlayBillingEvent.PurchasesObserved -> processPurchases(event)
            is PlayBillingEvent.OperationFailed -> updateFailure(event)
            PlayBillingEvent.SubscriptionNotReady ->
                updateMessage(BillingMessageCode.SUBSCRIPTION_NOT_READY)
            PlayBillingEvent.ManageSubscriptionUnavailable ->
                updateMessage(BillingMessageCode.MANAGE_SUBSCRIPTION_UNAVAILABLE)
            PlayBillingEvent.PurchaseCanceled -> updateMessage(BillingMessageCode.PURCHASE_CANCELED)
        }
    }

    private fun updateConnecting() {
        _uiState.update {
            it.copy(
                isLoading = true,
                message = BillingUiMessage(BillingMessageCode.CONNECTING),
            )
        }
    }

    private fun updateConnected() {
        _uiState.update {
            it.copy(
                isReady = true,
                isLoading = false,
                message = BillingUiMessage(BillingMessageCode.CONNECTED),
            )
        }
    }

    private fun updateDisconnected() {
        _uiState.update {
            it.copy(
                isReady = false,
                isLoading = false,
                canStartPurchase = false,
                message = BillingUiMessage(BillingMessageCode.DISCONNECTED),
            )
        }
    }

    private fun updateOpeningPurchaseFlow() {
        _uiState.update {
            it.copy(
                isPurchaseInProgress = true,
                message = BillingUiMessage(BillingMessageCode.OPENING_PURCHASE_FLOW),
            )
        }
    }

    private fun updateProductDetails(event: PlayBillingEvent.ProductDetailsLoaded) {
        _uiState.update {
            it.copy(
                isLoading = false,
                productPrice = event.productPrice,
                canStartPurchase = event.canStartPurchase,
                message =
                    BillingUiMessage(
                        if (event.isProductAvailable) {
                            BillingMessageCode.PRODUCT_LOADED
                        } else {
                            BillingMessageCode.PRODUCT_UNAVAILABLE
                        },
                    ),
            )
        }
    }

    private fun updateFailure(event: PlayBillingEvent.OperationFailed) {
        _uiState.update {
            it.copy(
                isReady = event.isReady,
                isLoading = false,
                isPurchaseInProgress = false,
                canStartPurchase = event.canStartPurchase,
                message =
                    BillingUiMessage(
                        code = event.operation.toBillingMessageCode(),
                        responseCode = event.responseCode,
                    ),
            )
        }
    }

    private fun updateMessage(messageCode: BillingMessageCode) {
        _uiState.update { it.copy(message = BillingUiMessage(messageCode)) }
    }

    private fun processPurchases(event: PlayBillingEvent.PurchasesObserved) {
        billingScope.launch {
            applyBillingSyncOutcome(syncBillingEntitlementUseCase(event.summary))
        }
    }

    private suspend fun applyBillingSyncOutcome(outcome: BillingSyncOutcome) {
        if (!persistEntitlement(outcome.entitlementSnapshot)) return
        outcome.purchaseTokensToAcknowledge.forEach(billingGateway::acknowledgePurchase)
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                message = outcome.toBillingUiMessage(),
            )
        }
    }

    private suspend fun persistEntitlement(snapshot: BillingEntitlementSnapshot): Boolean =
        try {
            onEntitlementChanged(snapshot)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPurchaseInProgress = false,
                    message = BillingUiMessage(BillingMessageCode.ENTITLEMENT_SAVE_FAILED),
                )
            }
            false
        }
}
