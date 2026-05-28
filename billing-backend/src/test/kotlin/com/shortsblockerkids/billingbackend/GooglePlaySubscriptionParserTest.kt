package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class GooglePlaySubscriptionParserTest {
    @Test
    fun activeSubscriptionMapsToActiveEntitlement() {
        val subscription =
            parseSubscription(
                rawState = "SUBSCRIPTION_STATE_ACTIVE",
                expiryTime = "2026-01-02T00:00:00Z",
                autoRenewEnabled = true,
            )

        assertEquals(SubscriptionEntitlementState.ACTIVE, subscription.state)
        assertEquals(1_767_312_000_000L, subscription.activeUntilMillis)
        assertEquals(true, subscription.acknowledged)
    }

    @Test
    fun canceledSubscriptionStillInPaidPeriodMapsToCanceledActive() {
        val subscription =
            parseSubscription(
                rawState = "SUBSCRIPTION_STATE_CANCELED",
                expiryTime = "2026-01-02T00:00:00Z",
                autoRenewEnabled = false,
            )

        assertEquals(SubscriptionEntitlementState.CANCELED_ACTIVE, subscription.state)
    }

    @Test
    fun activeSubscriptionWithAutoRenewDisabledMapsToCanceledActiveUntilExpiry() {
        val subscription =
            parseSubscription(
                rawState = "SUBSCRIPTION_STATE_ACTIVE",
                expiryTime = "2026-01-02T00:00:00Z",
                autoRenewEnabled = false,
            )

        assertEquals(SubscriptionEntitlementState.CANCELED_ACTIVE, subscription.state)
    }

    @Test
    fun canceledSubscriptionAfterPaidPeriodMapsToExpired() {
        val subscription =
            parseSubscription(
                rawState = "SUBSCRIPTION_STATE_CANCELED",
                expiryTime = "2025-12-31T00:00:00Z",
                autoRenewEnabled = false,
            )

        assertEquals(SubscriptionEntitlementState.EXPIRED, subscription.state)
    }

    @Test
    fun expiredSubscriptionMapsToExpired() {
        val subscription =
            parseSubscription(
                rawState = "SUBSCRIPTION_STATE_EXPIRED",
                expiryTime = "2025-12-31T00:00:00Z",
                autoRenewEnabled = false,
            )

        assertEquals(SubscriptionEntitlementState.EXPIRED, subscription.state)
    }

    @Test(expected = InvalidPurchaseTokenException::class)
    fun missingPurchasedProductIsRejectedAsInvalidToken() {
        parseSubscription(productId = "other_product")
    }

    private fun parseSubscription(
        productId: String = "shorts_blocker_kids_monthly",
        rawState: String = "SUBSCRIPTION_STATE_ACTIVE",
        expiryTime: String = "2026-01-02T00:00:00Z",
        autoRenewEnabled: Boolean = true,
    ): VerifiedSubscription =
        GooglePlaySubscriptionParser.parse(
            response =
                JsonBodies.json
                    .parseToJsonElement(
                        """
                        {
                          "subscriptionState": "$rawState",
                          "acknowledgementState": "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
                          "lineItems": [
                            {
                              "productId": "shorts_blocker_kids_monthly",
                              "expiryTime": "$expiryTime",
                              "autoRenewingPlan": {
                                "autoRenewEnabled": $autoRenewEnabled
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                    ).jsonObject,
            productId = productId,
            purchaseToken = "token-1",
            nowMillis = 1_767_225_600_000L,
        )
}
