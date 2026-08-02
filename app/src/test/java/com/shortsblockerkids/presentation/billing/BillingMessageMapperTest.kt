package com.shortsblockerkids.presentation.billing

import com.shortsblockerkids.application.billing.BillingPurchaseSummary
import com.shortsblockerkids.application.billing.BillingSyncOutcome
import com.shortsblockerkids.application.billing.BillingSyncStatus
import com.shortsblockerkids.application.billing.PlayBillingEvent
import com.shortsblockerkids.application.billing.PlayBillingOperation
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillingMessageMapperTest {
    @Test
    fun gatewayEventsUseStableMessageCodes() {
        val expectedCodes =
            mapOf(
                PlayBillingEvent.Connecting to BillingMessageCode.CONNECTING,
                PlayBillingEvent.Connected to BillingMessageCode.CONNECTED,
                PlayBillingEvent.Disconnected to BillingMessageCode.DISCONNECTED,
                PlayBillingEvent.SubscriptionNotReady to BillingMessageCode.SUBSCRIPTION_NOT_READY,
                PlayBillingEvent.OpeningPurchaseFlow to BillingMessageCode.OPENING_PURCHASE_FLOW,
                PlayBillingEvent.ManageSubscriptionUnavailable to
                    BillingMessageCode.MANAGE_SUBSCRIPTION_UNAVAILABLE,
                PlayBillingEvent.PurchaseCanceled to BillingMessageCode.PURCHASE_CANCELED,
            )

        expectedCodes.forEach { (event, expectedCode) ->
            assertEquals(expectedCode, event.toBillingUiMessage()?.code)
        }
    }

    @Test
    fun productAvailabilityMapsWithoutChangingFormattedPriceData() {
        val available =
            PlayBillingEvent.ProductDetailsLoaded(
                productPrice = "Play formatted price",
                isProductAvailable = true,
                canStartPurchase = true,
            )
        val unavailable =
            PlayBillingEvent.ProductDetailsLoaded(
                productPrice = null,
                isProductAvailable = false,
                canStartPurchase = false,
            )

        assertEquals(BillingMessageCode.PRODUCT_LOADED, available.toBillingUiMessage()?.code)
        assertEquals(
            BillingMessageCode.PRODUCT_UNAVAILABLE,
            unavailable.toBillingUiMessage()?.code,
        )
        assertEquals("Play formatted price", available.productPrice)
    }

    @Test
    fun gatewayFailuresKeepOperationAndResponseCodeMappingStable() {
        val expectedCodes =
            mapOf(
                PlayBillingOperation.SETUP to BillingMessageCode.BILLING_UNAVAILABLE,
                PlayBillingOperation.OPEN_PURCHASE_FLOW to
                    BillingMessageCode.OPEN_PURCHASE_FLOW_FAILED,
                PlayBillingOperation.PURCHASE_UPDATE to BillingMessageCode.PURCHASE_FAILED,
                PlayBillingOperation.LOAD_PRODUCT_DETAILS to
                    BillingMessageCode.LOAD_SUBSCRIPTION_FAILED,
                PlayBillingOperation.RESTORE_PURCHASES to
                    BillingMessageCode.RESTORE_PURCHASES_FAILED,
                PlayBillingOperation.ACKNOWLEDGE_PURCHASE to
                    BillingMessageCode.ACKNOWLEDGE_PURCHASE_FAILED,
            )

        expectedCodes.forEach { (operation, expectedCode) ->
            val message =
                PlayBillingEvent
                    .OperationFailed(
                        operation = operation,
                        responseCode = 6,
                        isReady = false,
                        canStartPurchase = false,
                    ).toBillingUiMessage()

            assertEquals(expectedCode, message?.code)
            assertEquals(6, message?.responseCode)
        }
    }

    @Test
    fun stateOnlyGatewayEventsDoNotReplaceMessages() {
        assertNull(PlayBillingEvent.ProductDetailsLoading.toBillingUiMessage())
        assertNull(PlayBillingEvent.PurchaseFlowFinished.toBillingUiMessage())
        assertNull(
            PlayBillingEvent
                .PurchasesObserved(BillingPurchaseSummary(null))
                .toBillingUiMessage(),
        )
    }

    @Test
    fun syncFailuresAndRequirementsUseStableMessageCodes() {
        val expectedCodes =
            mapOf(
                BillingSyncStatus.BACKEND_VERIFICATION_REQUIRED to
                    BillingMessageCode.BACKEND_VERIFICATION_REQUIRED,
                BillingSyncStatus.BACKEND_INSTALLATION_ID_UNAVAILABLE to
                    BillingMessageCode.BACKEND_VERIFICATION_NOT_READY,
                BillingSyncStatus.BACKEND_PURCHASE_VERIFICATION_FAILED to
                    BillingMessageCode.VERIFY_WITH_BACKEND_FAILED,
                BillingSyncStatus.BACKEND_ENTITLEMENT_REFRESH_FAILED to
                    BillingMessageCode.REFRESH_BACKEND_FAILED,
            )

        expectedCodes.forEach { (status, expectedCode) ->
            assertEquals(expectedCode, outcome(status = status).toBillingUiMessage()?.code)
        }
        assertNull(outcome(status = null).toBillingUiMessage())
    }

    @Test
    fun resolvedBackendStatesUseStableMessageCodes() {
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

        expectedCodes.forEach { (state, expectedCode) ->
            assertEquals(
                expectedCode,
                outcome(state = state).toBillingUiMessage()?.code,
            )
        }
    }

    @Test
    fun paidStatesWinWhilePendingOverridesOtherResolvedStates() {
        val paidStates =
            mapOf(
                BillingEntitlementState.ACTIVE to BillingMessageCode.SUBSCRIPTION_ACTIVE,
                BillingEntitlementState.CANCELED_ACTIVE to
                    BillingMessageCode.SUBSCRIPTION_CANCELED_ACTIVE,
                BillingEntitlementState.IN_GRACE to BillingMessageCode.SUBSCRIPTION_IN_GRACE,
            )
        paidStates.forEach { (state, expectedCode) ->
            assertEquals(
                expectedCode,
                outcome(state = state, hasPendingSubscription = true).toBillingUiMessage()?.code,
            )
        }
        val nonPaidStates =
            BillingEntitlementState.entries - paidStates.keys
        nonPaidStates.forEach { state ->
            assertEquals(
                BillingMessageCode.PURCHASE_PENDING,
                outcome(state = state, hasPendingSubscription = true).toBillingUiMessage()?.code,
            )
        }
    }

    @Test
    fun verificationProgressUsesStableCode() {
        assertEquals(
            BillingMessageCode.VERIFYING_WITH_BACKEND,
            billingVerificationProgressMessage().code,
        )
    }

    private fun outcome(
        status: BillingSyncStatus? = BillingSyncStatus.ENTITLEMENT_RESOLVED,
        state: BillingEntitlementState = BillingEntitlementState.UNKNOWN,
        hasPendingSubscription: Boolean = false,
    ): BillingSyncOutcome =
        BillingSyncOutcome(
            entitlementSnapshot = BillingEntitlementSnapshot(state, checkedAtMillis = 1L),
            status = status,
            hasPendingSubscription = hasPendingSubscription,
        )
}
