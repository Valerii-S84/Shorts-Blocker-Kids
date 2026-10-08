package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.HttpURLConnection
import java.net.URI

class BillingBackendSecurityTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val backends = mutableListOf<BillingBackendServer>()

    @After fun tearDown() = backends.forEach { it.stop() }

    @Test fun ownValidTokenCannotOverwriteForeignEntitlement() {
        assertForeignEntitlementProtected(SubscriptionEntitlementState.ACTIVE)
    }

    @Test fun ownExpiredTokenCannotOverwriteForeignEntitlement() {
        assertForeignEntitlementProtected(SubscriptionEntitlementState.EXPIRED)
    }

    @Test fun foreignStatusRequiresAuthorization() {
        val fixture = startBackend()
        fixture.store.upsert(victimRecord())
        val response = request(fixture, "/entitlement/status?install_id=$VICTIM_ID", expected = 401)
        assertTrue(response.contains("Installation authorization required"))
    }

    @Test fun trustedProxyClientsHaveIndependentQuotasForInvalidRequests() {
        val fixture = startBackend(trusted = setOf("127.0.0.1"), verifyLimit = 1)
        request(fixture, "/billing/play/verify", "{}", forwarded("192.0.2.1"), 400)
        request(fixture, "/billing/play/verify", "{}", forwarded("192.0.2.1"), 429)
        request(fixture, "/billing/play/verify", "{}", forwarded("192.0.2.2"), 400)
        assertEquals(0, fixture.verifier.calls)
    }

    @Test fun untrustedForwardedHeadersAreRejectedBeforeVerification() {
        val fixture = startBackend()
        val response = request(fixture, "/billing/play/verify", verifyBody(VICTIM_ID), forwarded("192.0.2.1"), 400)
        assertTrue(response.contains("proxy", ignoreCase = true))
        assertEquals(0, fixture.verifier.calls)
    }

    @Test fun directHttpCannotBypassProductionHttpsGateWithSpoofedProto() {
        val fixture = startBackend(requireHttps = true)
        request(fixture, "/billing/play/verify", verifyBody(VICTIM_ID), mapOf("X-Forwarded-Proto" to "https"), 400)
        assertEquals(0, fixture.verifier.calls)
    }

    @Test fun registeredOwnersCanVerifyAndReadTheirStatus() {
        val fixture = startBackend()
        val owner = register(fixture)
        val verified = request(fixture, "/billing/play/verify", verifyBody(owner.id), owner.headers)
        val status = request(fixture, "/entitlement/status?install_id=${owner.id}", headers = owner.headers)
        assertTrue(verified.contains("\"state\":\"ACTIVE\""))
        assertTrue(status.contains("\"state\":\"ACTIVE\""))
        assertEquals(1, fixture.verifier.calls)
    }

    @Test fun oneRegisteredOwnersBearerCannotAccessAnotherOwner() {
        val fixture = startBackend()
        val victim = register(fixture)
        val attacker = register(fixture)
        request(fixture, "/billing/play/verify", verifyBody(victim.id), victim.headers)
        request(fixture, "/billing/play/verify", verifyBody(victim.id, "attacker-token"), attacker.headers, 401)
        request(fixture, "/entitlement/status?install_id=${victim.id}", headers = attacker.headers, expected = 401)
        assertEquals(1, fixture.verifier.calls)
        assertEquals(EntitlementStore.hashPurchaseToken("victim-token"), fixture.store.findByInstallId(victim.id)?.purchaseTokenHash)
    }

    @Test fun registrationCannotClaimAnExistingInstallId() {
        val fixture = startBackend()
        fixture.store.upsert(victimRecord())
        request(fixture, "/billing/installations", "{\"install_id\":\"$VICTIM_ID\"}", expected = 400)
        request(fixture, "/entitlement/status?install_id=$VICTIM_ID", expected = 401)
        assertEquals(victimRecord(), fixture.store.findByInstallId(VICTIM_ID))
    }

    @Test fun trustedHttpsProxyAllowsAuthenticatedVerifyAndStatus() {
        val fixture = startBackend(requireHttps = true, trusted = setOf("127.0.0.1"))
        val proxy = forwarded("192.0.2.10")
        val owner = register(fixture, proxy)
        request(fixture, "/billing/play/verify", verifyBody(owner.id), proxy + owner.headers)
        val status = request(fixture, "/entitlement/status?install_id=${owner.id}", headers = proxy + owner.headers)
        assertTrue(status.contains("\"state\":\"ACTIVE\""))
    }

    @Test fun duplicateAuthorizationHeadersAreRejected() {
        val fixture = startBackend()
        val owner = register(fixture)
        val connection = connection(fixture, "/entitlement/status?install_id=${owner.id}")
        connection.addRequestProperty("Authorization", owner.headers.getValue("Authorization"))
        connection.addRequestProperty("Authorization", owner.headers.getValue("Authorization"))
        assertEquals(401, connection.responseCode)
        connection.disconnect()
    }

    @Test fun productionRtdnCannotFallBackToSharedSecret() {
        val fixture = startBackend(requireHttps = true, trusted = setOf("127.0.0.1"), productionRtdn = true)
        val response =
            request(
                fixture,
                "/billing/play/rtdn",
                "{}",
                forwarded("192.0.2.20") + mapOf(BillingBackendServer.RTDN_SECRET_HEADER to "secret"),
                400,
            )
        assertTrue(response.contains("Missing RTDN authorization"))
        assertEquals(0, fixture.verifier.calls)
    }

    private fun assertForeignEntitlementProtected(state: SubscriptionEntitlementState) {
        val fixture = startBackend(state = state)
        val original = victimRecord()
        fixture.store.upsert(original)
        request(
            fixture,
            "/billing/play/verify",
            verifyBody(VICTIM_ID, "attacker-token"),
            mapOf("Authorization" to "Bearer ${"A".repeat(43)}"),
            401,
        )
        assertEquals(original, fixture.store.findByInstallId(VICTIM_ID))
        assertEquals(0, fixture.verifier.calls)
    }

    private fun startBackend(
        state: SubscriptionEntitlementState = SubscriptionEntitlementState.ACTIVE,
        requireHttps: Boolean = false,
        trusted: Set<String> = emptySet(),
        verifyLimit: Int = 30,
        productionRtdn: Boolean = false,
    ): Fixture {
        val config =
            BackendConfig(
                port = 0,
                packageName = "com.shortsblockerkids",
                productId = "shorts_blocker_kids_monthly",
                storeFile = temporaryFolder.newFile().toPath(),
                rtdnSharedSecret = "secret",
                requireHttps = requireHttps,
                trustedProxyAddresses = trusted,
                environment = if (productionRtdn) BackendEnvironment.PRODUCTION else BackendEnvironment.DEVELOPMENT,
                rtdnPubSubAudience = if (productionRtdn) "https://billing.example/billing/play/rtdn" else null,
                rtdnPubSubServiceAccountEmail = if (productionRtdn) "pubsub@example.iam.gserviceaccount.com" else null,
                rateLimits = RateLimitConfig(verifyPerMinute = verifyLimit),
            )
        val store = EntitlementStore(requireNotNull(config.storeFile))
        val verifier = SecurityVerifier(state)
        val backend = BillingBackendServer.create(config, store, verifier, RateLimiter(nowMillis = { 1_000L }))
        backend.start()
        backends.add(backend)
        return Fixture(backend, store, verifier)
    }

    private fun register(
        fixture: Fixture,
        headers: Map<String, String> = emptyMap(),
    ): Owner {
        val response = JsonBodies.json.parseToJsonElement(request(fixture, "/billing/installations", "{}", headers)).jsonObject
        return Owner(
            response.getValue("install_id").jsonPrimitive.content,
            mapOf("Authorization" to "Bearer ${response.getValue("bearer_token").jsonPrimitive.content}"),
        )
    }

    private fun request(
        fixture: Fixture,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        expected: Int = 200,
    ): String {
        val connection = connection(fixture, path)
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        if (body != null) {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = connection.responseCode
        val response =
            (if (code < 400) connection.inputStream else connection.errorStream)
                .bufferedReader()
                .use { it.readText() }
        connection.disconnect()
        assertEquals(response, expected, code)
        return response
    }

    private fun connection(
        fixture: Fixture,
        path: String,
    ): HttpURLConnection =
        (URI("http://127.0.0.1:${fixture.backend.port}$path").toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
        }

    private fun verifyBody(
        id: String,
        token: String = "victim-token",
    ) =
        """{"install_id":"$id","package_name":"com.shortsblockerkids","product_id":"shorts_blocker_kids_monthly","purchase_token":"$token"}"""

    private fun forwarded(client: String) = mapOf("X-Forwarded-For" to client, "X-Forwarded-Proto" to "https")

    private fun victimRecord() =
        EntitlementRecord(
            installId = VICTIM_ID,
            packageName = "com.shortsblockerkids",
            productId = "shorts_blocker_kids_monthly",
            purchaseTokenHash = EntitlementStore.hashPurchaseToken("victim-token"),
            state = SubscriptionEntitlementState.ACTIVE,
            activeUntilMillis = 10_000L,
            acknowledged = true,
            lastVerifiedAtMillis = 5_000L,
            appVersion = "0.1.0",
        )

    private data class Fixture(
        val backend: BillingBackendServer,
        val store: EntitlementStorage,
        val verifier: SecurityVerifier,
    )

    private data class Owner(
        val id: String,
        val headers: Map<String, String>,
    )

    private class SecurityVerifier(
        private val state: SubscriptionEntitlementState,
    ) : PlaySubscriptionVerifier {
        var calls = 0

        override fun verify(
            packageName: String,
            productId: String,
            purchaseToken: String,
        ): VerifiedSubscription {
            calls++
            return VerifiedSubscription(
                productId = productId,
                purchaseToken = purchaseToken,
                state = state,
                activeUntilMillis = 10_000L,
                acknowledged = true,
                verifiedAtMillis = 5_000L,
            )
        }
    }

    companion object {
        private const val VICTIM_ID = "00000000-0000-4000-8000-000000000001"
    }
}
