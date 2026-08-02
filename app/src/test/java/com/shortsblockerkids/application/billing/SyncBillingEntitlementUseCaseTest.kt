package com.shortsblockerkids.application.billing

import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncBillingEntitlementUseCaseTest {
    @Test
    fun configuredBackendTakesPrecedenceAndVerifiesPurchasedSubscriptionExactly() =
        runBlocking {
            val exactSnapshot =
                BillingEntitlementSnapshot(
                    state = BillingEntitlementState.CANCELED_ACTIVE,
                    checkedAtMillis = 4_321L,
                    activeUntilMillis = 9_876L,
                )
            val backend =
                RecordingBillingVerificationPort(
                    isConfigured = true,
                    verifyResult = exactSnapshot,
                )
            val timeProvider = RecordingTimeProvider()
            val useCase =
                createUseCase(
                    backend = backend,
                    timeProvider = timeProvider,
                    clientOnlyModeRequested = true,
                    internalTestingBuild = true,
                )
            val purchaseSummary = purchasedSummary(hasPendingSubscription = true)

            assertTrue(useCase.willVerifyPurchaseWithBackend(purchaseSummary))
            val outcome = useCase(purchaseSummary)

            assertSame(exactSnapshot, outcome.entitlementSnapshot)
            assertEquals(BillingSyncStatus.ENTITLEMENT_RESOLVED, outcome.status)
            assertTrue(outcome.hasPendingSubscription)
            assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
            assertEquals(
                listOf(
                    BillingVerificationRequest(
                        installId = INSTALL_ID,
                        packageName = PACKAGE_NAME,
                        productId = PRODUCT_ID,
                        purchaseToken = PURCHASE_TOKEN,
                        appVersion = APP_VERSION,
                    ),
                ),
                backend.verifyRequests,
            )
            assertTrue(backend.refreshInstallIds.isEmpty())
            assertEquals(0, timeProvider.callCount)
        }

    @Test
    fun backendVerificationProgressRequiresConfiguredBackendPurchaseAndInstallId() {
        val purchased = purchasedSummary()
        val noPurchase = noPurchaseSummary()

        assertFalse(
            createUseCase(RecordingBillingVerificationPort(isConfigured = false))
                .willVerifyPurchaseWithBackend(purchased),
        )
        assertFalse(
            createUseCase(
                backend = RecordingBillingVerificationPort(isConfigured = true),
                installId = null,
            ).willVerifyPurchaseWithBackend(purchased),
        )
        assertFalse(
            createUseCase(
                backend = RecordingBillingVerificationPort(isConfigured = true),
                installId = "   ",
            ).willVerifyPurchaseWithBackend(purchased),
        )
        assertFalse(
            createUseCase(RecordingBillingVerificationPort(isConfigured = true))
                .willVerifyPurchaseWithBackend(noPurchase),
        )
        assertTrue(
            createUseCase(RecordingBillingVerificationPort(isConfigured = true))
                .willVerifyPurchaseWithBackend(purchased),
        )
    }

    @Test
    fun missingOrBlankInstallIdFailsVerificationClosedWithoutBackendCall() =
        runBlocking {
            listOf<String?>(null, "", "   ").forEach { installId ->
                val backend = RecordingBillingVerificationPort(isConfigured = true)
                val timeProvider = RecordingTimeProvider()
                val outcome =
                    createUseCase(
                        backend = backend,
                        timeProvider = timeProvider,
                        installId = installId,
                    )(purchasedSummary())

                assertFailClosed(
                    outcome = outcome,
                    expectedStatus = BillingSyncStatus.BACKEND_INSTALLATION_ID_UNAVAILABLE,
                    expectedClearsLoading = false,
                )
                assertTrue(backend.verifyRequests.isEmpty())
                assertTrue(backend.refreshInstallIds.isEmpty())
                assertEquals(1, timeProvider.callCount)
            }
        }

    @Test
    fun verificationFailureReturnsUnknownImmediatelyWithoutAcknowledgement() =
        runBlocking {
            val backend =
                RecordingBillingVerificationPort(isConfigured = true).apply {
                    verifyFailure = IllegalStateException("verify failed")
                }
            val timeProvider = RecordingTimeProvider()

            val outcome =
                createUseCase(backend = backend, timeProvider = timeProvider)(
                    purchasedSummary(),
                )

            assertFailClosed(outcome, BillingSyncStatus.BACKEND_PURCHASE_VERIFICATION_FAILED)
            assertEquals(1, backend.verifyRequests.size)
            assertTrue(backend.refreshInstallIds.isEmpty())
            assertEquals(1, timeProvider.callCount)
        }

    @Test
    fun configuredBackendRefreshStoresExactSnapshotWithoutReconstruction() =
        runBlocking {
            val exactSnapshot =
                BillingEntitlementSnapshot(
                    state = BillingEntitlementState.ACTIVE,
                    checkedAtMillis = 2_222L,
                    activeUntilMillis = 8_888L,
                )
            val backend =
                RecordingBillingVerificationPort(
                    isConfigured = true,
                    refreshResult = exactSnapshot,
                )
            val timeProvider = RecordingTimeProvider()

            val outcome =
                createUseCase(
                    backend = backend,
                    timeProvider = timeProvider,
                    clientOnlyModeRequested = true,
                    internalTestingBuild = true,
                )(noPurchaseSummary())

            assertSame(exactSnapshot, outcome.entitlementSnapshot)
            assertEquals(BillingSyncStatus.ENTITLEMENT_RESOLVED, outcome.status)
            assertFalse(outcome.hasPendingSubscription)
            assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
            assertEquals(listOf(INSTALL_ID), backend.refreshInstallIds)
            assertTrue(backend.verifyRequests.isEmpty())
            assertEquals(0, timeProvider.callCount)
        }

    @Test
    fun nullRefreshCreatesExpiredSnapshotWithCurrentTimeAndPendingState() =
        runBlocking {
            listOf(false, true).forEach { hasPendingSubscription ->
                val backend =
                    RecordingBillingVerificationPort(
                        isConfigured = true,
                        refreshResult = null,
                    )
                val timeProvider = RecordingTimeProvider()

                val outcome =
                    createUseCase(backend = backend, timeProvider = timeProvider)(
                        noPurchaseSummary(hasPendingSubscription),
                    )

                assertEquals(BillingEntitlementState.EXPIRED, outcome.entitlementSnapshot.state)
                assertEquals(NOW_MILLIS, outcome.entitlementSnapshot.checkedAtMillis)
                assertNull(outcome.entitlementSnapshot.activeUntilMillis)
                assertEquals(BillingSyncStatus.ENTITLEMENT_RESOLVED, outcome.status)
                assertEquals(hasPendingSubscription, outcome.hasPendingSubscription)
                assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
                assertEquals(listOf(INSTALL_ID), backend.refreshInstallIds)
                assertEquals(1, timeProvider.callCount)
            }
        }

    @Test
    fun refreshFailureReturnsUnknownImmediatelyWithoutAcknowledgement() =
        runBlocking {
            val backend =
                RecordingBillingVerificationPort(isConfigured = true).apply {
                    refreshFailure = IllegalStateException("refresh failed")
                }
            val timeProvider = RecordingTimeProvider()

            val outcome =
                createUseCase(backend = backend, timeProvider = timeProvider)(noPurchaseSummary())

            assertFailClosed(outcome, BillingSyncStatus.BACKEND_ENTITLEMENT_REFRESH_FAILED)
            assertEquals(listOf(INSTALL_ID), backend.refreshInstallIds)
            assertTrue(backend.verifyRequests.isEmpty())
            assertEquals(1, timeProvider.callCount)
        }

    @Test
    fun missingOrBlankInstallIdFailsRefreshClosedAndPreservesCurrentMessage() =
        runBlocking {
            listOf<String?>(null, "", "   ").forEach { installId ->
                val backend = RecordingBillingVerificationPort(isConfigured = true)
                val timeProvider = RecordingTimeProvider()
                val outcome =
                    createUseCase(
                        backend = backend,
                        timeProvider = timeProvider,
                        installId = installId,
                    )(noPurchaseSummary())

                assertFailClosed(
                    outcome = outcome,
                    expectedStatus = null,
                    expectedClearsLoading = false,
                )
                assertTrue(backend.verifyRequests.isEmpty())
                assertTrue(backend.refreshInstallIds.isEmpty())
                assertEquals(1, timeProvider.callCount)
            }
        }

    @Test
    fun clientOnlyPurchasedSubscriptionUsesCompleteBooleanMatrix() =
        runBlocking {
            val cases =
                listOf(
                    ClientOnlyCase(false, false, BillingEntitlementState.EXPIRED, emptyList()),
                    ClientOnlyCase(false, true, BillingEntitlementState.EXPIRED, emptyList()),
                    ClientOnlyCase(true, false, BillingEntitlementState.EXPIRED, emptyList()),
                    ClientOnlyCase(true, true, BillingEntitlementState.ACTIVE, ACK_TOKENS),
                )

            cases.forEach { case ->
                val backend = RecordingBillingVerificationPort(isConfigured = false)
                val timeProvider = RecordingTimeProvider()
                val outcome =
                    createUseCase(
                        backend = backend,
                        timeProvider = timeProvider,
                        clientOnlyModeRequested = case.clientOnlyModeRequested,
                        internalTestingBuild = case.internalTestingBuild,
                    )(purchasedSummary())

                assertEquals(case.expectedState, outcome.entitlementSnapshot.state)
                assertEquals(NOW_MILLIS, outcome.entitlementSnapshot.checkedAtMillis)
                assertEquals(
                    if (case.expectedState == BillingEntitlementState.ACTIVE) {
                        BillingSyncStatus.ENTITLEMENT_RESOLVED
                    } else {
                        BillingSyncStatus.BACKEND_VERIFICATION_REQUIRED
                    },
                    outcome.status,
                )
                assertFalse(outcome.hasPendingSubscription)
                assertEquals(case.expectedAcknowledgementTokens, outcome.purchaseTokensToAcknowledge)
                assertTrue(backend.verifyRequests.isEmpty())
                assertTrue(backend.refreshInstallIds.isEmpty())
                assertEquals(1, timeProvider.callCount)
            }
        }

    @Test
    fun permittedClientOnlyPathDoesNotAcknowledgeAlreadyAcknowledgedPurchase() =
        runBlocking {
            val outcome =
                createUseCase(
                    backend = RecordingBillingVerificationPort(isConfigured = false),
                    clientOnlyModeRequested = true,
                    internalTestingBuild = true,
                )(
                    purchasedSummary(
                        hasPendingSubscription = true,
                        unacknowledgedTokens = emptyList(),
                    ),
                )

            assertEquals(BillingEntitlementState.ACTIVE, outcome.entitlementSnapshot.state)
            assertEquals(BillingSyncStatus.ENTITLEMENT_RESOLVED, outcome.status)
            assertTrue(outcome.hasPendingSubscription)
            assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
        }

    @Test
    fun pendingOnlyAndNoPurchaseNeverGrantClientOnlyProtectionOrAcknowledgement() =
        runBlocking {
            listOf(
                noPurchaseSummary(
                    hasPendingSubscription = true,
                    unacknowledgedTokens = ACK_TOKENS,
                ),
                noPurchaseSummary(
                    hasPendingSubscription = false,
                    unacknowledgedTokens = ACK_TOKENS,
                ),
            ).forEach { purchaseSummary ->
                val outcome =
                    createUseCase(
                        backend = RecordingBillingVerificationPort(isConfigured = false),
                        clientOnlyModeRequested = true,
                        internalTestingBuild = true,
                    )(purchaseSummary)

                assertEquals(BillingEntitlementState.EXPIRED, outcome.entitlementSnapshot.state)
                assertFalse(outcome.entitlementSnapshot.isActive)
                assertEquals(BillingSyncStatus.ENTITLEMENT_RESOLVED, outcome.status)
                assertEquals(
                    purchaseSummary.hasPendingSubscription,
                    outcome.hasPendingSubscription,
                )
                assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
            }
        }

    @Test
    fun blankPurchaseTokenRemainsAPurchasedSubscriptionAndIsVerifiedExactly() =
        runBlocking {
            val backend = RecordingBillingVerificationPort(isConfigured = true)
            val purchaseSummary = purchasedSummary(purchaseToken = "")
            val useCase = createUseCase(backend)

            assertTrue(purchaseSummary.hasPurchasedSubscription)
            assertTrue(useCase.willVerifyPurchaseWithBackend(purchaseSummary))

            useCase(purchaseSummary)

            assertEquals("", backend.verifyRequests.single().purchaseToken)
            assertTrue(backend.refreshInstallIds.isEmpty())
        }

    private fun createUseCase(
        backend: RecordingBillingVerificationPort,
        timeProvider: RecordingTimeProvider = RecordingTimeProvider(),
        installId: String? = INSTALL_ID,
        clientOnlyModeRequested: Boolean = false,
        internalTestingBuild: Boolean = false,
    ): SyncBillingEntitlementUseCase =
        SyncBillingEntitlementUseCase(
            billingVerificationPort = backend,
            timeProvider = timeProvider,
            configuration =
                BillingSyncConfiguration(
                    installId = installId,
                    packageName = PACKAGE_NAME,
                    productId = PRODUCT_ID,
                    appVersion = APP_VERSION,
                    clientOnlyModeRequested = clientOnlyModeRequested,
                    internalTestingBuild = internalTestingBuild,
                ),
        )

    private fun purchasedSummary(
        purchaseToken: String = PURCHASE_TOKEN,
        hasPendingSubscription: Boolean = false,
        unacknowledgedTokens: List<String> = ACK_TOKENS,
    ): BillingPurchaseSummary =
        BillingPurchaseSummary(
            purchasedSubscriptionToken = purchaseToken,
            hasPendingSubscription = hasPendingSubscription,
            unacknowledgedPurchasedSubscriptionTokens = unacknowledgedTokens,
        )

    private fun noPurchaseSummary(
        hasPendingSubscription: Boolean = false,
        unacknowledgedTokens: List<String> = emptyList(),
    ): BillingPurchaseSummary =
        BillingPurchaseSummary(
            purchasedSubscriptionToken = null,
            hasPendingSubscription = hasPendingSubscription,
            unacknowledgedPurchasedSubscriptionTokens = unacknowledgedTokens,
        )

    private fun assertFailClosed(
        outcome: BillingSyncOutcome,
        expectedStatus: BillingSyncStatus?,
        expectedClearsLoading: Boolean = true,
    ) {
        assertEquals(BillingEntitlementState.UNKNOWN, outcome.entitlementSnapshot.state)
        assertEquals(NOW_MILLIS, outcome.entitlementSnapshot.checkedAtMillis)
        assertNull(outcome.entitlementSnapshot.activeUntilMillis)
        assertFalse(outcome.entitlementSnapshot.isActive)
        assertEquals(expectedStatus, outcome.status)
        assertEquals(expectedClearsLoading, outcome.clearsLoading)
        assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
    }

    private data class ClientOnlyCase(
        val clientOnlyModeRequested: Boolean,
        val internalTestingBuild: Boolean,
        val expectedState: BillingEntitlementState,
        val expectedAcknowledgementTokens: List<String>,
    )

    private class RecordingTimeProvider(
        private val nowMillis: Long = NOW_MILLIS,
    ) : TimeProvider {
        var callCount: Int = 0
            private set

        override fun currentTimeMillis(): Long {
            callCount += 1
            return nowMillis
        }
    }

    private class RecordingBillingVerificationPort(
        override val isConfigured: Boolean,
        var verifyResult: BillingEntitlementSnapshot = ACTIVE_SNAPSHOT,
        var refreshResult: BillingEntitlementSnapshot? = ACTIVE_SNAPSHOT,
    ) : BillingVerificationPort {
        val verifyRequests = mutableListOf<BillingVerificationRequest>()
        val refreshInstallIds = mutableListOf<String>()
        var verifyFailure: RuntimeException? = null
        var refreshFailure: RuntimeException? = null

        override suspend fun verifyPurchase(request: BillingVerificationRequest): BillingEntitlementSnapshot {
            verifyRequests += request
            verifyFailure?.let { failure -> throw failure }
            return verifyResult
        }

        override suspend fun refreshEntitlement(installId: String): BillingEntitlementSnapshot? {
            refreshInstallIds += installId
            refreshFailure?.let { failure -> throw failure }
            return refreshResult
        }
    }

    private companion object {
        const val NOW_MILLIS = 1_234L
        const val INSTALL_ID = "installation-id"
        const val PACKAGE_NAME = "com.shortsblockerkids"
        const val PRODUCT_ID = "shorts_blocker_kids_monthly"
        const val PURCHASE_TOKEN = "purchase-token"
        const val APP_VERSION = "1.0"

        val ACK_TOKENS = listOf("purchase-token", "second-purchase-token")
        val ACTIVE_SNAPSHOT =
            BillingEntitlementSnapshot(
                state = BillingEntitlementState.ACTIVE,
                checkedAtMillis = 100L,
            )
    }
}
