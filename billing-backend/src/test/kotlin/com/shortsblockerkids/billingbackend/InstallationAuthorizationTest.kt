package com.shortsblockerkids.billingbackend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InstallationAuthorizationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun registersIndependentIdentitiesAndOnlyAuthenticatesTheMatchingOwner() {
        val authorization = InstallationAuthorization(newStore())
        val first = authorization.register()
        val second = authorization.register()

        assertNotEquals(first.installId, second.installId)
        assertNotEquals(first.bearerToken, second.bearerToken)
        assertEquals(43, first.bearerToken.length)
        assertTrue(authorization.authenticate(first.installId, "Bearer ${first.bearerToken}"))
        assertTrue(authorization.authenticate(second.installId, "Bearer ${second.bearerToken}"))
        assertFalse(authorization.authenticate(first.installId, "Bearer ${second.bearerToken}"))
    }

    @Test
    fun uuidOrPurchaseTokenDoesNotAuthenticateInstallation() {
        val authorization = InstallationAuthorization(newStore())
        val owner = authorization.register()

        assertFalse(authorization.authenticate(owner.installId, null))
        assertFalse(authorization.authenticate(owner.installId, owner.installId))
        assertFalse(authorization.authenticate(owner.installId, "Bearer valid-purchase-token"))
        assertFalse(authorization.authenticate(owner.installId, "Bearer expired-purchase-token"))
    }

    @Test
    fun ownershipPersistsAcrossRestartWithoutPersistingRawBearer() {
        val path = temporaryFolder.newFile("restart.json").toPath()
        val owner = InstallationAuthorization(EntitlementStore(path)).register()
        val restarted = InstallationAuthorization(EntitlementStore(path))

        assertTrue(restarted.authenticate(owner.installId, "Bearer ${owner.bearerToken}"))
        assertFalse(path.toFile().readText().contains(owner.bearerToken))
    }

    @Test
    fun legacyEntitlementCannotBeClaimedThroughRegistrationStorage() {
        val store = newStore()
        store.upsert(legacyRecord())

        assertFalse(store.createInstallation("legacy-install", "a".repeat(64)))
        assertEquals(null, store.installationBearerHash("legacy-install"))
        assertFalse(InstallationAuthorization(store).authenticate("legacy-install", "Bearer ${"a".repeat(43)}"))
        assertEquals(SubscriptionEntitlementState.ACTIVE, store.findByInstallId("legacy-install")?.state)
    }

    @Test
    fun existingOwnershipCannotBeReplaced() {
        val store = newStore()

        assertTrue(store.createInstallation("new-install", "a".repeat(64)))
        assertFalse(store.createInstallation("new-install", "b".repeat(64)))
        assertEquals("a".repeat(64), store.installationBearerHash("new-install"))
    }

    @Test
    fun legacyEntitlementRemainsUnclaimableAfterStorageRestart() {
        val path = temporaryFolder.newFile("legacy.json").toPath()
        EntitlementStore(path).upsert(legacyRecord())
        val restarted = EntitlementStore(path)

        assertFalse(restarted.createInstallation("legacy-install", "a".repeat(64)))
        assertEquals(null, restarted.installationBearerHash("legacy-install"))
    }

    @Test
    fun rejectsMalformedAndCombinedAuthorizationHeaders() {
        val authorization = InstallationAuthorization(newStore())
        val owner = authorization.register()
        val header = "Bearer ${owner.bearerToken}"

        assertFalse(authorization.authenticate(owner.installId, "$header, $header"))
        assertFalse(authorization.authenticate(owner.installId, "$header "))
        assertFalse(authorization.authenticate(owner.installId, "Basic ${owner.bearerToken}"))
    }

    private fun newStore() = EntitlementStore(temporaryFolder.newFile().toPath())

    private fun legacyRecord() =
        EntitlementRecord(
            installId = "legacy-install",
            packageName = "com.shortsblockerkids",
            productId = "shorts_blocker_kids_monthly",
            purchaseTokenHash = EntitlementStore.hashPurchaseToken("victim-purchase-token"),
            state = SubscriptionEntitlementState.ACTIVE,
            activeUntilMillis = 10_000L,
            acknowledged = true,
            lastVerifiedAtMillis = 5_000L,
            appVersion = "0.1.0",
        )
}
