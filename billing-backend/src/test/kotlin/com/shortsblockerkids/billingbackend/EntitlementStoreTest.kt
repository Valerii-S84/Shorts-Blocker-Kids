package com.shortsblockerkids.billingbackend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
}
