package com.shortsblockerkids.presentation.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class BillingUiStateTest {
    @Test
    fun defaultStateUsesStableConnectingCode() {
        val state = BillingUiState()

        assertEquals(BillingMessageCode.CONNECTING, state.message.code)
        assertEquals(null, state.message.responseCode)
    }

    @Test
    fun billingMessageKeepsTechnicalResponseCodeSeparateFromPresentation() {
        val message =
            BillingUiMessage(
                code = BillingMessageCode.PURCHASE_FAILED,
                responseCode = 6,
            )

        assertEquals(BillingMessageCode.PURCHASE_FAILED, message.code)
        assertEquals(6, message.responseCode)
    }

    @Test
    fun productPriceRemainsTheGooglePlayFormattedValue() {
        val formattedPrice = "localized Play price"
        val state = BillingUiState(productPrice = formattedPrice)

        assertEquals(formattedPrice, state.productPrice)
    }
}
