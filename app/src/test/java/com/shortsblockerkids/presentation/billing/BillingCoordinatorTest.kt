package com.shortsblockerkids.presentation.billing

import com.shortsblockerkids.application.billing.BillingPurchaseSummary
import com.shortsblockerkids.application.billing.BillingSyncConfiguration
import com.shortsblockerkids.application.billing.BillingVerificationPort
import com.shortsblockerkids.application.billing.BillingVerificationRequest
import com.shortsblockerkids.application.billing.PlayBillingEvent
import com.shortsblockerkids.application.billing.PlayBillingGateway
import com.shortsblockerkids.application.billing.PlayBillingOperation
import com.shortsblockerkids.application.billing.SyncBillingEntitlementUseCase
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingCoordinatorTest {
    @Test
    fun gatewayLifecycleAndActionsDelegateExactly() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway = gateway)

        coordinator.start()
        coordinator.refreshPurchases()
        coordinator.launchPurchase()
        coordinator.openManageSubscription()
        coordinator.stop()

        assertEquals(
            listOf("start", "refresh", "launch", "manage", "stop"),
            gateway.actions,
        )
    }

    @Test
    fun connectionProductAndPurchaseFlowEventsPreserveUiStateTransitions() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway = gateway)

        gateway.emit(PlayBillingEvent.Connecting)
        assertTrue(coordinator.uiState.value.isLoading)
        assertEquals(BillingMessageCode.CONNECTING, coordinator.uiState.value.message.code)

        gateway.emit(PlayBillingEvent.Connected)
        assertTrue(coordinator.uiState.value.isReady)
        assertFalse(coordinator.uiState.value.isLoading)
        assertEquals(BillingMessageCode.CONNECTED, coordinator.uiState.value.message.code)

        gateway.emit(PlayBillingEvent.ProductDetailsLoading)
        assertTrue(coordinator.uiState.value.isLoading)

        gateway.emit(
            PlayBillingEvent.ProductDetailsLoaded(
                productPrice = "Play price",
                isProductAvailable = true,
                canStartPurchase = true,
            ),
        )
        assertFalse(coordinator.uiState.value.isLoading)
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
    fun messageOnlyGatewayEventsChangeNoOtherUiState() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway = gateway)
        val events =
            listOf(
                PlayBillingEvent.SubscriptionNotReady to
                    BillingMessageCode.SUBSCRIPTION_NOT_READY,
                PlayBillingEvent.ManageSubscriptionUnavailable to
                    BillingMessageCode.MANAGE_SUBSCRIPTION_UNAVAILABLE,
                PlayBillingEvent.PurchaseCanceled to BillingMessageCode.PURCHASE_CANCELED,
            )

        events.forEach { (event, expectedCode) ->
            gateway.emit(event)
            assertEquals(expectedCode, coordinator.uiState.value.message.code)
            assertFalse(coordinator.uiState.value.isReady)
            assertFalse(coordinator.uiState.value.isLoading)
        }
    }

    @Test
    fun failureEventAppliesGatewayReadinessAndResponseCode() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway = gateway)
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
    fun backendVerificationShowsProgressThenStoresExactSnapshot() {
        val gateway = RecordingPlayBillingGateway()
        val exactSnapshot =
            BillingEntitlementSnapshot(
                state = BillingEntitlementState.CANCELED_ACTIVE,
                checkedAtMillis = 77L,
                activeUntilMillis = 88L,
            )
        val verificationResult = CompletableDeferred<BillingEntitlementSnapshot>()
        val port = RecordingVerificationPort(isConfigured = true, verificationResult)
        val storedSnapshots = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(
                gateway = gateway,
                port = port,
                onEntitlementChanged = storedSnapshots::add,
            )

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary(purchasedSubscriptionToken = "token"),
            ),
        )
        assertTrue(coordinator.uiState.value.isLoading)
        assertEquals(
            BillingMessageCode.VERIFYING_WITH_BACKEND,
            coordinator.uiState.value.message.code,
        )

        verificationResult.complete(exactSnapshot)

        assertSame(exactSnapshot, storedSnapshots.single())
        assertFalse(coordinator.uiState.value.isLoading)
        assertEquals(
            BillingMessageCode.SUBSCRIPTION_CANCELED_ACTIVE,
            coordinator.uiState.value.message.code,
        )
        assertTrue(gateway.acknowledgedTokens.isEmpty())
    }

    @Test
    fun permittedClientOnlyPurchaseAcknowledgesAndStoresActiveEntitlement() {
        val gateway = RecordingPlayBillingGateway()
        val storedSnapshots = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(
                gateway = gateway,
                clientOnlyModeRequested = true,
                internalTestingBuild = true,
                onEntitlementChanged = storedSnapshots::add,
            )

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary(
                    purchasedSubscriptionToken = "token",
                    unacknowledgedPurchasedSubscriptionTokens = listOf("token"),
                ),
            ),
        )

        assertEquals(listOf("token"), gateway.acknowledgedTokens)
        assertEquals(BillingEntitlementState.ACTIVE, storedSnapshots.single().state)
        assertEquals(BillingMessageCode.SUBSCRIPTION_ACTIVE, coordinator.uiState.value.message.code)
    }

    @Test
    fun missingInstallIdRefreshFailsClosedWithoutReplacingCurrentMessageOrLoading() {
        val gateway = RecordingPlayBillingGateway()
        val storedSnapshots = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(
                gateway = gateway,
                port = RecordingVerificationPort(isConfigured = true),
                installId = null,
                onEntitlementChanged = storedSnapshots::add,
            )
        gateway.emit(PlayBillingEvent.ProductDetailsLoading)

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary(purchasedSubscriptionToken = null),
            ),
        )

        assertEquals(BillingEntitlementState.UNKNOWN, storedSnapshots.single().state)
        assertTrue(coordinator.uiState.value.isLoading)
        assertEquals(BillingMessageCode.CONNECTING, coordinator.uiState.value.message.code)
    }

    private fun createCoordinator(
        gateway: RecordingPlayBillingGateway,
        port: BillingVerificationPort = RecordingVerificationPort(isConfigured = false),
        installId: String? = INSTALL_ID,
        clientOnlyModeRequested: Boolean = false,
        internalTestingBuild: Boolean = false,
        onEntitlementChanged: (BillingEntitlementSnapshot) -> Unit = {},
    ): BillingCoordinator =
        BillingCoordinator(
            billingGateway = gateway,
            syncBillingEntitlementUseCase =
                SyncBillingEntitlementUseCase(
                    billingVerificationPort = port,
                    timeProvider = TimeProvider { NOW_MILLIS },
                    configuration =
                        BillingSyncConfiguration(
                            installId = installId,
                            packageName = "com.shortsblockerkids",
                            productId = "shorts_blocker_kids_monthly",
                            appVersion = "1.0",
                            clientOnlyModeRequested = clientOnlyModeRequested,
                            internalTestingBuild = internalTestingBuild,
                        ),
                ),
            onEntitlementChanged = onEntitlementChanged,
            billingScope = CoroutineScope(Dispatchers.Unconfined),
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

    private class RecordingVerificationPort(
        override val isConfigured: Boolean,
        private val verificationResult: CompletableDeferred<BillingEntitlementSnapshot>? = null,
    ) : BillingVerificationPort {
        override suspend fun verifyPurchase(request: BillingVerificationRequest): BillingEntitlementSnapshot =
            verificationResult?.await() ?: ACTIVE_SNAPSHOT

        override suspend fun refreshEntitlement(installId: String): BillingEntitlementSnapshot? = ACTIVE_SNAPSHOT
    }

    private companion object {
        const val NOW_MILLIS = 1_000L
        const val INSTALL_ID = "installation-id"
        val ACTIVE_SNAPSHOT =
            BillingEntitlementSnapshot(
                state = BillingEntitlementState.ACTIVE,
                checkedAtMillis = 500L,
            )
    }
}
