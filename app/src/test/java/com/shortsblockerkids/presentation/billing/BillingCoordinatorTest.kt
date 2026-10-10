package com.shortsblockerkids.presentation.billing

import com.shortsblockerkids.application.billing.BillingPurchaseSummary
import com.shortsblockerkids.application.billing.PlayBillingEvent
import com.shortsblockerkids.application.billing.PlayBillingGateway
import com.shortsblockerkids.application.billing.PlayBillingOperation
import com.shortsblockerkids.application.billing.SyncBillingEntitlementUseCase
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class BillingCoordinatorTest {
    @Test
    fun gatewayLifecycleAndActionsDelegateExactly() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway)

        coordinator.start()
        coordinator.refreshPurchases()
        coordinator.launchPurchase()
        coordinator.openManageSubscription()
        coordinator.stop()

        assertEquals(listOf("start", "refresh", "launch", "manage", "stop"), gateway.actions)
    }

    @Test
    fun connectionProductAndPurchaseFlowEventsPreserveUiStateTransitions() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway)

        gateway.emit(PlayBillingEvent.Connecting)
        assertTrue(coordinator.uiState.value.isLoading)
        assertEquals(BillingMessageCode.CONNECTING, coordinator.uiState.value.message.code)

        gateway.emit(PlayBillingEvent.Connected)
        assertTrue(coordinator.uiState.value.isReady)
        assertFalse(coordinator.uiState.value.isLoading)

        gateway.emit(PlayBillingEvent.ProductDetailsLoading)
        assertTrue(coordinator.uiState.value.isLoading)
        gateway.emit(
            PlayBillingEvent.ProductDetailsLoaded(
                productPrice = "Play price",
                isProductAvailable = true,
                canStartPurchase = true,
            ),
        )
        assertEquals("Play price", coordinator.uiState.value.productPrice)
        assertTrue(coordinator.uiState.value.canStartPurchase)
        assertEquals(BillingMessageCode.PRODUCT_LOADED, coordinator.uiState.value.message.code)

        gateway.emit(PlayBillingEvent.OpeningPurchaseFlow)
        assertTrue(coordinator.uiState.value.isPurchaseInProgress)
        gateway.emit(PlayBillingEvent.PurchaseFlowFinished)
        assertFalse(coordinator.uiState.value.isPurchaseInProgress)

        gateway.emit(PlayBillingEvent.Disconnected)
        assertFalse(coordinator.uiState.value.isReady)
        assertFalse(coordinator.uiState.value.canStartPurchase)
        assertEquals(BillingMessageCode.DISCONNECTED, coordinator.uiState.value.message.code)
    }

    @Test
    fun unavailableProductAndSdkErrorsPreserveFailureState() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway)

        gateway.emit(PlayBillingEvent.ProductDetailsLoaded(null, false, false))
        assertEquals(BillingMessageCode.PRODUCT_UNAVAILABLE, coordinator.uiState.value.message.code)
        assertFalse(coordinator.uiState.value.canStartPurchase)

        gateway.emit(
            PlayBillingEvent.OperationFailed(
                operation = PlayBillingOperation.RESTORE_PURCHASES,
                responseCode = 3,
                isReady = false,
                canStartPurchase = false,
            ),
        )
        assertEquals(BillingMessageCode.RESTORE_PURCHASES_FAILED, coordinator.uiState.value.message.code)
        assertEquals(3, coordinator.uiState.value.message.responseCode)
        assertFalse(coordinator.uiState.value.isReady)
    }

    @Test
    fun purchaseFlowFailurePreservesGatewayReadinessAndResponseCode() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway)
        gateway.emit(PlayBillingEvent.OpeningPurchaseFlow)

        gateway.emit(
            PlayBillingEvent.OperationFailed(
                operation = PlayBillingOperation.OPEN_PURCHASE_FLOW,
                responseCode = 5,
                isReady = true,
                canStartPurchase = true,
            ),
        )

        val state = coordinator.uiState.value
        assertTrue(state.isReady)
        assertFalse(state.isLoading)
        assertFalse(state.isPurchaseInProgress)
        assertTrue(state.canStartPurchase)
        assertEquals(BillingMessageCode.OPEN_PURCHASE_FLOW_FAILED, state.message.code)
        assertEquals(5, state.message.responseCode)
    }

    @Test
    fun productLoadFailureClearsReadinessAndPurchaseAvailability() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway)
        gateway.emit(PlayBillingEvent.Connected)
        gateway.emit(PlayBillingEvent.ProductDetailsLoaded("Play price", true, true))

        gateway.emit(
            PlayBillingEvent.OperationFailed(
                operation = PlayBillingOperation.LOAD_PRODUCT_DETAILS,
                responseCode = 3,
                isReady = false,
                canStartPurchase = false,
            ),
        )

        val state = coordinator.uiState.value
        assertFalse(state.isReady)
        assertFalse(state.isLoading)
        assertFalse(state.isPurchaseInProgress)
        assertFalse(state.canStartPurchase)
        assertEquals(BillingMessageCode.LOAD_SUBSCRIPTION_FAILED, state.message.code)
        assertEquals(3, state.message.responseCode)
    }

    @Test
    fun manageUnavailableAndPurchaseCanceledKeepTheirMessages() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway)

        gateway.emit(PlayBillingEvent.ManageSubscriptionUnavailable)
        assertEquals(BillingMessageCode.MANAGE_SUBSCRIPTION_UNAVAILABLE, coordinator.uiState.value.message.code)
        gateway.emit(PlayBillingEvent.PurchaseCanceled)
        assertEquals(BillingMessageCode.PURCHASE_CANCELED, coordinator.uiState.value.message.code)
    }

    @Test
    fun purchasedPlaySubscriptionIsStoredBeforeAcknowledgment() {
        val gateway = RecordingPlayBillingGateway()
        val saved = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(gateway) {
                assertTrue(gateway.acknowledgedTokens.isEmpty())
                saved += it
            }

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary(
                    purchasedSubscriptionToken = "token",
                    unacknowledgedPurchasedSubscriptionTokens = listOf("token"),
                ),
            ),
        )

        assertEquals(BillingEntitlementState.ACTIVE, saved.single().state)
        assertEquals(listOf("token"), gateway.acknowledgedTokens)
        assertEquals(BillingMessageCode.SUBSCRIPTION_ACTIVE, coordinator.uiState.value.message.code)
    }

    @Test
    fun pendingOnlyAndEmptyRestoreDoNotAcknowledgeOrGrantAccess() {
        val gateway = RecordingPlayBillingGateway()
        val saved = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator = createCoordinator(gateway) { saved += it }

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary(
                    purchasedSubscriptionToken = null,
                    hasPendingSubscription = true,
                ),
            ),
        )
        assertEquals(BillingEntitlementState.PENDING, saved.last().state)
        assertEquals(BillingMessageCode.PURCHASE_PENDING, coordinator.uiState.value.message.code)

        coordinator.refreshPurchases()
        gateway.emit(PlayBillingEvent.PurchasesObserved(BillingPurchaseSummary(null)))
        assertEquals(BillingEntitlementState.INACTIVE, saved.last().state)
        assertEquals(BillingMessageCode.NO_ACTIVE_SUBSCRIPTION, coordinator.uiState.value.message.code)
        assertTrue(gateway.acknowledgedTokens.isEmpty())
    }

    @Test
    fun persistenceMustCompleteBeforeAcknowledgmentAndSuccessMessage() {
        val gateway = RecordingPlayBillingGateway()
        val persistence = CompletableDeferred<Unit>()
        val saved = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(gateway) {
                persistence.await()
                saved += it
            }

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary("token", unacknowledgedPurchasedSubscriptionTokens = listOf("token")),
            ),
        )
        assertTrue(saved.isEmpty())
        assertTrue(gateway.acknowledgedTokens.isEmpty())
        persistence.complete(Unit)
        assertEquals(BillingEntitlementState.ACTIVE, saved.single().state)
        assertEquals(listOf("token"), gateway.acknowledgedTokens)
        assertEquals(BillingMessageCode.SUBSCRIPTION_ACTIVE, coordinator.uiState.value.message.code)
    }

    @Test
    fun failedWriteDoesNotAcknowledgeAndLaterRestoreCanRecover() {
        val gateway = RecordingPlayBillingGateway()
        val saved = mutableListOf<BillingEntitlementSnapshot>()
        var failWrite = true
        val coordinator =
            createCoordinator(gateway) {
                if (failWrite) throw IOException("Injected write failure")
                saved += it
            }
        val purchase =
            BillingPurchaseSummary(
                purchasedSubscriptionToken = "token",
                unacknowledgedPurchasedSubscriptionTokens = listOf("token"),
            )

        gateway.emit(PlayBillingEvent.PurchasesObserved(purchase))
        assertEquals(BillingMessageCode.ENTITLEMENT_SAVE_FAILED, coordinator.uiState.value.message.code)
        assertTrue(saved.isEmpty())
        assertTrue(gateway.acknowledgedTokens.isEmpty())

        failWrite = false
        coordinator.refreshPurchases()
        gateway.emit(PlayBillingEvent.PurchasesObserved(purchase))
        assertEquals(BillingEntitlementState.ACTIVE, saved.single().state)
        assertEquals(listOf("token"), gateway.acknowledgedTokens)
        assertEquals(BillingMessageCode.SUBSCRIPTION_ACTIVE, coordinator.uiState.value.message.code)
    }

    @Test
    fun scopeCancellationStopsPendingWriteWithoutAcknowledgment() {
        val gateway = RecordingPlayBillingGateway()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val persistence = CompletableDeferred<Unit>()
        val saved = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(gateway, scope = scope) {
                persistence.await()
                saved += it
            }

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary("token", unacknowledgedPurchasedSubscriptionTokens = listOf("token")),
            ),
        )
        scope.cancel()
        persistence.complete(Unit)

        assertTrue(saved.isEmpty())
        assertTrue(gateway.acknowledgedTokens.isEmpty())
        assertEquals(BillingMessageCode.CONNECTING, coordinator.uiState.value.message.code)
    }

    private fun createCoordinator(
        gateway: RecordingPlayBillingGateway,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
        onEntitlementChanged: suspend (BillingEntitlementSnapshot) -> Unit = {},
    ): BillingCoordinator =
        BillingCoordinator(
            billingGateway = gateway,
            syncBillingEntitlementUseCase = SyncBillingEntitlementUseCase(),
            onEntitlementChanged = onEntitlementChanged,
            billingScope = scope,
        )

    private class RecordingPlayBillingGateway : PlayBillingGateway {
        private var listener: (PlayBillingEvent) -> Unit = {}
        val actions = mutableListOf<String>()
        val acknowledgedTokens = mutableListOf<String>()

        override fun setEventListener(listener: (PlayBillingEvent) -> Unit) {
            this.listener = listener
        }

        override fun start() {
            actions += "start"
        }

        override fun stop() {
            actions += "stop"
        }

        override fun refreshPurchases() {
            actions += "refresh"
        }

        override fun launchPurchase() {
            actions += "launch"
        }

        override fun openManageSubscription() {
            actions += "manage"
        }

        override fun acknowledgePurchase(purchaseToken: String) {
            acknowledgedTokens += purchaseToken
        }

        fun emit(event: PlayBillingEvent) {
            listener(event)
        }
    }
}
