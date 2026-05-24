package com.shortsblockerkids.core.billing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingBackendClientTest {
    @Test
    fun blankBackendUrlKeepsClientOnlyBillingPathDisabled() {
        runBlocking {
            val client = HttpBillingBackendClient.fromBaseUrl("")

            assertSame(DisabledBillingBackendClient, client)
            assertFalse(client.isConfigured)
            assertTrue(client.refreshEntitlement("install-id") == null)
        }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun disabledBackendDoesNotVerifyPurchases() {
        runBlocking {
            DisabledBillingBackendClient.verifyPurchase(
                BillingBackendPurchaseRequest(
                    installId = "install-id",
                    packageName = "com.shortsblockerkids",
                    productId = "shorts_blocker_kids_monthly",
                    purchaseToken = "token",
                    appVersion = "0.1.0",
                ),
            )
        }
    }
}
