package com.shortsblockerkids.infrastructure.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.ProtectionActivationOperation
import com.shortsblockerkids.application.port.ProtectionActivationStore
import com.shortsblockerkids.application.port.SettingsStatePort
import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

internal const val SETTINGS_STORE_NAME = "shorts_blocker_settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_STORE_NAME,
)

class DataStoreSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : ProtectionActivationStore,
    TemporaryAllowStore,
    SettingsStatePort,
    PinStateStore by DataStorePinStateStore(dataStore) {
    constructor(context: Context) : this(
        dataStore = context.applicationContext.settingsDataStore,
    )

    override fun readSettings(): Flow<AppSettingsSnapshot> = readStoredSettings().map(DataStoreSettingsMapper::toSnapshot)

    internal fun readStoredSettings(): Flow<StoredAppSettings> = dataStore.data.map(DataStoreSettingsMapper::toStoredAppSettings)

    suspend fun setProtectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DataStoreSettingsMapper.protectionEnabledKey] = enabled
        }
    }

    override suspend fun completeProtectionActivation(
        decision: (AppSettingsSnapshot) -> ProtectionActivationOperation,
    ): ProtectionActivationOperation {
        var operation: ProtectionActivationOperation =
            ProtectionActivationOperation.PrerequisitesNotMet
        dataStore.edit { preferences ->
            operation =
                decision(
                    DataStoreSettingsMapper.toSnapshot(
                        DataStoreSettingsMapper.toStoredAppSettings(preferences),
                    ),
                )
            when (val currentOperation = operation) {
                is ProtectionActivationOperation.Record -> {
                    preferences[DataStoreSettingsMapper.protectionEnabledKey] = true
                    preferences[DataStoreSettingsMapper.freeTestDurationDaysKey] =
                        preferences[DataStoreSettingsMapper.freeTestDurationDaysKey]
                            ?: FreeTestPolicy.DEFAULT_DURATION_DAYS
                    if (preferences[DataStoreSettingsMapper.freeTestStartedAtKey] == null) {
                        preferences[DataStoreSettingsMapper.freeTestStartedAtKey] =
                            currentOperation.nowMillis
                    }
                }

                ProtectionActivationOperation.AlreadyStarted ->
                    preferences[DataStoreSettingsMapper.protectionEnabledKey] = true

                ProtectionActivationOperation.PrerequisitesNotMet -> Unit
            }
        }
        return operation
    }

    suspend fun updateBillingEntitlement(
        isActive: Boolean,
        checkedAtMillis: Long = System.currentTimeMillis(),
    ) {
        updateBillingEntitlement(
            BillingEntitlementSnapshot(
                isActive = isActive,
                checkedAtMillis = checkedAtMillis,
            ),
        )
    }

    suspend fun updateBillingEntitlement(snapshot: BillingEntitlementSnapshot) {
        dataStore.edit { preferences ->
            preferences[DataStoreSettingsMapper.billingSubscriptionActiveKey] = snapshot.isActive
            preferences[DataStoreSettingsMapper.billingEntitlementStateKey] = snapshot.state.name
            preferences[DataStoreSettingsMapper.billingLastVerifiedAtKey] = snapshot.checkedAtMillis
            preferences.setNullableLong(
                DataStoreSettingsMapper.billingActiveUntilMillisKey,
                snapshot.activeUntilMillis,
            )
        }
    }

    suspend fun getOrCreateBillingInstallationId(): String {
        val current = readStoredSettings().first().billingInstallationId
        if (!current.isNullOrBlank()) {
            return current
        }
        val created = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            if (preferences[DataStoreSettingsMapper.billingInstallationIdKey].isNullOrBlank()) {
                preferences[DataStoreSettingsMapper.billingInstallationIdKey] = created
            }
        }
        return readStoredSettings().first().billingInstallationId ?: created
    }

    suspend fun setDisclosureAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[DataStoreSettingsMapper.accessibilityDisclosureAcceptedKey] = accepted
        }
    }

    suspend fun setSelectedMode(mode: ProtectionMode) {
        dataStore.edit { preferences ->
            preferences[DataStoreSettingsMapper.selectedModeKey] = mode.name
        }
    }

    suspend fun setPlatformEnabled(
        platformId: String,
        enabled: Boolean,
    ) {
        require(platformId in ProtectionConfiguration.DEFAULT_ENABLED_PLATFORM_IDS) {
            "Unsupported protected platform id: $platformId"
        }
        dataStore.edit { preferences ->
            val current = DataStoreSettingsMapper.enabledPlatformIds(preferences)
            preferences[DataStoreSettingsMapper.enabledPlatformIdsKey] =
                if (enabled) {
                    current + platformId
                } else {
                    current - platformId
                }
        }
    }

    suspend fun acceptAccessibilityDisclosure() {
        setDisclosureAccepted(true)
    }

    override suspend fun setTemporaryAllowUntil(allowUntilMillis: Long?) {
        dataStore.edit { preferences ->
            preferences.setNullableLong(
                DataStoreSettingsMapper.temporaryAllowUntilKey,
                allowUntilMillis,
            )
        }
    }

    override suspend fun removeTemporaryAllowIf(shouldRemove: (Long) -> Boolean): Boolean {
        var removed = false
        dataStore.edit { preferences ->
            val allowUntil =
                preferences[DataStoreSettingsMapper.temporaryAllowUntilKey] ?: return@edit
            if (shouldRemove(allowUntil)) {
                preferences.remove(DataStoreSettingsMapper.temporaryAllowUntilKey)
                removed = true
            }
        }
        return removed
    }

    private fun MutablePreferences.setNullableLong(
        key: Preferences.Key<Long>,
        value: Long?,
    ) {
        if (value == null) {
            remove(key)
        } else {
            this[key] = value
        }
    }
}
