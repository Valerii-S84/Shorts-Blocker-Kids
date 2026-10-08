package com.shortsblockerkids.billingbackend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class EntitlementStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun persistsEntitlementWithoutRawPurchaseToken() {
        val storeFile = temporaryFolder.newFile("store.json").toPath()
        val token = "purchase-token"
        val store = EntitlementStore(storeFile)

        store.upsert(
            EntitlementRecord(
                installId = "install-1",
                packageName = "com.shortsblockerkids",
                productId = "shorts_blocker_kids_monthly",
                purchaseTokenHash = EntitlementStore.hashPurchaseToken(token),
                state = SubscriptionEntitlementState.ACTIVE,
                activeUntilMillis = 10_000L,
                acknowledged = true,
                lastVerifiedAtMillis = 5_000L,
                appVersion = "0.1.0",
            ),
        )

        val reloaded = EntitlementStore(storeFile)
        val record = reloaded.findByInstallId("install-1")

        assertEquals(SubscriptionEntitlementState.ACTIVE, record?.state)
        assertEquals(10_000L, record?.activeUntilMillis)
        assertFalse(storeFile.toFile().readText().contains(token))
    }

    @Test
    fun findsExistingEntitlementByPurchaseTokenHashOnly() {
        val store = EntitlementStore(temporaryFolder.newFile("token-hash.json").toPath())
        val token = "purchase-token"
        val tokenHash = EntitlementStore.hashPurchaseToken(token)

        store.upsert(
            EntitlementRecord(
                installId = "install-1",
                packageName = "com.shortsblockerkids",
                productId = "shorts_blocker_kids_monthly",
                purchaseTokenHash = tokenHash,
                state = SubscriptionEntitlementState.ACTIVE,
                activeUntilMillis = 10_000L,
                acknowledged = true,
                lastVerifiedAtMillis = 5_000L,
                appVersion = "0.1.0",
            ),
        )

        assertEquals("install-1", store.findByPurchaseTokenHash(tokenHash)?.installId)
        assertTrue(store.findByPurchaseTokenHash(token).let { it == null })
    }

    @Test
    fun tracksRtdnMessagesIdempotently() {
        val store = EntitlementStore(temporaryFolder.newFile("rtdn.json").toPath())

        assertFalse(store.isRtdnProcessed("msg-1"))
        assertTrue(store.markRtdnProcessed("msg-1"))
        assertTrue(store.isRtdnProcessed("msg-1"))
        assertFalse(store.markRtdnProcessed("msg-1"))
    }

    @Test
    fun rtdnPersistsEntitlementAndMarkerTogetherAndSkipsDuplicates() {
        val path = temporaryFolder.newFile("atomic-rtdn.json").toPath()
        val store = EntitlementStore(path)
        store.upsert(record())

        assertEquals(RtdnApplyResult.UPDATED, store.applyRtdn("message", "token", verified()))
        val saved = Files.readString(path)
        assertEquals(
            RtdnApplyResult.DUPLICATE,
            store.applyRtdn("message", "token", verified().copy(state = SubscriptionEntitlementState.ACTIVE)),
        )
        assertEquals(saved, Files.readString(path))
        val reloaded = EntitlementStore(path)
        assertTrue(reloaded.isRtdnProcessed("message"))
        assertEquals(SubscriptionEntitlementState.EXPIRED, reloaded.findByInstallId("install")?.state)
    }

    @Test
    fun rtdnWithoutInstallStillPersistsDeduplicationMarker() {
        val path = temporaryFolder.newFile("no-install.json").toPath()
        val store = EntitlementStore(path)

        assertEquals(RtdnApplyResult.NO_INSTALL_RECORD, store.applyRtdn("message", "token", verified()))
        assertTrue(EntitlementStore(path).isRtdnProcessed("message"))
    }

    @Test
    fun failedReplacementPreservesMemoryDiskAndAuthorizationForEveryMutation() {
        val path = temporaryFolder.newFile("failed-replace.json").toPath()
        val initial = EntitlementStore(path)
        initial.upsert(record())
        initial.createInstallation("authorized", "original-hash")
        val saved = Files.readString(path)
        var replacementAttempts = 0
        val store =
            EntitlementStore(path) { temporary, target ->
                assertEquals(path.toAbsolutePath(), target)
                assertEquals(target.parent, temporary.parent)
                assertTrue(Files.isRegularFile(temporary))
                replacementAttempts++
                throw IOException("Injected replacement failure")
            }
        val mutations = failingMutations(store)
        mutations.forEach { mutation ->
            assertThrows(IOException::class.java) { mutation() }
            assertEquals(saved, Files.readString(path))
            assertEquals(record(), store.findByInstallId("install"))
            assertEquals("original-hash", store.installationBearerHash("authorized"))
            assertEquals(null, store.installationBearerHash("new-install"))
            assertFalse(store.isRtdnProcessed("message"))
            Files.list(path.parent).use { files ->
                assertFalse(files.anyMatch { it.fileName.toString().endsWith(".tmp") })
            }
        }
        assertEquals(mutations.size, replacementAttempts)
        val reloaded = EntitlementStore(path)
        assertEquals(RtdnApplyResult.UPDATED, reloaded.applyRtdn("message", "token", verified()))
        assertTrue(EntitlementStore(path).isRtdnProcessed("message"))
    }

    @Test
    fun replacementReceivesCompleteSnapshotBeforeMemoryChanges() {
        val path = temporaryFolder.newFile("replace-snapshot.json").toPath()
        lateinit var store: EntitlementStore
        store =
            EntitlementStore(path) { temporary, target ->
                assertEquals(null, store.findByInstallId("install"))
                assertEquals(record(), EntitlementStore(temporary).findByInstallId("install"))
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                Unit
            }

        store.upsert(record())

        assertEquals(record(), store.findByInstallId("install"))
        assertEquals(record(), EntitlementStore(path).findByInstallId("install"))
    }

    @Test
    fun malformedStoreFailsClosedAndPreservesOriginalBytes() {
        val path = temporaryFolder.newFile("malformed.json").toPath()
        listOf(
            "{\"installation_authorizations\":",
            """{"installation_authorizations":{"install":"hash"},"entitlements":[{}]}""",
            """{"installation_authorizations":[]}""",
        ).forEach { malformed ->
            Files.writeString(path, malformed)

            val failure = assertThrows(IllegalStateException::class.java) { EntitlementStore(path) }

            assertTrue(requireNotNull(failure.message).contains("original file preserved"))
            assertTrue(requireNotNull(failure.message).contains(path.toString()))
            assertEquals(malformed, Files.readString(path))
        }
    }

    private fun failingMutations(store: EntitlementStore): List<() -> Unit> =
        listOf(
            {
                store.createInstallation("new-install", "new-hash")
                Unit
            },
            { store.upsert(record().copy(state = SubscriptionEntitlementState.EXPIRED)) },
            {
                store.markRtdnProcessed("message")
                Unit
            },
            {
                store.updateByPurchaseToken("token", verified())
                Unit
            },
            {
                store.applyRtdn("message", "token", verified())
                Unit
            },
        )

    private fun record(): EntitlementRecord =
        EntitlementRecord(
            installId = "install",
            packageName = "com.shortsblockerkids",
            productId = "monthly",
            purchaseTokenHash = EntitlementStore.hashPurchaseToken("token"),
            state = SubscriptionEntitlementState.ACTIVE,
            activeUntilMillis = 10_000L,
            acknowledged = true,
            lastVerifiedAtMillis = 5_000L,
            appVersion = null,
        )

    private fun verified(): VerifiedSubscription =
        VerifiedSubscription(
            productId = "monthly",
            purchaseToken = "token",
            state = SubscriptionEntitlementState.EXPIRED,
            activeUntilMillis = null,
            acknowledged = true,
            verifiedAtMillis = 6_000L,
        )
}
