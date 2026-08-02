package com.shortsblockerkids.core.storage

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.core.billing.BillingEntitlementState
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun storageRecordKeepsExistingDefaults() {
        val settings = AppSettings()

        assertTrue(settings.protectionEnabled)
        assertFalse(settings.accessibilityDisclosureAccepted)
        assertEquals(ProtectionMode.BLOCK_SHORTS, settings.selectedMode)
        assertEquals(
            ProtectionConfiguration.DEFAULT_ENABLED_PLATFORM_IDS,
            settings.enabledPlatformIds,
        )
        assertEquals(FreeTestPolicy.DEFAULT_DURATION_DAYS, settings.freeTestDurationDays)
        assertEquals(BillingEntitlementState.UNKNOWN, settings.billingEntitlementState)
        assertEquals(1, settings.pinHashVersion)
        assertEquals(0, settings.failedPinAttempts)
    }

    @Test
    fun pinIsConfiguredOnlyWhenHashAndSaltAreBothPresent() {
        assertTrue(AppSettings(pinHash = "hash", pinSalt = "salt").isPinCreated)
        assertFalse(AppSettings(pinHash = "", pinSalt = "salt").isPinCreated)
        assertFalse(AppSettings(pinHash = "hash", pinSalt = " ").isPinCreated)
        assertFalse(AppSettings(pinHash = null, pinSalt = "salt").isPinCreated)
        assertFalse(AppSettings(pinHash = "hash", pinSalt = null).isPinCreated)
    }

    @Test
    fun mapperExposesOnlyConsumerSafeSettings() {
        val snapshot =
            AppSettings(
                protectionEnabled = false,
                accessibilityDisclosureAccepted = true,
                temporaryAllowUntil = 2_000L,
                freeTestStartedAt = 1_000L,
                billingInstallationId = "installation-secret",
                billingEntitlementState = BillingEntitlementState.CANCELED_ACTIVE,
                billingLastVerifiedAt = 3_000L,
                billingActiveUntilMillis = 4_000L,
                pinHash = "pin-hash",
                pinSalt = "pin-salt",
                failedPinAttempts = 5,
            ).toSnapshot()

        assertFalse(snapshot.protectionConfiguration.isEnabled)
        assertTrue(snapshot.protectionConfiguration.isAccessibilityDisclosureAccepted)
        assertTrue(snapshot.protectionConfiguration.isPinConfigured)
        assertEquals(2_000L, snapshot.protectionConfiguration.temporaryAllowUntilMillis)
        assertEquals(1_000L, snapshot.entitlement.freeTestStartedAtMillis)
        assertTrue(snapshot.entitlement.isPaidProtectionAllowed)
        assertEquals(3_000L, snapshot.entitlement.paidLastVerifiedAtMillis)
        assertEquals(4_000L, snapshot.entitlement.paidActiveUntilMillis)
        assertEquals(
            BillingEntitlementState.CANCELED_ACTIVE.name,
            snapshot.billingEntitlementStateName,
        )
    }

    @Test
    fun snapshotTypeDoesNotExposeStorageOnlyMetadata() {
        val fieldNames = AppSettingsSnapshot::class.java.declaredFields.map { field -> field.name }

        listOf(
            "pinHash",
            "pinSalt",
            "pinHashVersion",
            "failedPinAttempts",
            "pinLockoutUntil",
            "billingInstallationId",
        ).forEach { forbiddenField ->
            assertFalse("snapshot exposes $forbiddenField", forbiddenField in fieldNames)
        }
    }
}
