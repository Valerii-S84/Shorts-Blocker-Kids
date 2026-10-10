package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LegacyInstallationMigrationTest {
    private val backends = mutableListOf<BillingBackendServer>()

    @After
    fun tearDown() = backends.forEach { it.stop() }

    @Test
    fun legacyPaidTokenCannotBeRestoredByNewInstallationEvenRepeatedly() {
        val fixture = startBackend()
        val token = uniqueToken()
        val legacy = record(UUID.randomUUID().toString(), token)
        fixture.store.upsert(legacy)
        val claimant = register(fixture)

        repeat(2) {
            val response = request(fixture, "/billing/play/verify", body(claimant.installId, token), claimant, 400)
            assertTrue(response.contains("Purchase token already belongs to another installation"))
        }

        assertEquals(legacy, fixture.store.findByInstallId(legacy.installId))
        assertNull(fixture.store.installationBearerHash(legacy.installId))
        assertUnchangedClaimant(fixture, claimant)
        assertEquals(0, fixture.verifier.calls.get())
    }

    @Test
    fun legacyUuidAndNewInstallationsBearerCannotAuthorizeLegacyMutationOrRead() {
        val fixture = startBackend()
        val token = uniqueToken()
        val legacy = record(UUID.randomUUID().toString(), token)
        fixture.store.upsert(legacy)
        val claimant = register(fixture)

        request(fixture, "/billing/play/verify", body(legacy.installId, token), claimant, 401)
        request(fixture, "/entitlement/status?install_id=${legacy.installId}", owner = claimant, expected = 401)

        assertEquals(legacy, fixture.store.findByInstallId(legacy.installId))
        assertNull(fixture.store.installationBearerHash(legacy.installId))
        assertUnchangedClaimant(fixture, claimant)
        assertEquals(0, fixture.verifier.calls.get())
    }

    @Test
    fun currentAuthorizedOwnerCanRestoreButForeignInstallationCannotTakeToken() {
        val fixture = startBackend()
        val owner = register(fixture)
        val foreign = register(fixture)
        val token = uniqueToken()
        fixture.store.upsert(record(owner.installId, token))
        val originalAuthorization = fixture.store.installationBearerHash(owner.installId)

        repeat(2) {
            val response = request(fixture, "/billing/play/verify", body(owner.installId, token), owner)
            assertTrue(response.contains("\"state\":\"ACTIVE\""))
        }
        val restored = fixture.store.findByInstallId(owner.installId)
        request(fixture, "/billing/play/verify", body(owner.installId, token), foreign, 401)
        request(fixture, "/billing/play/verify", body(foreign.installId, token), foreign, 400)

        assertEquals(restored, fixture.store.findByInstallId(owner.installId))
        assertEquals(originalAuthorization, fixture.store.installationBearerHash(owner.installId))
        assertUnchangedClaimant(fixture, foreign)
        assertEquals(2, fixture.verifier.calls.get())
    }

    @Test
    fun concurrentLegacyRestoreAttemptsAcrossServersLeaveOriginalOwnershipUntouched() {
        val first = startBackend()
        val second = startBackend()
        val token = uniqueToken()
        val legacy = record(UUID.randomUUID().toString(), token)
        first.store.upsert(legacy)
        val firstClaimant = register(first)
        val secondClaimant = register(second)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val requests = listOf(first to firstClaimant, second to secondClaimant).map { (fixture, claimant) ->
                executor.submit<String> {
                    ready.countDown()
                    check(start.await(5, TimeUnit.SECONDS))
                    request(fixture, "/billing/play/verify", body(claimant.installId, token), claimant, 400)
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            requests.forEach { assertTrue(it.get(10, TimeUnit.SECONDS).contains("already belongs")) }
        } finally {
            start.countDown()
            executor.shutdownNow()
        }

        assertEquals(legacy, first.store.findByInstallId(legacy.installId))
        assertEquals(legacy, second.store.findByInstallId(legacy.installId))
        assertNull(first.store.installationBearerHash(legacy.installId))
        assertUnchangedClaimant(first, firstClaimant)
        assertUnchangedClaimant(second, secondClaimant)
        assertEquals(0, first.verifier.calls.get() + second.verifier.calls.get())
    }

    @Test
    fun invalidTokenAndUnavailableVerificationLeaveNoEntitlementOrAuthorizationDrift() {
        listOf(InvalidPurchaseTokenException() to 400, BillingBackendUnavailableException("Test failure") to 503)
            .forEach { (failure, status) ->
                val fixture = startBackend(verificationFailure = failure)
                val claimant = register(fixture)

                request(fixture, "/billing/play/verify", body(claimant.installId, uniqueToken()), claimant, status)

                assertUnchangedClaimant(fixture, claimant)
                assertEquals(1, fixture.verifier.calls.get())
            }
    }

    @Test
    fun storageFailureAfterVerificationLeavesLegacyEntitlementAndAuthorizationsUntouched() {
        val fixture = startBackend(failEntitlementWrite = true)
        val legacy = record(UUID.randomUUID().toString(), uniqueToken())
        fixture.store.upsert(legacy)
        val claimant = register(fixture)

        request(fixture, "/billing/play/verify", body(claimant.installId, uniqueToken()), claimant, 503)

        assertEquals(legacy, fixture.store.findByInstallId(legacy.installId))
        assertNull(fixture.store.installationBearerHash(legacy.installId))
        assertUnchangedClaimant(fixture, claimant)
        assertEquals(1, fixture.verifier.calls.get())
    }

    private fun startBackend(
        verificationFailure: RuntimeException? = null,
        failEntitlementWrite: Boolean = false,
    ): Fixture {
        val url = System.getenv("SBK_TEST_POSTGRES_URL")
        assumeTrue("Set SBK_TEST_POSTGRES_URL to an isolated regression database", !url.isNullOrBlank())
        val database = DatabaseConfig(requireNotNull(url), null, null)
        DatabaseMigrator(database, Path.of("migrations")).applyMigrations()
        val store = PostgresEntitlementStore(database)
        val endpointStore =
            if (failEntitlementWrite) {
                object : EntitlementStorage by store {
                    override fun upsert(record: EntitlementRecord) = throw IllegalStateException("Test storage failure")
                }
            } else {
                store
            }
        val verifier = RecordingVerifier(verificationFailure)
        val config = BackendConfig(bindAddress = "127.0.0.1", port = 0, database = database)
        val backend = BillingBackendServer.create(config, endpointStore, verifier)
        backend.start()
        backends.add(backend)
        return Fixture(backend, store, verifier)
    }

    private fun register(fixture: Fixture): Owner {
        val response = JsonBodies.json.parseToJsonElement(request(fixture, "/billing/installations", "{}")).jsonObject
        val installId = response.getValue("install_id").jsonPrimitive.content
        return Owner(
            installId,
            "Bearer ${response.getValue("bearer_token").jsonPrimitive.content}",
            requireNotNull(fixture.store.installationBearerHash(installId)),
        )
    }

    private fun assertUnchangedClaimant(fixture: Fixture, claimant: Owner) {
        assertNull(fixture.store.findByInstallId(claimant.installId))
        assertEquals(claimant.originalBearerHash, fixture.store.installationBearerHash(claimant.installId))
        assertTrue(InstallationAuthorization(fixture.store).authenticate(claimant.installId, claimant.authorization))
    }

    private fun request(
        fixture: Fixture,
        path: String,
        body: String? = null,
        owner: Owner? = null,
        expected: Int = 200,
    ): String {
        val connection = URI("http://127.0.0.1:${fixture.backend.port}$path").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        owner?.let { connection.setRequestProperty("Authorization", it.authorization) }
        try {
            if (body != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val status = connection.responseCode
            val response = (if (status < 400) connection.inputStream else connection.errorStream)
                .bufferedReader().use { it.readText() }
            assertEquals(response, expected, status)
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun body(installId: String, token: String): String =
        """{"install_id":"$installId","package_name":"com.shortsblockerkids","product_id":"shorts_blocker_kids_monthly","purchase_token":"$token"}"""

    private fun uniqueToken(): String = "legacy-regression-${UUID.randomUUID()}"

    private fun record(installId: String, token: String): EntitlementRecord =
        EntitlementRecord(
            installId = installId,
            packageName = "com.shortsblockerkids",
            productId = "shorts_blocker_kids_monthly",
            purchaseTokenHash = EntitlementStore.hashPurchaseToken(token),
            state = SubscriptionEntitlementState.ACTIVE,
            activeUntilMillis = 10_000L,
            acknowledged = true,
            lastVerifiedAtMillis = 5_000L,
            appVersion = "legacy-test",
        )

    private data class Fixture(
        val backend: BillingBackendServer,
        val store: PostgresEntitlementStore,
        val verifier: RecordingVerifier,
    )

    private data class Owner(
        val installId: String,
        val authorization: String,
        val originalBearerHash: String,
    )

    private class RecordingVerifier(private val failure: RuntimeException?) : PlaySubscriptionVerifier {
        val calls = AtomicInteger()

        override fun verify(packageName: String, productId: String, purchaseToken: String): VerifiedSubscription {
            calls.incrementAndGet()
            failure?.let { throw it }
            return VerifiedSubscription(
                productId = productId,
                purchaseToken = purchaseToken,
                state = SubscriptionEntitlementState.ACTIVE,
                activeUntilMillis = 10_000L,
                acknowledged = true,
                verifiedAtMillis = 6_000L,
            )
        }
    }
}
