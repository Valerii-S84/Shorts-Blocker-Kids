package com.shortsblockerkids.presentation.billing

import com.shortsblockerkids.application.billing.BillingPurchaseSummary
import com.shortsblockerkids.application.billing.BillingSyncOutcome
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
        assertEquals(BillingMessageCode.PRODUCT_UNAVAILABLE, unavailable.toBillingUiMessage()?.code)
        assertEquals("Play formatted price", available.productPrice)
    }

    @Test
    fun sdkFailuresKeepOperationAndResponseCodeMappingStable() {
        val expectedCodes =
            mapOf(
                PlayBillingOperation.SETUP to BillingMessageCode.BILLING_UNAVAILABLE,
                PlayBillingOperation.OPEN_PURCHASE_FLOW to BillingMessageCode.OPEN_PURCHASE_FLOW_FAILED,
                PlayBillingOperation.PURCHASE_UPDATE to BillingMessageCode.PURCHASE_FAILED,
                PlayBillingOperation.LOAD_PRODUCT_DETAILS to BillingMessageCode.LOAD_SUBSCRIPTION_FAILED,
                PlayBillingOperation.RESTORE_PURCHASES to BillingMessageCode.RESTORE_PURCHASES_FAILED,
                PlayBillingOperation.ACKNOWLEDGE_PURCHASE to BillingMessageCode.ACKNOWLEDGE_PURCHASE_FAILED,
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
        assertNull(PlayBillingEvent.PurchasesObserved(BillingPurchaseSummary(null)).toBillingUiMessage())
    }

    @Test
    fun playPurchaseStatesMapToAccessMessages() {
        val expectedCodes =
            mapOf(
                BillingEntitlementState.ACTIVE to BillingMessageCode.SUBSCRIPTION_ACTIVE,
                BillingEntitlementState.PENDING to BillingMessageCode.PURCHASE_PENDING,
                BillingEntitlementState.INACTIVE to BillingMessageCode.NO_ACTIVE_SUBSCRIPTION,
            )

        expectedCodes.forEach { (state, expectedCode) ->
            assertEquals(expectedCode, outcome(state).toBillingUiMessage().code)
        }
        assertEquals(
            BillingMessageCode.SUBSCRIPTION_ACTIVE,
            outcome(BillingEntitlementState.ACTIVE, hasPendingSubscription = true)
                .toBillingUiMessage()
                .code,
        )
        assertEquals(
            BillingMessageCode.PURCHASE_PENDING,
            outcome(BillingEntitlementState.INACTIVE, hasPendingSubscription = true)
                .toBillingUiMessage()
                .code,
        )
    }

    private fun outcome(
        state: BillingEntitlementState,
        hasPendingSubscription: Boolean = false,
    ): BillingSyncOutcome =
        BillingSyncOutcome(
            entitlementSnapshot = BillingEntitlementSnapshot(state),
            hasPendingSubscription = hasPendingSubscription,
        )
}
