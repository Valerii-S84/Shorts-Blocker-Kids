package com.shortsblockerkids.infrastructure.billing

import com.shortsblockerkids.application.billing.BillingVerificationRequest
import com.shortsblockerkids.application.billing.DisabledBillingVerificationPort
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class HttpBillingVerificationAdapterTest {
    @Test
    fun blankBackendKeepsBillingDisabled() = runBlocking {
        for (url in listOf("", "   ")) {
            val client = HttpBillingVerificationAdapter.fromBaseUrl(url, MemoryBillingInstallationStore())

            assertSame(DisabledBillingVerificationPort, client)
            assertFalse(client.isConfigured)
            assertEquals(null, client.refreshEntitlement("legacy-install"))
        }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun disabledBackendDoesNotVerifyPurchases() = runBlocking {
        DisabledBillingVerificationPort.verifyPurchase(purchaseRequest())
        Unit
    }

    @Test
    fun registrationSuppliesNoLegacyIdAndVerifyUsesServerIdentityAndBearer() = runBlocking {
        HttpFixture(
            Response(201, REGISTRATION),
            Response(200, ENTITLEMENT),
        ).use { server ->
            val store = MemoryBillingInstallationStore()
            val client = HttpBillingVerificationAdapter.fromBaseUrl(server.baseUrl, store)
            val snapshot = client.verifyPurchase(purchaseRequest())
            val requests = server.awaitRequests()
            val body = Json.parseToJsonElement(requests[1].body).jsonObject

            assertEquals("POST", requests[0].method)
            assertEquals("/billing/installations", requests[0].target)
            assertEquals("{}", requests[0].body)
            assertEquals(null, requests[0].headers["authorization"])
            assertEquals(INSTALL_ID, body["install_id"]?.jsonPrimitive?.content)
            assertEquals("Bearer $BEARER", requests[1].headers["authorization"])
            assertEquals("/billing/play/verify", requests[1].target)
            assertEquals("token", body["purchase_token"]?.jsonPrimitive?.content)
            assertEquals("com.shortsblockerkids", body["package_name"]?.jsonPrimitive?.content)
            assertEquals(PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID, body["product_id"]?.jsonPrimitive?.content)
            assertEquals("0.1.0", body["app_version"]?.jsonPrimitive?.content)
            assertEquals(BillingInstallation(INSTALL_ID, BEARER), store.read())
            assertEquals(BillingEntitlementState.ACTIVE, snapshot.state)
            assertEquals(1234L, snapshot.checkedAtMillis)
            assertEquals(5678L, snapshot.activeUntilMillis)
        }
    }

    @Test
    fun statusUsesStoredOwnerInsteadOfCallerSuppliedVictimUuid() = runBlocking {
        HttpFixture(Response(200, ENTITLEMENT)).use { server ->
            val client = HttpBillingVerificationAdapter.fromBaseUrl(server.baseUrl, ownedStore())
            val snapshot = client.refreshEntitlement("victim-install")
            val request = server.awaitRequests().single()

            assertEquals("GET", request.method)
            assertEquals("/entitlement/status?install_id=$INSTALL_ID", request.target)
            assertEquals("Bearer $BEARER", request.headers["authorization"])
            assertEquals(BillingEntitlementState.ACTIVE, snapshot?.state)
        }
    }

    @Test
    fun newAdapterReusesPersistedRegistrationForVerifyAndStatus() = runBlocking {
        val store = ownedStore()
        HttpFixture(Response(200, ENTITLEMENT), Response(200, ENTITLEMENT)).use { server ->
            HttpBillingVerificationAdapter.fromBaseUrl(server.baseUrl, store).verifyPurchase(purchaseRequest())
            HttpBillingVerificationAdapter.fromBaseUrl(server.baseUrl, store).refreshEntitlement("legacy-install")
            val requests = server.awaitRequests()

            assertEquals(listOf("POST", "GET"), requests.map { it.method })
            assertTrue(requests.all { it.headers["authorization"] == "Bearer $BEARER" })
        }
    }

    @Test
    fun statusRegistersFreshOwnershipAndRetainsClockFallback() = runBlocking {
        HttpFixture(Response(201, REGISTRATION), Response(200, "{}")).use { server ->
            val client = HttpBillingVerificationAdapter(server.baseUrl, { 42L }, MemoryBillingInstallationStore())
            val snapshot = client.refreshEntitlement("legacy-install")
            val request = server.awaitRequests().last()

            assertEquals("/entitlement/status?install_id=$INSTALL_ID", request.target)
            assertEquals("Bearer $BEARER", request.headers["authorization"])
            assertEquals(BillingEntitlementState.UNKNOWN, snapshot?.state)
            assertEquals(42L, snapshot?.checkedAtMillis)
            assertEquals(null, snapshot?.activeUntilMillis)
        }
    }

    @Test
    fun malformedRegistrationIsNotSavedOrUsedToSendAPurchase() = runBlocking {
        HttpFixture(Response(201, "{\"install_id\":\"$INSTALL_ID\",\"bearer_token\":\"\"}")).use { server ->
            val store = MemoryBillingInstallationStore()
            val error = runCatching {
                HttpBillingVerificationAdapter.fromBaseUrl(server.baseUrl, store).verifyPurchase(purchaseRequest())
            }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
            assertEquals(null, store.read())
            assertEquals("/billing/installations", server.awaitRequests().single().target)
        }
    }

    @Test
    fun backendRejectionFailsClosed() = runBlocking {
        HttpFixture(Response(401, "{}")).use { server ->
            val error = runCatching {
                HttpBillingVerificationAdapter.fromBaseUrl(server.baseUrl, ownedStore()).verifyPurchase(purchaseRequest())
            }.exceptionOrNull()

            assertEquals("Billing backend returned HTTP 401.", error?.message)
            server.awaitRequests()
            Unit
        }
    }

    @Test
    fun redirectDoesNotForwardCredentialsToAnotherOrigin() = runBlocking {
        HttpFixture(Response(302, "{}", "Location: http://127.0.0.1:1/capture\r\n")).use { server ->
            val error = runCatching {
                HttpBillingVerificationAdapter.fromBaseUrl(server.baseUrl, ownedStore()).refreshEntitlement("legacy-install")
            }.exceptionOrNull()

            assertEquals("Billing backend returned HTTP 302.", error?.message)
            server.awaitRequests()
            Unit
        }
    }

    private fun ownedStore() = MemoryBillingInstallationStore(BillingInstallation(INSTALL_ID, BEARER))

    private fun purchaseRequest() =
        BillingVerificationRequest(
            installId = "legacy-install",
            packageName = "com.shortsblockerkids",
            productId = PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID,
            purchaseToken = "token",
            appVersion = "0.1.0",
        )

    private class HttpFixture(
        vararg responses: Response,
    ) : AutoCloseable {
        private val socket = ServerSocket(0, responses.size, InetAddress.getByName("127.0.0.1"))
        private val served = CountDownLatch(responses.size)
        private val failure = AtomicReference<Throwable?>()
        private val requests = mutableListOf<RecordedRequest>()
        private val worker = thread(start = true, name = "billing-client-test") {
            try {
                responses.forEach { response ->
                    serve(response)
                    served.countDown()
                }
            } catch (throwable: Throwable) {
                if (!socket.isClosed) failure.set(throwable)
                while (served.count > 0) served.countDown()
            }
        }
        val baseUrl = "http://127.0.0.1:${socket.localPort}"

        private fun serve(response: Response) {
            socket.accept().use { client ->
                val reader = client.getInputStream().bufferedReader(Charsets.UTF_8)
                val requestLine = reader.readLine().split(" ", limit = 3)
                val headers = linkedMapOf<String, String>()
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    val separator = line.indexOf(':')
                    headers[line.substring(0, separator).lowercase()] = line.substring(separator + 1).trim()
                    line = reader.readLine()
                }
                val body = CharArray(headers["content-length"]?.toIntOrNull() ?: 0)
                var read = 0
                while (read < body.size) {
                    val count = reader.read(body, read, body.size - read)
                    if (count < 0) break
                    read += count
                }
                requests.add(RecordedRequest(requestLine[0], requestLine[1], headers, String(body, 0, read)))
                val bytes = response.body.toByteArray(Charsets.UTF_8)
                val head = "HTTP/1.1 ${response.code} Test\r\nContent-Type: application/json\r\n" +
                    "Content-Length: ${bytes.size}\r\n${response.headers}Connection: close\r\n\r\n"
                client.getOutputStream().write(head.toByteArray(Charsets.UTF_8))
                client.getOutputStream().write(bytes)
                client.getOutputStream().flush()
            }
        }

        fun awaitRequests(): List<RecordedRequest> {
            assertTrue("test server did not receive expected requests", served.await(3, TimeUnit.SECONDS))
            failure.get()?.let { throw AssertionError("test HTTP server failed", it) }
            return requests.toList()
        }

        override fun close() {
            socket.close()
            worker.join(1_000L)
        }
    }

    private data class Response(val code: Int, val body: String, val headers: String = "")

    private data class RecordedRequest(
        val method: String,
        val target: String,
        val headers: Map<String, String>,
        val body: String,
    )

    private companion object {
        const val INSTALL_ID = "fa3999d4-c2ce-4664-aec6-4d7190c0dc16"
        val BEARER = "a".repeat(43)
        val REGISTRATION = "{\"install_id\":\"$INSTALL_ID\",\"bearer_token\":\"$BEARER\"}"
        const val ENTITLEMENT = "{\"state\":\"ACTIVE\",\"checked_at_millis\":1234,\"active_until_millis\":5678}"
    }
}
