package com.shortsblockerkids.presentation.billing

import com.shortsblockerkids.application.billing.BillingSyncOutcome
import com.shortsblockerkids.application.billing.PlayBillingEvent
import com.shortsblockerkids.application.billing.PlayBillingGateway
import com.shortsblockerkids.application.billing.SyncBillingEntitlementUseCase
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillingCoordinator(
    private val billingGateway: PlayBillingGateway,
    private val syncBillingEntitlementUseCase: SyncBillingEntitlementUseCase,
    private val onEntitlementChanged: (BillingEntitlementSnapshot) -> Unit,
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
            PlayBillingEvent.Connecting -> updateConnecting(event)
            PlayBillingEvent.Connected -> updateConnected(event)
            PlayBillingEvent.Disconnected -> updateDisconnected(event)
            PlayBillingEvent.ProductDetailsLoading ->
                _uiState.update { it.copy(isLoading = true) }
            PlayBillingEvent.OpeningPurchaseFlow ->
                _uiState.update {
                    it.copy(
                        isPurchaseInProgress = true,
                        message = requireNotNull(event.toBillingUiMessage()),
                    )
                }
            PlayBillingEvent.PurchaseFlowFinished ->
                _uiState.update { it.copy(isPurchaseInProgress = false) }
            is PlayBillingEvent.ProductDetailsLoaded -> updateProductDetails(event)
            is PlayBillingEvent.PurchasesObserved -> processPurchases(event)
            is PlayBillingEvent.OperationFailed -> updateFailure(event)
            PlayBillingEvent.SubscriptionNotReady,
            PlayBillingEvent.ManageSubscriptionUnavailable,
            PlayBillingEvent.PurchaseCanceled,
            -> updateMessage(event)
        }
    }

    private fun updateConnecting(event: PlayBillingEvent) {
        _uiState.update {
            it.copy(
                isLoading = true,
                message = requireNotNull(event.toBillingUiMessage()),
            )
        }
    }

    private fun updateConnected(event: PlayBillingEvent) {
        _uiState.update {
            it.copy(
                isReady = true,
                isLoading = false,
                message = requireNotNull(event.toBillingUiMessage()),
            )
        }
    }

    private fun updateDisconnected(event: PlayBillingEvent) {
        _uiState.update {
            it.copy(
                isReady = false,
                isLoading = false,
                canStartPurchase = false,
                message = requireNotNull(event.toBillingUiMessage()),
            )
        }
    }

    private fun updateProductDetails(event: PlayBillingEvent.ProductDetailsLoaded) {
        _uiState.update {
            it.copy(
                isLoading = false,
                productPrice = event.productPrice,
                canStartPurchase = event.canStartPurchase,
                message = requireNotNull(event.toBillingUiMessage()),
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
                message = requireNotNull(event.toBillingUiMessage()),
            )
        }
    }

    private fun updateMessage(event: PlayBillingEvent) {
        _uiState.update { it.copy(message = requireNotNull(event.toBillingUiMessage())) }
    }

    private fun processPurchases(event: PlayBillingEvent.PurchasesObserved) {
        if (syncBillingEntitlementUseCase.willVerifyPurchaseWithBackend(event.summary)) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    message = billingVerificationProgressMessage(),
                )
            }
        }
        billingScope.launch {
            applyBillingSyncOutcome(syncBillingEntitlementUseCase(event.summary))
        }
    }

    private fun applyBillingSyncOutcome(outcome: BillingSyncOutcome) {
        outcome.purchaseTokensToAcknowledge.forEach(billingGateway::acknowledgePurchase)
        onEntitlementChanged(outcome.entitlementSnapshot)
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = if (outcome.clearsLoading) false else currentState.isLoading,
                message = outcome.toBillingUiMessage() ?: currentState.message,
            )
        }
    }
}
