package com.shortsblockerkids.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shortsblockerkids.core.billing.BillingEntitlementState
import com.shortsblockerkids.core.model.ProtectionMode
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
            val repository = SettingsRepository(createDataStore("empty"))

            val settings = repository.readSettings().first()

            assertTrue(settings.protectionEnabled)
            assertFalse(settings.accessibilityDisclosureAccepted)
            assertEquals(ProtectionMode.BLOCK_SHORTS, settings.selectedMode)
            assertEquals(AppSettings.DEFAULT_ENABLED_PLATFORM_IDS, settings.enabledPlatformIds)
            assertEquals(BillingEntitlementState.UNKNOWN, settings.billingEntitlementState)
            assertFalse(settings.isPinCreated)
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
            val repository = SettingsRepository(dataStore)

            val settings = repository.readSettings().first()

            assertEquals(ProtectionMode.BLOCK_SHORTS, settings.selectedMode)
            assertEquals(BillingEntitlementState.UNKNOWN, settings.billingEntitlementState)
            assertFalse(settings.hasBillingEntitlement(nowMillis = 1_000L))
        }

    @Test
    fun corruptedBlankPinMetadataKeepsPinNotConfigured() =
        runBlocking {
            val dataStore = createDataStore("blank-pin")
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("pinHash")] = " "
                preferences[stringPreferencesKey("pinSalt")] = ""
            }
            val repository = SettingsRepository(dataStore)

            val settings = repository.readSettings().first()

            assertFalse(settings.isPinCreated)
            assertFalse(settings.canProtect(nowMillis = 1_000L))
        }

    private fun createDataStore(name: String): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, "$name.preferences_pb") },
        )
    }
}
