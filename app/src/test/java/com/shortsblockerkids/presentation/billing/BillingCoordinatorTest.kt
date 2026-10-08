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
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    fun unavailableProductDetailsDisablePurchaseStart() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway = gateway)

        gateway.emit(
            PlayBillingEvent.ProductDetailsLoaded(
                productPrice = null,
                isProductAvailable = false,
                canStartPurchase = false,
            ),
        )

        val state = coordinator.uiState.value
        assertFalse(state.isLoading)
        assertEquals(null, state.productPrice)
        assertFalse(state.canStartPurchase)
        assertEquals(BillingMessageCode.PRODUCT_UNAVAILABLE, state.message.code)
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
    fun failureEventCanClearGatewayReadinessAndPurchaseAvailability() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator = createCoordinator(gateway = gateway)
        gateway.emit(PlayBillingEvent.Connected)
        gateway.emit(
            PlayBillingEvent.ProductDetailsLoaded(
                productPrice = "Play price",
                isProductAvailable = true,
                canStartPurchase = true,
            ),
        )

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

    @Test
    fun backendPurchaseVerificationFailureClearsLoadingAndShowsFailureMessage() {
        val gateway = RecordingPlayBillingGateway()
        val storedSnapshots = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(
                gateway = gateway,
                port = RecordingVerificationPort(
                    isConfigured = true,
                    verifyFailure = IllegalStateException("backend rejected purchase"),
                ),
                onEntitlementChanged = storedSnapshots::add,
            )

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary(
                    purchasedSubscriptionToken = "token",
                    hasPendingSubscription = true,
                ),
            ),
        )

        val state = coordinator.uiState.value
        assertFalse(state.isLoading)
        assertEquals(BillingMessageCode.VERIFY_WITH_BACKEND_FAILED, state.message.code)
        assertEquals(BillingEntitlementState.UNKNOWN, storedSnapshots.single().state)
    }

    @Test
    fun backendRefreshFailureClearsLoadingAndShowsFailureMessage() {
        val gateway = RecordingPlayBillingGateway()
        val storedSnapshots = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(
                gateway = gateway,
                port = RecordingVerificationPort(
                    isConfigured = true,
                    refreshFailure = IllegalStateException("backend unavailable"),
                ),
                onEntitlementChanged = storedSnapshots::add,
            )

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary(purchasedSubscriptionToken = null),
            ),
        )

        val state = coordinator.uiState.value
        assertFalse(state.isLoading)
        assertEquals(BillingMessageCode.REFRESH_BACKEND_FAILED, state.message.code)
        assertEquals(BillingEntitlementState.UNKNOWN, storedSnapshots.single().state)
    }

    @Test
    fun purchaseWithoutBackendAndWithoutClientOnlyModeShowsBackendRequired() {
        val gateway = RecordingPlayBillingGateway()
        val storedSnapshots = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(
                gateway = gateway,
                onEntitlementChanged = storedSnapshots::add,
            )

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary(purchasedSubscriptionToken = "token"),
            ),
        )

        val state = coordinator.uiState.value
        assertFalse(state.isLoading)
        assertEquals(BillingMessageCode.BACKEND_VERIFICATION_REQUIRED, state.message.code)
        assertEquals(BillingEntitlementState.EXPIRED, storedSnapshots.single().state)
        assertTrue(gateway.acknowledgedTokens.isEmpty())
    }

    @Test
    fun entitlementPersistenceIsAwaitedBeforePublishingSuccess() {
        val gateway = RecordingPlayBillingGateway()
        val persistence = CompletableDeferred<Unit>()
        val storedSnapshots = mutableListOf<BillingEntitlementSnapshot>()
        val coordinator =
            createCoordinator(
                gateway = gateway,
                port = RecordingVerificationPort(isConfigured = true),
                onEntitlementChanged = {
                    persistence.await()
                    storedSnapshots += it
                },
            )

        gateway.emit(PlayBillingEvent.PurchasesObserved(BillingPurchaseSummary(purchasedSubscriptionToken = "token")))

        assertTrue(coordinator.uiState.value.isLoading)
        assertEquals(BillingMessageCode.VERIFYING_WITH_BACKEND, coordinator.uiState.value.message.code)
        assertTrue(storedSnapshots.isEmpty())
        persistence.complete(Unit)
        assertSame(ACTIVE_SNAPSHOT, storedSnapshots.single())
        assertFalse(coordinator.uiState.value.isLoading)
        assertEquals(BillingMessageCode.SUBSCRIPTION_ACTIVE, coordinator.uiState.value.message.code)
    }

    @Test
    fun entitlementWriteFailureShowsFailureAndLaterRefreshCanRecover() {
        val gateway = RecordingPlayBillingGateway()
        val storedSnapshots = mutableListOf<BillingEntitlementSnapshot>()
        var failWrite = true
        val coordinator =
            createCoordinator(
                gateway = gateway,
                port = RecordingVerificationPort(isConfigured = true),
                onEntitlementChanged = {
                    if (failWrite) throw IOException("Injected entitlement write failure")
                    storedSnapshots += it
                },
            )

        gateway.emit(PlayBillingEvent.PurchasesObserved(BillingPurchaseSummary(purchasedSubscriptionToken = "token")))

        assertFalse(coordinator.uiState.value.isLoading)
        assertFalse(coordinator.uiState.value.isPurchaseInProgress)
        assertEquals(BillingMessageCode.ENTITLEMENT_SAVE_FAILED, coordinator.uiState.value.message.code)
        assertTrue(storedSnapshots.isEmpty())
        failWrite = false
        coordinator.refreshPurchases()
        gateway.emit(PlayBillingEvent.PurchasesObserved(BillingPurchaseSummary(purchasedSubscriptionToken = null)))
        assertSame(ACTIVE_SNAPSHOT, storedSnapshots.single())
        assertEquals(BillingMessageCode.SUBSCRIPTION_ACTIVE, coordinator.uiState.value.message.code)
    }

    @Test
    fun failedClientOnlyWriteDoesNotAcknowledgePurchaseOrPublishSuccess() {
        val gateway = RecordingPlayBillingGateway()
        val coordinator =
            createCoordinator(
                gateway = gateway,
                clientOnlyModeRequested = true,
                internalTestingBuild = true,
                onEntitlementChanged = { throw IOException("Injected entitlement write failure") },
            )

        gateway.emit(
            PlayBillingEvent.PurchasesObserved(
                BillingPurchaseSummary(
                    purchasedSubscriptionToken = "token",
                    unacknowledgedPurchasedSubscriptionTokens = listOf("token"),
                ),
            ),
        )

        assertTrue(gateway.acknowledgedTokens.isEmpty())
        assertFalse(coordinator.uiState.value.isLoading)
        assertEquals(BillingMessageCode.ENTITLEMENT_SAVE_FAILED, coordinator.uiState.value.message.code)
    }

    @Test
    fun activityScopeCancellationCancelsPendingWriteWithoutPublishingSuccessOrFailure() {
        val gateway = RecordingPlayBillingGateway()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val persistence = CompletableDeferred<Unit>()
        val storedSnapshots = mutableListOf<BillingEntitlementSnapshot>()
        var writeFinished = false
        val coordinator =
            createCoordinator(
                gateway = gateway,
                port = RecordingVerificationPort(isConfigured = true),
                billingScope = scope,
                onEntitlementChanged = {
                    try {
                        persistence.await()
                        storedSnapshots += it
                    } finally {
                        writeFinished = true
                    }
                },
            )

        gateway.emit(PlayBillingEvent.PurchasesObserved(BillingPurchaseSummary(purchasedSubscriptionToken = "token")))
        scope.cancel()
        persistence.complete(Unit)

        assertTrue(writeFinished)
        assertTrue(storedSnapshots.isEmpty())
        assertEquals(BillingMessageCode.VERIFYING_WITH_BACKEND, coordinator.uiState.value.message.code)
    }

    private fun createCoordinator(
        gateway: RecordingPlayBillingGateway,
        port: BillingVerificationPort = RecordingVerificationPort(isConfigured = false),
        installId: String? = INSTALL_ID,
        clientOnlyModeRequested: Boolean = false,
        internalTestingBuild: Boolean = false,
        onEntitlementChanged: suspend (BillingEntitlementSnapshot) -> Unit = {},
        billingScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
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
            billingScope = billingScope,
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
        private val verifyFailure: RuntimeException? = null,
        private val refreshFailure: RuntimeException? = null,
    ) : BillingVerificationPort {
        override suspend fun verifyPurchase(request: BillingVerificationRequest): BillingEntitlementSnapshot {
            verifyFailure?.let { throw it }
            return verificationResult?.await() ?: ACTIVE_SNAPSHOT
        }

        override suspend fun refreshEntitlement(installId: String): BillingEntitlementSnapshot? {
            refreshFailure?.let { throw it }
            return ACTIVE_SNAPSHOT
        }
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
