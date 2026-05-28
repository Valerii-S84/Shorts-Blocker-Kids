package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun canceledActivePurchaseRemainsActiveUntilPaidPeriodEnds() {
        val backend =
            startBackend(
                FakeVerifier(
                    state = SubscriptionEntitlementState.CANCELED_ACTIVE,
                    activeUntilMillis = 20_000L,
                    verifiedAtMillis = 7_000L,
                ),
            )

        val response =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
            )

        assertTrue(response.contains("\"state\":\"CANCELED_ACTIVE\""))
        assertTrue(response.contains("\"is_active\":true"))
        assertTrue(response.contains("\"active_until_millis\":20000"))
        assertTrue(response.contains("\"checked_at_millis\":7000"))
    }

    @Test
    fun expiredPurchaseDoesNotGrantEntitlement() {
        val backend =
            startBackend(
                FakeVerifier(
                    state = SubscriptionEntitlementState.EXPIRED,
                    activeUntilMillis = 4_000L,
                    verifiedAtMillis = 8_000L,
                ),
            )

        val response =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
            )

        assertTrue(response.contains("\"state\":\"EXPIRED\""))
        assertTrue(response.contains("\"is_active\":false"))
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
    fun repeatedVerificationUpdatesExistingEntitlement() {
        val backend =
            startBackend(
                SequenceVerifier(
                    verifiedSubscription(
                        state = SubscriptionEntitlementState.ACTIVE,
                        verifiedAtMillis = 5_000L,
                    ),
                    verifiedSubscription(
                        state = SubscriptionEntitlementState.EXPIRED,
                        activeUntilMillis = 4_000L,
                        verifiedAtMillis = 9_000L,
                    ),
                ),
            )

        post(
            url = "http://127.0.0.1:${backend.port}/billing/play/verify",
            body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
        )
        val updated =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
            )
        val status = get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=install-1")

        assertTrue(updated.contains("\"state\":\"EXPIRED\""))
        assertTrue(status.contains("\"state\":\"EXPIRED\""))
        assertTrue(status.contains("\"is_active\":false"))
        assertTrue(status.contains("\"checked_at_millis\":9000"))
    }

    @Test
    fun statusReturnsUnknownForUnregisteredInstall() {
        val backend = startBackend()

        val response =
            get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=missing")

        assertTrue(response.contains("\"state\":\"UNKNOWN\""))
        assertTrue(response.contains("\"is_active\":false"))
    }

    @Test
    fun invalidPurchaseTokenIsRejectedWithoutStoredEntitlement() {
        val backend = startBackend(ThrowingVerifier(InvalidPurchaseTokenException()))

        val response =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
                expectedCode = 400,
            )
        val status = get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=install-1")

        assertTrue(response.contains("Invalid purchase token"))
        assertFalse(response.contains("token-1"))
        assertTrue(status.contains("\"state\":\"UNKNOWN\""))
        assertTrue(status.contains("\"is_active\":false"))
    }

    @Test
    fun purchaseNotFoundDoesNotCreateEntitlement() {
        val backend = startBackend(ThrowingVerifier(PurchaseNotFoundException()))

        val response =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
                expectedCode = 404,
            )
        val status = get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=install-1")

        assertTrue(response.contains("Purchase not found"))
        assertTrue(status.contains("\"state\":\"UNKNOWN\""))
        assertTrue(status.contains("\"is_active\":false"))
    }

    @Test
    fun backendUnavailableReturnsServiceUnavailableWithoutGrantingEntitlement() {
        val backend = startBackend(ThrowingVerifier(BillingBackendUnavailableException("upstream down")))

        val response =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
                expectedCode = 503,
            )
        val status = get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=install-1")

        assertTrue(response.contains("Billing backend temporarily unavailable"))
        assertFalse(response.contains("upstream down"))
        assertTrue(status.contains("\"state\":\"UNKNOWN\""))
    }

    @Test
    fun unexpectedBackendErrorReturnsGenericErrorWithoutGrantingEntitlement() {
        val backend = startBackend(ThrowingVerifier(RuntimeException("token-1 leaked")))

        val response =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
                expectedCode = 500,
            )
        val status = get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=install-1")

        assertTrue(response.contains("Internal billing backend error"))
        assertFalse(response.contains("token-1"))
        assertFalse(response.contains("leaked"))
        assertTrue(status.contains("\"state\":\"UNKNOWN\""))
    }

    @Test
    fun healthDoesNotRequireGoogleCredentialsAtStartup() {
        val config =
            BackendConfig(
                port = 0,
                packageName = "com.shortsblockerkids",
                productId = "shorts_blocker_kids_monthly",
                storeFile = temporaryFolder.newFile("health.json").toPath(),
                rtdnSharedSecret = null,
            )
        val backend = BillingBackendServer.create(config = config)
        backend.start()
        startedServers.add(backend)

        val response = get("http://127.0.0.1:${backend.port}/health")

        assertTrue(response.contains("\"status\":\"ok\""))
    }

    @Test
    fun billingEndpointsRequireHttpsForwardingHeaderWhenConfigured() {
        val backend = startBackend(config = backendConfig(requireHttps = true))

        val insecure =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
                expectedCode = 400,
            )
        val secure =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
                headers = mapOf("X-Forwarded-Proto" to "https"),
            )

        assertTrue(insecure.contains("HTTPS is required"))
        assertTrue(secure.contains("\"state\":\"ACTIVE\""))
    }

    @Test
    fun verifyEndpointIsRateLimited() {
        val backend =
            startBackend(
                config =
                    backendConfig(
                        rateLimits = RateLimitConfig(verifyPerMinute = 1),
                    ),
            )
        post(
            url = "http://127.0.0.1:${backend.port}/billing/play/verify",
            body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
        )

        val limited =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
                expectedCode = 429,
            )

        assertTrue(limited.contains("Rate limit exceeded"))
    }

    @Test
    fun oversizedRequestBodyIsRejected() {
        val backend = startBackend(config = backendConfig(maxRequestBytes = 16))

        val response =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = verifyBody(installId = "install-1", purchaseToken = "token-1"),
                expectedCode = 413,
            )

        assertTrue(response.contains("Request body too large"))
    }

    @Test
    fun verifyRejectsMissingRequiredFieldsWithoutStoredEntitlement() {
        val backend = startBackend()

        val response =
            post(
                url = "http://127.0.0.1:${backend.port}/billing/play/verify",
                body = "{}",
                expectedCode = 400,
            )
        val status = get("http://127.0.0.1:${backend.port}/entitlement/status?install_id=install-1")

        assertTrue(response.contains("Missing required field: install_id"))
        assertTrue(status.contains("\"state\":\"UNKNOWN\""))
        assertTrue(status.contains("\"is_active\":false"))
    }

    @Test
    fun statusEndpointRejectsMissingOrInvalidInstallId() {
        val backend = startBackend()

        val missing =
            get(
                url = "http://127.0.0.1:${backend.port}/entitlement/status",
                expectedCode = 400,
            )
        val invalid =
            get(
                url = "http://127.0.0.1:${backend.port}/entitlement/status?install_id=bad%20id",
                expectedCode = 400,
            )

        assertTrue(missing.contains("Missing install_id"))
        assertTrue(invalid.contains("Missing install_id"))
    }

    private fun startBackend(
        verifier: PlaySubscriptionVerifier = FakeVerifier(),
        config: BackendConfig = backendConfig(),
    ): BillingBackendServer {
        val backend =
            BillingBackendServer.create(
                config = config,
                verifier = verifier,
            )
        backend.start()
        startedServers.add(backend)
        return backend
    }

    private fun backendConfig(
        requireHttps: Boolean = false,
        maxRequestBytes: Int = 32_768,
        rateLimits: RateLimitConfig = RateLimitConfig(),
    ): BackendConfig {
        val config =
            BackendConfig(
                port = 0,
                packageName = "com.shortsblockerkids",
                productId = "shorts_blocker_kids_monthly",
                storeFile = temporaryFolder.newFile("backend.json").toPath(),
                rtdnSharedSecret = "secret",
                requireHttps = requireHttps,
                maxRequestBytes = maxRequestBytes,
                rateLimits = rateLimits,
            )
        return config
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

    private fun get(
        url: String,
        expectedCode: Int = 200,
    ): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        assertEquals(expectedCode, connection.responseCode)
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
                    "packageName" to JsonPrimitive("com.shortsblockerkids"),
                    "subscriptionNotification" to
                        JsonObject(
                            mapOf(
                                "subscriptionId" to JsonPrimitive("shorts_blocker_kids_monthly"),
                                "purchaseToken" to JsonPrimitive(token),
                                "notificationType" to JsonPrimitive(2),
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

private class FakeVerifier(
    state: SubscriptionEntitlementState = SubscriptionEntitlementState.ACTIVE,
    activeUntilMillis: Long? = 10_000L,
    acknowledged: Boolean = true,
    verifiedAtMillis: Long = 5_000L,
) : PlaySubscriptionVerifier {
    private val subscription =
        verifiedSubscription(
            state = state,
            activeUntilMillis = activeUntilMillis,
            acknowledged = acknowledged,
            verifiedAtMillis = verifiedAtMillis,
        )

    override fun verify(
        packageName: String,
        productId: String,
        purchaseToken: String,
    ): VerifiedSubscription = subscription.copy(productId = productId, purchaseToken = purchaseToken)
}

private class SequenceVerifier(
    vararg subscriptions: VerifiedSubscription,
) : PlaySubscriptionVerifier {
    private val remaining = subscriptions.toMutableList()

    override fun verify(
        packageName: String,
        productId: String,
        purchaseToken: String,
    ): VerifiedSubscription =
        remaining
            .removeAt(0)
            .copy(productId = productId, purchaseToken = purchaseToken)
}

private class ThrowingVerifier(
    private val throwable: RuntimeException,
) : PlaySubscriptionVerifier {
    override fun verify(
        packageName: String,
        productId: String,
        purchaseToken: String,
    ): VerifiedSubscription = throw throwable
}

private fun verifiedSubscription(
    state: SubscriptionEntitlementState,
    activeUntilMillis: Long? = 10_000L,
    acknowledged: Boolean = true,
    verifiedAtMillis: Long = 5_000L,
): VerifiedSubscription =
    VerifiedSubscription(
        productId = "shorts_blocker_kids_monthly",
        purchaseToken = "purchase-token",
        state = state,
        activeUntilMillis = activeUntilMillis,
        acknowledged = acknowledged,
        verifiedAtMillis = verifiedAtMillis,
    )
