package com.shortsblockerkids.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.ProtectionActivationStore
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.core.billing.BillingEntitlementSnapshot
import com.shortsblockerkids.core.billing.BillingEntitlementState
import com.shortsblockerkids.core.entitlement.FreeTestPolicy
import com.shortsblockerkids.core.model.ProtectionMode
import com.shortsblockerkids.core.security.PinHasher
import com.shortsblockerkids.core.security.PinRateLimiter
import com.shortsblockerkids.core.security.PinVerificationResult
import com.shortsblockerkids.infrastructure.storage.DataStorePinStateStore
import com.shortsblockerkids.infrastructure.storage.PinPreferenceKeys
import com.shortsblockerkids.infrastructure.storage.SettingsPinAccessAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private const val SETTINGS_STORE_NAME = "shorts_blocker_settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_STORE_NAME,
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val pinHasher: PinHasher = PinHasher(),
    private val pinRateLimiter: PinRateLimiter = PinRateLimiter(),
) : ProtectionActivationStore {
    private val pinStateStore: PinStateStore = DataStorePinStateStore(dataStore)

    constructor(
        context: Context,
        pinHasher: PinHasher = PinHasher(),
        pinRateLimiter: PinRateLimiter = PinRateLimiter(),
    ) : this(
        dataStore = context.applicationContext.settingsDataStore,
        pinHasher = pinHasher,
        pinRateLimiter = pinRateLimiter,
    )

    internal fun pinStateStore(): PinStateStore = pinStateStore

    fun readSettings(): Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PROTECTION_ENABLED] = enabled
        }
    }

    override suspend fun recordSuccessfulProtectionActivation(nowMillis: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_PROTECTION_ENABLED] = true
            preferences[KEY_FREE_TEST_DURATION_DAYS] =
                preferences[KEY_FREE_TEST_DURATION_DAYS] ?: FreeTestPolicy.DEFAULT_DURATION_DAYS
            if (preferences[KEY_FREE_TEST_STARTED_AT] == null) {
                preferences[KEY_FREE_TEST_STARTED_AT] = nowMillis
            }
        }
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
            preferences[KEY_BILLING_SUBSCRIPTION_ACTIVE] = snapshot.isActive
            preferences[KEY_BILLING_ENTITLEMENT_STATE] = snapshot.state.name
            preferences[KEY_BILLING_LAST_VERIFIED_AT] = snapshot.checkedAtMillis
            preferences.setNullableLong(KEY_BILLING_ACTIVE_UNTIL_MILLIS, snapshot.activeUntilMillis)
        }
    }

    suspend fun getOrCreateBillingInstallationId(): String {
        val current = readSettings().first().billingInstallationId
        if (!current.isNullOrBlank()) {
            return current
        }
        val created = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            if (preferences[KEY_BILLING_INSTALLATION_ID].isNullOrBlank()) {
                preferences[KEY_BILLING_INSTALLATION_ID] = created
            }
        }
        return readSettings().first().billingInstallationId ?: created
    }

    suspend fun setDisclosureAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED] = accepted
        }
    }

    suspend fun setSelectedMode(mode: ProtectionMode) {
        dataStore.edit { preferences ->
            preferences[KEY_SELECTED_MODE] = mode.name
        }
    }

    suspend fun setPlatformEnabled(
        platformId: String,
        enabled: Boolean,
    ) {
        require(platformId in AppSettings.DEFAULT_ENABLED_PLATFORM_IDS) {
            "Unsupported protected platform id: $platformId"
        }
        dataStore.edit { preferences ->
            val current = preferences.enabledPlatformIds()
            preferences[KEY_ENABLED_PLATFORM_IDS] =
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

    suspend fun setTemporaryAllowUntil(allowUntilMillis: Long?) {
        dataStore.edit { preferences ->
            preferences.setNullableLong(KEY_TEMPORARY_ALLOW_UNTIL, allowUntilMillis)
        }
    }

    suspend fun setTemporaryAllowForMinutes(
        minutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        require(minutes > 0) { "Temporary allow duration must be positive." }
        setTemporaryAllowUntil(nowMillis + minutes * 60_000L)
    }

    suspend fun clearExpiredTemporaryAllow(nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            val allowUntil = preferences[KEY_TEMPORARY_ALLOW_UNTIL] ?: return@edit
            if (allowUntil <= nowMillis) {
                preferences.remove(KEY_TEMPORARY_ALLOW_UNTIL)
            }
        }
    }

    suspend fun savePin(pin: String) {
        SettingsPinAccessAdapter(
            pinStateStore = pinStateStore,
            pinHasher = pinHasher,
            pinRateLimiter = pinRateLimiter,
        ).createPin(pin)
    }

    suspend fun verifyPin(
        pin: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): PinVerificationResult =
        SettingsPinAccessAdapter(
            pinStateStore = pinStateStore,
            pinHasher = pinHasher,
            pinRateLimiter = pinRateLimiter,
            timeProvider = TimeProvider { nowMillis },
        ).verifyPin(pin)

    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            protectionEnabled = this[KEY_PROTECTION_ENABLED] ?: true,
            accessibilityDisclosureAccepted = this[KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED] ?: false,
            selectedMode = enumValueOrDefault(this[KEY_SELECTED_MODE], ProtectionMode.BLOCK_SHORTS),
            enabledPlatformIds = enabledPlatformIds(),
            temporaryAllowUntil = this[KEY_TEMPORARY_ALLOW_UNTIL],
            freeTestStartedAt = this[KEY_FREE_TEST_STARTED_AT],
            freeTestDurationDays =
                this[KEY_FREE_TEST_DURATION_DAYS] ?: FreeTestPolicy.DEFAULT_DURATION_DAYS,
            billingInstallationId = this[KEY_BILLING_INSTALLATION_ID],
            billingEntitlementState =
                enumValueOrDefault(
                    this[KEY_BILLING_ENTITLEMENT_STATE],
                    BillingEntitlementState.UNKNOWN,
                ),
            billingSubscriptionActive = this[KEY_BILLING_SUBSCRIPTION_ACTIVE] ?: false,
            billingLastVerifiedAt = this[KEY_BILLING_LAST_VERIFIED_AT],
            billingActiveUntilMillis = this[KEY_BILLING_ACTIVE_UNTIL_MILLIS],
            pinHash = this[PinPreferenceKeys.PIN_HASH],
            pinSalt = this[PinPreferenceKeys.PIN_SALT],
            pinHashVersion =
                this[PinPreferenceKeys.PIN_HASH_VERSION] ?: PinHasher.CURRENT_VERSION,
            failedPinAttempts = this[PinPreferenceKeys.FAILED_PIN_ATTEMPTS] ?: 0,
            pinLockoutUntil = this[PinPreferenceKeys.PIN_LOCKOUT_UNTIL],
        )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String?,
        default: T,
    ): T = value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

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

    private fun Preferences.enabledPlatformIds(): Set<String> =
        this[KEY_ENABLED_PLATFORM_IDS]
            ?.filterTo(mutableSetOf()) { platformId ->
                platformId in AppSettings.DEFAULT_ENABLED_PLATFORM_IDS
            }
            ?: AppSettings.DEFAULT_ENABLED_PLATFORM_IDS

    companion object {
        private val KEY_PROTECTION_ENABLED = booleanPreferencesKey("protectionEnabled")
        private val KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED =
            booleanPreferencesKey(
                "accessibilityDisclosureAccepted",
            )
        private val KEY_SELECTED_MODE = stringPreferencesKey("selectedMode")
        private val KEY_ENABLED_PLATFORM_IDS = stringSetPreferencesKey("enabledPlatformIds")
        private val KEY_TEMPORARY_ALLOW_UNTIL = longPreferencesKey("temporaryAllowUntil")
        private val KEY_FREE_TEST_STARTED_AT = longPreferencesKey("free_test_started_at")
        private val KEY_FREE_TEST_DURATION_DAYS = intPreferencesKey("free_test_duration_days")
        private val KEY_BILLING_INSTALLATION_ID = stringPreferencesKey("billing_installation_id")
        private val KEY_BILLING_ENTITLEMENT_STATE =
            stringPreferencesKey("billing_entitlement_state")
        private val KEY_BILLING_SUBSCRIPTION_ACTIVE =
            booleanPreferencesKey("billing_subscription_active")
        private val KEY_BILLING_LAST_VERIFIED_AT = longPreferencesKey("billing_last_verified_at")
        private val KEY_BILLING_ACTIVE_UNTIL_MILLIS =
            longPreferencesKey("billing_active_until_millis")
    }
}
