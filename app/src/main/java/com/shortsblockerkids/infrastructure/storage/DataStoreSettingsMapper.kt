package com.shortsblockerkids.infrastructure.storage

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.PinHashingPort
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.entitlement.allowsPaidProtection
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionMode

internal object DataStoreSettingsMapper {
    val protectionEnabledKey = booleanPreferencesKey("protectionEnabled")
    val accessibilityDisclosureAcceptedKey =
        booleanPreferencesKey("accessibilityDisclosureAccepted")
    val selectedModeKey = stringPreferencesKey("selectedMode")
    val enabledPlatformIdsKey = stringSetPreferencesKey("enabledPlatformIds")
    val temporaryAllowUntilKey = longPreferencesKey("temporaryAllowUntil")
    val freeTestStartedAtKey = longPreferencesKey("free_test_started_at")
    val freeTestDurationDaysKey = intPreferencesKey("free_test_duration_days")
    val billingInstallationIdKey = stringPreferencesKey("billing_installation_id")
    val billingEntitlementStateKey = stringPreferencesKey("billing_entitlement_state")
    val billingSubscriptionActiveKey = booleanPreferencesKey("billing_subscription_active")
    val billingLastVerifiedAtKey = longPreferencesKey("billing_last_verified_at")
    val billingActiveUntilMillisKey = longPreferencesKey("billing_active_until_millis")

    fun toStoredAppSettings(preferences: Preferences): StoredAppSettings =
        StoredAppSettings(
            protectionEnabled = preferences[protectionEnabledKey] ?: true,
            accessibilityDisclosureAccepted =
                preferences[accessibilityDisclosureAcceptedKey] ?: false,
            selectedMode =
                enumValueOrDefault(
                    preferences[selectedModeKey],
                    ProtectionMode.BLOCK_SHORTS,
                ),
            enabledPlatformIds = enabledPlatformIds(preferences),
            temporaryAllowUntil = preferences[temporaryAllowUntilKey],
            freeTestStartedAt = preferences[freeTestStartedAtKey],
            freeTestDurationDays =
                preferences[freeTestDurationDaysKey] ?: FreeTestPolicy.DEFAULT_DURATION_DAYS,
            billingInstallationId = preferences[billingInstallationIdKey],
            billingEntitlementState =
                enumValueOrDefault(
                    preferences[billingEntitlementStateKey],
                    BillingEntitlementState.UNKNOWN,
                ),
            billingSubscriptionActive = preferences[billingSubscriptionActiveKey] ?: false,
            billingLastVerifiedAt = preferences[billingLastVerifiedAtKey],
            billingActiveUntilMillis = preferences[billingActiveUntilMillisKey],
            pinHash = preferences[PinPreferenceKeys.PIN_HASH],
            pinSalt = preferences[PinPreferenceKeys.PIN_SALT],
            pinHashVersion =
                preferences[PinPreferenceKeys.PIN_HASH_VERSION] ?: PinHashingPort.CURRENT_VERSION,
            failedPinAttempts = preferences[PinPreferenceKeys.FAILED_PIN_ATTEMPTS] ?: 0,
            pinLockoutUntil = preferences[PinPreferenceKeys.PIN_LOCKOUT_UNTIL],
        )

    fun toSnapshot(settings: StoredAppSettings): AppSettingsSnapshot =
        AppSettingsSnapshot(
            protectionConfiguration =
                ProtectionConfiguration(
                    isEnabled = settings.protectionEnabled,
                    isAccessibilityDisclosureAccepted =
                        settings.accessibilityDisclosureAccepted,
                    mode = settings.selectedMode,
                    enabledPlatformIds = settings.enabledPlatformIds,
                    temporaryAllowUntilMillis = settings.temporaryAllowUntil,
                    isPinConfigured = settings.isPinCreated,
                ),
            entitlement =
                EntitlementSnapshot(
                    freeTestStartedAtMillis = settings.freeTestStartedAt,
                    freeTestDurationDays = settings.freeTestDurationDays,
                    isPaidProtectionAllowed =
                        settings.billingEntitlementState.allowsPaidProtection(),
                    paidLastVerifiedAtMillis = settings.billingLastVerifiedAt,
                    paidActiveUntilMillis = settings.billingActiveUntilMillis,
                ),
            billingEntitlementStateName = settings.billingEntitlementState.name,
        )

    fun enabledPlatformIds(preferences: Preferences): Set<String> =
        preferences[enabledPlatformIdsKey]
            ?.filterTo(mutableSetOf()) { platformId ->
                platformId in ProtectionConfiguration.DEFAULT_ENABLED_PLATFORM_IDS
            }
            ?: ProtectionConfiguration.DEFAULT_ENABLED_PLATFORM_IDS

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String?,
        default: T,
    ): T = value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
