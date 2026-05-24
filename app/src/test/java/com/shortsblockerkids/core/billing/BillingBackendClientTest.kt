package com.shortsblockerkids.core.billing

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

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

    @Test
    fun configuredBackendVerifiesPurchaseViaPostAndParsesEntitlement() {
        runBlocking {
            SingleResponseHttpServer(
                statusCode = 200,
                responseBody =
                    """
                    {
                      "state": "ACTIVE",
                      "checked_at_millis": 1234,
                      "active_until_millis": 5678
                    }
                    """.trimIndent(),
            ).use { server ->
                val client = HttpBillingBackendClient.fromBaseUrl(server.baseUrl)

                val snapshot =
                    client.verifyPurchase(
                        BillingBackendPurchaseRequest(
                            installId = "install-id",
                            packageName = "com.shortsblockerkids",
                            productId = BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID,
                            purchaseToken = "token",
                            appVersion = "0.1.0",
                        ),
                    )
                val request = server.awaitRequest()
                val body = Json.parseToJsonElement(request.body).jsonObject

                assertTrue(client.isConfigured)
                assertEquals("POST", request.method)
                assertEquals("/billing/play/verify", request.target)
                assertEquals("install-id", body["install_id"]?.jsonPrimitive?.contentOrNull)
                assertEquals("com.shortsblockerkids", body["package_name"]?.jsonPrimitive?.contentOrNull)
                assertEquals(
                    BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID,
                    body["product_id"]?.jsonPrimitive?.contentOrNull,
                )
                assertEquals("token", body["purchase_token"]?.jsonPrimitive?.contentOrNull)
                assertEquals("0.1.0", body["app_version"]?.jsonPrimitive?.contentOrNull)
                assertEquals(BillingEntitlementState.ACTIVE, snapshot.state)
                assertEquals(1234L, snapshot.checkedAtMillis)
                assertEquals(5678L, snapshot.activeUntilMillis)
            }
        }
    }

    @Test
    fun refreshEntitlementEncodesInstallIdAndUsesClockFallbackForMissingFields() {
        runBlocking {
            SingleResponseHttpServer(
                statusCode = 200,
                responseBody = """{"state":"IN_GRACE"}""",
            ).use { server ->
                val client = HttpBillingBackendClient("${server.baseUrl}/", nowMillis = { 42L })

                val snapshot = client.refreshEntitlement("install id/child")
                val request = server.awaitRequest()

                assertNotNull(snapshot)
                assertEquals("GET", request.method)
                assertEquals("/entitlement/status?install_id=install+id%2Fchild", request.target)
                assertEquals(BillingEntitlementState.IN_GRACE, snapshot?.state)
                assertEquals(42L, snapshot?.checkedAtMillis)
                assertEquals(null, snapshot?.activeUntilMillis)
            }
        }
    }

    @Test
    fun unknownBackendStateFallsBackToUnknownAndNullTimestampsUseDefaults() {
        runBlocking {
            SingleResponseHttpServer(
                statusCode = 200,
                responseBody =
                    """
                    {
                      "state": "SUSPENDED_BY_MOONLIGHT",
                      "checked_at_millis": null,
                      "active_until_millis": null
                    }
                    """.trimIndent(),
            ).use { server ->
                val client = HttpBillingBackendClient(server.baseUrl, nowMillis = { 900L })

                val snapshot = client.refreshEntitlement("install-id")

                assertEquals(BillingEntitlementState.UNKNOWN, snapshot?.state)
                assertEquals(900L, snapshot?.checkedAtMillis)
                assertEquals(null, snapshot?.activeUntilMillis)
            }
        }
    }

    @Test
    fun missingStateAndInvalidTimestampsUseSafeDefaults() {
        runBlocking {
            SingleResponseHttpServer(
                statusCode = 200,
                responseBody =
                    """
                    {
                      "checked_at_millis": "not-a-time",
                      "active_until_millis": "not-a-time"
                    }
                    """.trimIndent(),
            ).use { server ->
                val client = HttpBillingBackendClient(server.baseUrl, nowMillis = { 901L })

                val snapshot = client.refreshEntitlement("install-id")

                assertEquals(BillingEntitlementState.UNKNOWN, snapshot?.state)
                assertEquals(901L, snapshot?.checkedAtMillis)
                assertEquals(null, snapshot?.activeUntilMillis)
            }
        }
    }

    @Test
    fun backendHttpErrorsSurfaceStatusWithoutResponseBodyDetails() {
        runBlocking {
            SingleResponseHttpServer(
                statusCode = 409,
                responseBody = """{"error":"token details stay server-side"}""",
            ).use { server ->
                val client = HttpBillingBackendClient(server.baseUrl)

                val error =
                    runCatching { client.refreshEntitlement("install-id") }
                        .exceptionOrNull()

                assertTrue(error is IllegalStateException)
                assertEquals("Billing backend returned HTTP 409.", error?.message)
            }
        }
    }

    @Test
    fun directBlankHttpClientRejectsNetworkOperationsConservatively() {
        runBlocking {
            val client = HttpBillingBackendClient(" ")

            val verifyError =
                runCatching {
                    client.verifyPurchase(
                        BillingBackendPurchaseRequest(
                            installId = "install-id",
                            packageName = "com.shortsblockerkids",
                            productId = BillingAvailability.MONTHLY_SUBSCRIPTION_PRODUCT_ID,
                            purchaseToken = "token",
                            appVersion = "0.1.0",
                        ),
                    )
                }.exceptionOrNull()

            assertFalse(client.isConfigured)
            assertTrue(verifyError is IllegalStateException)
            assertEquals("Billing backend is not configured.", verifyError?.message)
            assertEquals(null, client.refreshEntitlement("install-id"))
        }
    }

    private class SingleResponseHttpServer(
        private val statusCode: Int,
        private val responseBody: String,
    ) : AutoCloseable {
        private val serverSocket =
            ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        private val served = CountDownLatch(1)
        private val failure = AtomicReference<Throwable?>()
        private var recordedRequest: RecordedRequest? = null
        private val serverThread =
            thread(start = true, name = "billing-backend-client-test") {
                try {
                    serverSocket.accept().use { socket ->
                        val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                        val requestLine = reader.readLine()
                        val headers = mutableMapOf<String, String>()
                        while (true) {
                            val line = reader.readLine()
                            if (line.isNullOrEmpty()) {
                                break
                            }
                            val separatorIndex = line.indexOf(':')
                            if (separatorIndex > 0) {
                                headers[line.substring(0, separatorIndex).lowercase()] =
                                    line.substring(separatorIndex + 1).trim()
                            }
                        }
                        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                        val bodyChars = CharArray(contentLength)
                        var read = 0
                        while (read < contentLength) {
                            val next = reader.read(bodyChars, read, contentLength - read)
                            if (next == -1) {
                                break
                            }
                            read += next
                        }
                        val parts = requestLine.split(" ", limit = 3)
                        recordedRequest =
                            RecordedRequest(
                                method = parts[0],
                                target = parts[1],
                                body = String(bodyChars, 0, read),
                            )

                        val bytes = responseBody.toByteArray(Charsets.UTF_8)
                        socket.getOutputStream().use { output ->
                            output.write(
                                (
                                    "HTTP/1.1 $statusCode Test\r\n" +
                                        "Content-Type: application/json\r\n" +
                                        "Content-Length: ${bytes.size}\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                                ).toByteArray(Charsets.UTF_8),
                            )
                            output.write(bytes)
                            output.flush()
                        }
                    }
                } catch (throwable: Throwable) {
                    if (!serverSocket.isClosed) {
                        failure.set(throwable)
                    }
                } finally {
                    served.countDown()
                }
            }

        val baseUrl: String = "http://127.0.0.1:${serverSocket.localPort}"

        fun awaitRequest(): RecordedRequest {
            assertTrue("test HTTP server did not receive a request", served.await(3, TimeUnit.SECONDS))
            failure.get()?.let { throw AssertionError("test HTTP server failed", it) }
            return requireNotNull(recordedRequest) { "test HTTP server did not record a request" }
        }

        override fun close() {
            serverSocket.close()
            serverThread.join(1_000L)
        }
    }

    private data class RecordedRequest(
        val method: String,
        val target: String,
        val body: String,
    )
}
