package com.shortsblockerkids.infrastructure.storage

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredAppSettingsTest {
    @Test
    fun storageRecordKeepsExistingDefaults() {
        val settings = StoredAppSettings()

        assertTrue(settings.protectionEnabled)
        assertFalse(settings.accessibilityDisclosureAccepted)
        assertEquals(ProtectionMode.BLOCK_SHORTS, settings.selectedMode)
        assertEquals(
            ProtectionConfiguration.DEFAULT_ENABLED_PLATFORM_IDS,
            settings.enabledPlatformIds,
        )
        assertEquals(FreeTestPolicy.DEFAULT_DURATION_DAYS, settings.freeTestDurationDays)
        assertFalse(settings.billingSubscriptionActive)
        assertEquals(1, settings.pinHashVersion)
        assertEquals(0, settings.failedPinAttempts)
    }

    @Test
    fun pinIsConfiguredOnlyWhenHashAndSaltAreBothPresent() {
        assertTrue(StoredAppSettings(pinHash = "hash", pinSalt = "salt").isPinCreated)
        assertFalse(StoredAppSettings(pinHash = "", pinSalt = "salt").isPinCreated)
        assertFalse(StoredAppSettings(pinHash = "hash", pinSalt = " ").isPinCreated)
        assertFalse(StoredAppSettings(pinHash = null, pinSalt = "salt").isPinCreated)
        assertFalse(StoredAppSettings(pinHash = "hash", pinSalt = null).isPinCreated)
    }

    @Test
    fun mapperExposesOnlyConsumerSafeSettings() {
        val snapshot =
            DataStoreSettingsMapper.toSnapshot(
                StoredAppSettings(
                    protectionEnabled = false,
                    accessibilityDisclosureAccepted = true,
                    temporaryAllowUntil = 2_000L,
                    freeTestStartedAt = 1_000L,
                    billingSubscriptionActive = true,
                    pinHash = "pin-hash",
                    pinSalt = "pin-salt",
                    failedPinAttempts = 5,
                ),
            )

        assertFalse(snapshot.protectionConfiguration.isEnabled)
        assertTrue(snapshot.protectionConfiguration.isAccessibilityDisclosureAccepted)
        assertTrue(snapshot.protectionConfiguration.isPinConfigured)
        assertEquals(2_000L, snapshot.protectionConfiguration.temporaryAllowUntilMillis)
        assertEquals(1_000L, snapshot.entitlement.freeTestStartedAtMillis)
        assertTrue(snapshot.entitlement.isPaidProtectionAllowed)
        assertEquals("ACTIVE", snapshot.billingEntitlementStateName)
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
