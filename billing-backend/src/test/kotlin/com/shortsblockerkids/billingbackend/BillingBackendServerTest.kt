package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

class BillingBackendServerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val startedServers = mutableListOf<BillingBackendServer>()

    @After
    fun tearDown() {
        startedServers.forEach { it.stop() }
    }

    @Test
    fun verifyPurchaseStoresBackendEntitlement() {
        val backend = startBackend()

        val response =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body =
                    """
                    {
                      "install_id":"install-1",
                      "package_name":"com.shortsblockerkids",
                      "product_id":"shorts_blocker_kids_monthly",
                      "purchase_token":"token-1",
                      "app_version":"0.1.0"
                    }
                    """.trimIndent(),
            )

        assertTrue(response.contains("\"state\":\"ACTIVE\""))
        assertTrue(response.contains("\"is_active\":true"))
    }

    @Test
    fun rtdnRequiresSharedSecretAndIsIdempotent() {
        val backend = startBackend()
        post(
            url = "http://127.0.0.1:${backend.port}/billing/play/verify",
            body =
                """
                {
                  "install_id":"install-1",
                  "package_name":"com.shortsblockerkids",
                  "product_id":"shorts_blocker_kids_monthly",
                  "purchase_token":"token-1"
                }
                """.trimIndent(),
        )

        val unauthorized =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/rtdn",
                body = pubSubBody("msg-1", "token-1"),
                headers = emptyMap(),
                expectedCode = 400,
            )
        val updated =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/rtdn",
                body = pubSubBody("msg-1", "token-1"),
                headers = mapOf(BillingBackendServer.RTDN_SECRET_HEADER to "secret"),
            )
        val duplicate =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/rtdn",
                body = pubSubBody("msg-1", "token-1"),
                headers = mapOf(BillingBackendServer.RTDN_SECRET_HEADER to "secret"),
            )

        assertTrue(unauthorized.contains("Invalid RTDN source"))
        assertTrue(updated.contains("\"status\":\"updated\""))
        assertTrue(duplicate.contains("\"status\":\"duplicate\""))
    }

    @Test
    fun duplicatePurchaseTokenCannotGrantSecondInstall() {
        val backend = startBackend()
        post(
            url = "http://127.0.0.1:${backend.port}/billing/play/verify",
            body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
        )

        val duplicate =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-2", purchaseToken = "token-1"),
                expectedCode = 400,
            )
        val firstInstallStatus =
            get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=install-1")
        val secondInstallStatus =
            get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=install-2")

        assertTrue(duplicate.contains("Purchase token already belongs to another installation"))
        assertTrue(firstInstallStatus.contains("\"state\":\"ACTIVE\""))
        assertTrue(secondInstallStatus.contains("\"state\":\"UNKNOWN\""))
    }

    @Test
    fun statusReturnsUnknownForUnregisteredInstall() {
        val backend = startBackend()

        val response =
            get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=missing")

        assertTrue(response.contains("\"state\":\"UNKNOWN\""))
        assertTrue(response.contains("\"is_active\":false"))
    }

    private fun startBackend(): BillingBackendServer {
        val config =
            BackendConfig(
                port = 0,
                packageName = "com.shortsblockerkids",
                productId = "shorts_blocker_kids_monthly",
                storeFile = temporaryFolder.newFile("backend.json").toPath(),
                rtdnSharedSecret = "secret",
            )
        val backend =
            BillingBackendServer.create(
                config = config,
                verifier = FakeVerifier(),
            )
        backend.start()
        startedServers.add(backend)
        return backend
    }

    private fun verifyBody(
        installId: String,
        purchaseToken: String,
    ): String =
        """
        {
          "install_id":"$installId",
          "package_name":"com.shortsblockerkids",
          "product_id":"shorts_blocker_kids_monthly",
          "purchase_token":"$purchaseToken",
          "app_version":"0.1.0"
        }
        """.trimIndent()

    private fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        expectedCode: Int = 200,
    ): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        connection.outputStream.use { it.write(body.toByteArray()) }
        assertEquals(expectedCode, connection.responseCode)
        return connection.responseBody()
    }

    private fun get(url: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        assertEquals(200, connection.responseCode)
        return connection.responseBody()
    }

    private fun HttpURLConnection.responseBody(): String =
        if (responseCode in 200..299) {
            inputStream.bufferedReader().use { it.readText() }
        } else {
            errorStream.bufferedReader().use { it.readText() }
        }

    private fun pubSubBody(
        messageId: String,
        token: String,
    ): String {
        val data =
            JsonObject(
                mapOf(
                    "subscriptionNotification" to
                        JsonObject(
                            mapOf(
                                "subscriptionId" to JsonPrimitive("shorts_blocker_kids_monthly"),
                                "purchaseToken" to JsonPrimitive(token),
                            ),
                        ),
                ),
            )
        val encoded =
            Base64.getEncoder().encodeToString(
                JsonBodies.json.encodeToString(JsonObject.serializer(), data).toByteArray(),
            )
        return """
            {
              "message": {
                "messageId": "$messageId",
                "data": "$encoded"
              }
            }
            """.trimIndent()
    }
}

private class FakeVerifier : PlaySubscriptionVerifier {
    override fun verify(
        packageName: String,
        productId: String,
        purchaseToken: String,
    ): VerifiedSubscription =
        VerifiedSubscription(
            productId = productId,
            purchaseToken = purchaseToken,
            state = SubscriptionEntitlementState.ACTIVE,
            activeUntilMillis = 10_000L,
            acknowledged = true,
            verifiedAtMillis = 5_000L,
        )
}
