package com.shortsblockerkids.infrastructure.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.PinLockoutClock
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.ProtectionActivationOperation
import com.shortsblockerkids.application.port.ProtectionActivationStore
import com.shortsblockerkids.application.port.SettingsStatePort
import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionMode
import com.shortsblockerkids.infrastructure.time.AndroidPinLockoutClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal const val SETTINGS_STORE_NAME = "shorts_blocker_settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_STORE_NAME,
)

class DataStoreSettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val deadlineClock: PinLockoutClock? = null,
) : ProtectionActivationStore,
    TemporaryAllowStore,
    SettingsStatePort,
    PinStateStore by DataStorePinStateStore(dataStore) {
    constructor(
        context: Context,
        deadlineClock: PinLockoutClock = AndroidPinLockoutClock(context),
    ) : this(
        dataStore = context.applicationContext.settingsDataStore,
        deadlineClock = deadlineClock,
    )

    override fun readSettings(): Flow<AppSettingsSnapshot> =
        readStoredSettings().map {
            DataStoreSettingsMapper.toSnapshot(it, deadlineClock?.read()?.bootCount)
        }

    internal fun readStoredSettings(): Flow<StoredAppSettings> = dataStore.data.map(DataStoreSettingsMapper::toStoredAppSettings)

    suspend fun setProtectionEnabled(enabled: Boolean) {
        editSettings { preferences ->
            preferences[DataStoreSettingsMapper.protectionEnabledKey] = enabled
        }
    }

    override suspend fun completeProtectionActivation(
        decision: (AppSettingsSnapshot) -> ProtectionActivationOperation,
    ): ProtectionActivationOperation {
        var operation: ProtectionActivationOperation =
            ProtectionActivationOperation.PrerequisitesNotMet
        editSettings { preferences ->
            operation =
                decision(
                    DataStoreSettingsMapper.toSnapshot(
                        DataStoreSettingsMapper.toStoredAppSettings(preferences),
                        deadlineClock?.read()?.bootCount,
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

    suspend fun updateBillingEntitlement(isActive: Boolean) {
        updateBillingEntitlement(BillingEntitlementSnapshot(isActive = isActive))
    }

    suspend fun updateBillingEntitlement(snapshot: BillingEntitlementSnapshot) {
        editSettings { preferences ->
            preferences[DataStoreSettingsMapper.playSubscriptionActiveKey] = snapshot.isActive
        }
    }

    suspend fun setDisclosureAccepted(accepted: Boolean) {
        editSettings { preferences ->
            preferences[DataStoreSettingsMapper.accessibilityDisclosureAcceptedKey] = accepted
        }
    }

    suspend fun setSelectedMode(mode: ProtectionMode) {
        editSettings { preferences ->
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
        editSettings { preferences ->
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
        editSettings { preferences ->
            preferences.remove(DataStoreSettingsMapper.temporaryAllowElapsedRealtimeDeadlineKey)
            preferences.remove(DataStoreSettingsMapper.temporaryAllowBootCountKey)
            preferences.setNullableLong(
                DataStoreSettingsMapper.temporaryAllowUntilKey,
                allowUntilMillis,
            )
        }
    }

    override suspend fun removeTemporaryAllowIf(shouldRemove: (Long) -> Boolean): Boolean =
        removeTemporaryAllowDeadlineIf { wallUntil, _, _ -> wallUntil != null && shouldRemove(wallUntil) }

    override suspend fun setTemporaryAllowDeadline(
        allowUntilMillis: Long,
        elapsedRealtimeDeadlineMillis: Long,
        bootCount: Int,
    ) {
        editSettings { preferences ->
            preferences[DataStoreSettingsMapper.temporaryAllowUntilKey] = allowUntilMillis
            preferences[DataStoreSettingsMapper.temporaryAllowElapsedRealtimeDeadlineKey] = elapsedRealtimeDeadlineMillis
            preferences[DataStoreSettingsMapper.temporaryAllowBootCountKey] = bootCount
        }
    }

    override suspend fun removeTemporaryAllowDeadlineIf(shouldRemove: (Long?, Long?, Int?) -> Boolean): Boolean {
        var removed = false
        editSettings { preferences ->
            val wallUntil = preferences[DataStoreSettingsMapper.temporaryAllowUntilKey]
            val elapsedUntil = preferences[DataStoreSettingsMapper.temporaryAllowElapsedRealtimeDeadlineKey]
            val bootCount = preferences[DataStoreSettingsMapper.temporaryAllowBootCountKey]
            if ((wallUntil != null || elapsedUntil != null) && shouldRemove(wallUntil, elapsedUntil, bootCount)) {
                preferences.remove(DataStoreSettingsMapper.temporaryAllowUntilKey)
                preferences.remove(DataStoreSettingsMapper.temporaryAllowElapsedRealtimeDeadlineKey)
                preferences.remove(DataStoreSettingsMapper.temporaryAllowBootCountKey)
                removed = true
            }
        }
        return removed
    }

    private suspend fun editSettings(transform: suspend (MutablePreferences) -> Unit) {
        dataStore.edit(transform)
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
