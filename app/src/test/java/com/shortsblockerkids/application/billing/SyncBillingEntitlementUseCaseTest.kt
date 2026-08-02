package com.shortsblockerkids.application.billing

import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.core.billing.BillingBackendClient
import com.shortsblockerkids.core.billing.BillingBackendPurchaseRequest
import com.shortsblockerkids.core.billing.BillingEntitlementSnapshot
import com.shortsblockerkids.core.billing.BillingEntitlementState
import com.shortsblockerkids.core.billing.BillingMessageCode
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
                RecordingBillingBackendClient(
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

            assertEquals(
                BillingMessageCode.VERIFYING_WITH_BACKEND,
                useCase.messageCodeWhileSyncing(purchaseSummary),
            )
            val outcome = useCase(purchaseSummary)

            assertSame(exactSnapshot, outcome.entitlementSnapshot)
            assertEquals(BillingMessageCode.SUBSCRIPTION_CANCELED_ACTIVE, outcome.messageCodeToApply)
            assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
            assertEquals(
                listOf(
                    BillingBackendPurchaseRequest(
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
    fun verificationProgressMessageRequiresConfiguredBackendPurchaseAndInstallId() {
        val purchased = purchasedSummary()
        val noPurchase = noPurchaseSummary()

        assertNull(
            createUseCase(RecordingBillingBackendClient(isConfigured = false))
                .messageCodeWhileSyncing(purchased),
        )
        assertNull(
            createUseCase(
                backend = RecordingBillingBackendClient(isConfigured = true),
                installId = null,
            ).messageCodeWhileSyncing(purchased),
        )
        assertNull(
            createUseCase(
                backend = RecordingBillingBackendClient(isConfigured = true),
                installId = "   ",
            ).messageCodeWhileSyncing(purchased),
        )
        assertNull(
            createUseCase(RecordingBillingBackendClient(isConfigured = true))
                .messageCodeWhileSyncing(noPurchase),
        )
        assertEquals(
            BillingMessageCode.VERIFYING_WITH_BACKEND,
            createUseCase(RecordingBillingBackendClient(isConfigured = true))
                .messageCodeWhileSyncing(purchased),
        )
    }

    @Test
    fun missingOrBlankInstallIdFailsVerificationClosedWithoutBackendCall() =
        runBlocking {
            listOf<String?>(null, "", "   ").forEach { installId ->
                val backend = RecordingBillingBackendClient(isConfigured = true)
                val timeProvider = RecordingTimeProvider()
                val outcome =
                    createUseCase(
                        backend = backend,
                        timeProvider = timeProvider,
                        installId = installId,
                    )(purchasedSummary())

                assertFailClosed(
                    outcome = outcome,
                    expectedMessageCode = BillingMessageCode.BACKEND_VERIFICATION_NOT_READY,
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
                RecordingBillingBackendClient(isConfigured = true).apply {
                    verifyFailure = IllegalStateException("verify failed")
                }
            val timeProvider = RecordingTimeProvider()

            val outcome =
                createUseCase(backend = backend, timeProvider = timeProvider)(
                    purchasedSummary(),
                )

            assertFailClosed(outcome, BillingMessageCode.VERIFY_WITH_BACKEND_FAILED)
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
                RecordingBillingBackendClient(
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
            assertEquals(BillingMessageCode.SUBSCRIPTION_ACTIVE, outcome.messageCodeToApply)
            assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
            assertEquals(listOf(INSTALL_ID), backend.refreshInstallIds)
            assertTrue(backend.verifyRequests.isEmpty())
            assertEquals(0, timeProvider.callCount)
        }

    @Test
    fun nullRefreshCreatesExpiredSnapshotWithCurrentTimeAndPendingMessage() =
        runBlocking {
            listOf(
                false to BillingMessageCode.NO_ACTIVE_SUBSCRIPTION,
                true to BillingMessageCode.PURCHASE_PENDING,
            ).forEach { (hasPendingSubscription, expectedMessageCode) ->
                val backend =
                    RecordingBillingBackendClient(
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
                assertEquals(expectedMessageCode, outcome.messageCodeToApply)
                assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
                assertEquals(listOf(INSTALL_ID), backend.refreshInstallIds)
                assertEquals(1, timeProvider.callCount)
            }
        }

    @Test
    fun refreshFailureReturnsUnknownImmediatelyWithoutAcknowledgement() =
        runBlocking {
            val backend =
                RecordingBillingBackendClient(isConfigured = true).apply {
                    refreshFailure = IllegalStateException("refresh failed")
                }
            val timeProvider = RecordingTimeProvider()

            val outcome =
                createUseCase(backend = backend, timeProvider = timeProvider)(noPurchaseSummary())

            assertFailClosed(outcome, BillingMessageCode.REFRESH_BACKEND_FAILED)
            assertEquals(listOf(INSTALL_ID), backend.refreshInstallIds)
            assertTrue(backend.verifyRequests.isEmpty())
            assertEquals(1, timeProvider.callCount)
        }

    @Test
    fun missingOrBlankInstallIdFailsRefreshClosedAndPreservesCurrentMessage() =
        runBlocking {
            listOf<String?>(null, "", "   ").forEach { installId ->
                val backend = RecordingBillingBackendClient(isConfigured = true)
                val timeProvider = RecordingTimeProvider()
                val outcome =
                    createUseCase(
                        backend = backend,
                        timeProvider = timeProvider,
                        installId = installId,
                    )(noPurchaseSummary())

                assertFailClosed(
                    outcome = outcome,
                    expectedMessageCode = null,
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
                val backend = RecordingBillingBackendClient(isConfigured = false)
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
                        BillingMessageCode.SUBSCRIPTION_ACTIVE
                    } else {
                        BillingMessageCode.BACKEND_VERIFICATION_REQUIRED
                    },
                    outcome.messageCodeToApply,
                )
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
                    backend = RecordingBillingBackendClient(isConfigured = false),
                    clientOnlyModeRequested = true,
                    internalTestingBuild = true,
                )(
                    purchasedSummary(
                        hasPendingSubscription = true,
                        unacknowledgedTokens = emptyList(),
                    ),
                )

            assertEquals(BillingEntitlementState.ACTIVE, outcome.entitlementSnapshot.state)
            assertEquals(BillingMessageCode.SUBSCRIPTION_ACTIVE, outcome.messageCodeToApply)
            assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
        }

    @Test
    fun pendingOnlyAndNoPurchaseNeverGrantClientOnlyProtectionOrAcknowledgement() =
        runBlocking {
            listOf(
                noPurchaseSummary(
                    hasPendingSubscription = true,
                    unacknowledgedTokens = ACK_TOKENS,
                ) to
                    BillingMessageCode.PURCHASE_PENDING,
                noPurchaseSummary(
                    hasPendingSubscription = false,
                    unacknowledgedTokens = ACK_TOKENS,
                ) to
                    BillingMessageCode.NO_ACTIVE_SUBSCRIPTION,
            ).forEach { (purchaseSummary, expectedMessageCode) ->
                val outcome =
                    createUseCase(
                        backend = RecordingBillingBackendClient(isConfigured = false),
                        clientOnlyModeRequested = true,
                        internalTestingBuild = true,
                    )(purchaseSummary)

                assertEquals(BillingEntitlementState.EXPIRED, outcome.entitlementSnapshot.state)
                assertFalse(outcome.entitlementSnapshot.isActive)
                assertEquals(expectedMessageCode, outcome.messageCodeToApply)
                assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
            }
        }

    @Test
    fun backendSnapshotsUseStableMessageCodesForEveryEntitlementState() =
        runBlocking {
            val expectedCodes =
                mapOf(
                    BillingEntitlementState.ACTIVE to BillingMessageCode.SUBSCRIPTION_ACTIVE,
                    BillingEntitlementState.CANCELED_ACTIVE to
                        BillingMessageCode.SUBSCRIPTION_CANCELED_ACTIVE,
                    BillingEntitlementState.IN_GRACE to BillingMessageCode.SUBSCRIPTION_IN_GRACE,
                    BillingEntitlementState.PENDING to BillingMessageCode.PURCHASE_PENDING,
                    BillingEntitlementState.ON_HOLD to BillingMessageCode.PAYMENT_ISSUE,
                    BillingEntitlementState.REVOKED to BillingMessageCode.SUBSCRIPTION_REVOKED,
                    BillingEntitlementState.EXPIRED to BillingMessageCode.NO_ACTIVE_SUBSCRIPTION,
                    BillingEntitlementState.UNKNOWN to BillingMessageCode.VERIFICATION_UNAVAILABLE,
                )

            expectedCodes.forEach { (state, expectedMessageCode) ->
                val exactSnapshot =
                    BillingEntitlementSnapshot(
                        state = state,
                        checkedAtMillis = state.ordinal.toLong(),
                    )
                val backend =
                    RecordingBillingBackendClient(
                        isConfigured = true,
                        verifyResult = exactSnapshot,
                    )

                val outcome = createUseCase(backend)(purchasedSummary())

                assertSame(exactSnapshot, outcome.entitlementSnapshot)
                assertEquals(expectedMessageCode, outcome.messageCodeToApply)
                assertTrue(outcome.purchaseTokensToAcknowledge.isEmpty())
            }
        }

    @Test
    fun paidBackendStatesTakePriorityWhilePendingOverridesOtherBackendMessages() =
        runBlocking {
            val expectedCodes =
                mapOf(
                    BillingEntitlementState.ACTIVE to BillingMessageCode.SUBSCRIPTION_ACTIVE,
                    BillingEntitlementState.CANCELED_ACTIVE to
                        BillingMessageCode.SUBSCRIPTION_CANCELED_ACTIVE,
                    BillingEntitlementState.IN_GRACE to BillingMessageCode.SUBSCRIPTION_IN_GRACE,
                    BillingEntitlementState.PENDING to BillingMessageCode.PURCHASE_PENDING,
                    BillingEntitlementState.ON_HOLD to BillingMessageCode.PURCHASE_PENDING,
                    BillingEntitlementState.REVOKED to BillingMessageCode.PURCHASE_PENDING,
                    BillingEntitlementState.EXPIRED to BillingMessageCode.PURCHASE_PENDING,
                    BillingEntitlementState.UNKNOWN to BillingMessageCode.PURCHASE_PENDING,
                )

            expectedCodes.forEach { (state, expectedMessageCode) ->
                val backend =
                    RecordingBillingBackendClient(
                        isConfigured = true,
                        verifyResult =
                            BillingEntitlementSnapshot(
                                state = state,
                                checkedAtMillis = NOW_MILLIS,
                            ),
                    )

                val outcome =
                    createUseCase(backend)(
                        purchasedSummary(hasPendingSubscription = true),
                    )

                assertEquals(expectedMessageCode, outcome.messageCodeToApply)
            }
        }

    @Test
    fun blankPurchaseTokenRemainsAPurchasedSubscriptionAndIsVerifiedExactly() =
        runBlocking {
            val backend = RecordingBillingBackendClient(isConfigured = true)
            val purchaseSummary = purchasedSummary(purchaseToken = "")
            val useCase = createUseCase(backend)

            assertTrue(purchaseSummary.hasPurchasedSubscription)
            assertEquals(
                BillingMessageCode.VERIFYING_WITH_BACKEND,
                useCase.messageCodeWhileSyncing(purchaseSummary),
            )

            useCase(purchaseSummary)

            assertEquals("", backend.verifyRequests.single().purchaseToken)
            assertTrue(backend.refreshInstallIds.isEmpty())
        }

    private fun createUseCase(
        backend: RecordingBillingBackendClient,
        timeProvider: RecordingTimeProvider = RecordingTimeProvider(),
        installId: String? = INSTALL_ID,
        clientOnlyModeRequested: Boolean = false,
        internalTestingBuild: Boolean = false,
    ): SyncBillingEntitlementUseCase =
        SyncBillingEntitlementUseCase(
            billingBackendClient = backend,
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
        expectedMessageCode: BillingMessageCode?,
        expectedClearsLoading: Boolean = true,
    ) {
        assertEquals(BillingEntitlementState.UNKNOWN, outcome.entitlementSnapshot.state)
        assertEquals(NOW_MILLIS, outcome.entitlementSnapshot.checkedAtMillis)
        assertNull(outcome.entitlementSnapshot.activeUntilMillis)
        assertFalse(outcome.entitlementSnapshot.isActive)
        assertEquals(expectedMessageCode, outcome.messageCodeToApply)
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

    private class RecordingBillingBackendClient(
        override val isConfigured: Boolean,
        var verifyResult: BillingEntitlementSnapshot = ACTIVE_SNAPSHOT,
        var refreshResult: BillingEntitlementSnapshot? = ACTIVE_SNAPSHOT,
    ) : BillingBackendClient {
        val verifyRequests = mutableListOf<BillingBackendPurchaseRequest>()
        val refreshInstallIds = mutableListOf<String>()
        var verifyFailure: RuntimeException? = null
        var refreshFailure: RuntimeException? = null

        override suspend fun verifyPurchase(request: BillingBackendPurchaseRequest): BillingEntitlementSnapshot {
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
