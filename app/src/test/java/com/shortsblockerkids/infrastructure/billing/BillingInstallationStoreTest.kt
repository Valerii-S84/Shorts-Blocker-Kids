package com.shortsblockerkids.infrastructure.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BillingInstallationStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun newInstallationHasNoCredentialsAndCannotTreatLegacyIdAsOwnership() {
        val store = AndroidBillingInstallationStore(temporaryFolder.newFolder())

        assertNull(store.read())
    }

    @Test
    fun credentialsAreAvailableToNewStoreInstanceAfterRestart() {
        val directory = temporaryFolder.newFolder()
        val installation = BillingInstallation(INSTALL_ID, "a".repeat(43))
        AndroidBillingInstallationStore(directory).write(installation)

        assertEquals(installation, AndroidBillingInstallationStore(directory).read())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsARegistrationThatOnlyReturnsTheOldUuid() {
        BillingInstallation(INSTALL_ID, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedServerInstallationIdentity() {
        BillingInstallation("legacy-install", "a".repeat(43))
    }

    @Test(expected = IllegalArgumentException::class)
    fun corruptedCredentialsFailClosedInsteadOfSilentlyRegisteringAnotherOwner() {
        val directory = temporaryFolder.newFolder()
        java.io.File(directory, "billing-installation.json").writeText("{}")

        AndroidBillingInstallationStore(directory).read()
    }

    private companion object {
        const val INSTALL_ID = "fa3999d4-c2ce-4664-aec6-4d7190c0dc16"
    }
}
