package com.shortsblockerkids.infrastructure.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.application.protection.canProtect
import com.shortsblockerkids.application.protection.hasBillingEntitlement
import com.shortsblockerkids.core.billing.BillingEntitlementState
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageStateRecoveryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun emptyStorageUsesSafeDefaults() =
        runBlocking {
            val repository = DataStoreSettingsStore(createDataStore("empty"))

            val settings = repository.readSettings().first()

            assertTrue(settings.protectionConfiguration.isEnabled)
            assertFalse(settings.protectionConfiguration.isAccessibilityDisclosureAccepted)
            assertEquals(ProtectionMode.BLOCK_SHORTS, settings.protectionConfiguration.mode)
            assertEquals(
                ProtectionConfiguration.DEFAULT_ENABLED_PLATFORM_IDS,
                settings.protectionConfiguration.enabledPlatformIds,
            )
            assertEquals(BillingEntitlementState.UNKNOWN.name, settings.billingEntitlementStateName)
            assertFalse(settings.protectionConfiguration.isPinConfigured)
            assertFalse(settings.canProtect(nowMillis = 1_000L))
        }

    @Test
    fun corruptedEnumStorageFallsBackToSafeDefaults() =
        runBlocking {
            val dataStore = createDataStore("corrupted-enums")
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("selectedMode")] = "DELETE_ALL_APPS"
                preferences[stringPreferencesKey("billing_entitlement_state")] = "ROOTED"
            }
            val repository = DataStoreSettingsStore(dataStore)

            val settings = repository.readSettings().first()

            assertEquals(ProtectionMode.BLOCK_SHORTS, settings.protectionConfiguration.mode)
            assertEquals(BillingEntitlementState.UNKNOWN.name, settings.billingEntitlementStateName)
            assertFalse(settings.hasBillingEntitlement(nowMillis = 1_000L))
        }

    @Test
    fun corruptedBlankPinMetadataKeepsPinNotConfigured() =
        runBlocking {
            val dataStore = createDataStore("blank-pin")
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("pinHash")] = " "
                preferences[stringPreferencesKey("pinSalt")] = ""
                preferences[intPreferencesKey("failedPinAttempts")] = 5
                preferences[longPreferencesKey("pinLockoutUntil")] = 31_000L
            }
            val repository = DataStoreSettingsStore(dataStore)

            val settings = repository.readSettings().first()
            val result = repository.verifyPin("4826", nowMillis = 1_000L)
            val preferences = dataStore.data.first()

            assertFalse(settings.protectionConfiguration.isPinConfigured)
            assertFalse(settings.canProtect(nowMillis = 1_000L))
            assertEquals(PinVerificationResult.NotConfigured, result)
            assertEquals(5, preferences[intPreferencesKey("failedPinAttempts")])
            assertEquals(31_000L, preferences[longPreferencesKey("pinLockoutUntil")])
        }

    private fun createDataStore(name: String): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, "$name.preferences_pb") },
        )
    }

    private suspend fun DataStoreSettingsStore.verifyPin(
        pin: String,
        nowMillis: Long,
    ): PinVerificationResult =
        SettingsPinAccessAdapter(
            pinStateStore = this,
            timeProvider = TimeProvider { nowMillis },
        ).verifyPin(pin)
}
