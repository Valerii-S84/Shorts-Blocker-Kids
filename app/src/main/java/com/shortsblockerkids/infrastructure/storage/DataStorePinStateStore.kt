package com.shortsblockerkids.infrastructure.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.PinStateUpdate
import com.shortsblockerkids.core.security.PinHasher

internal class DataStorePinStateStore(
    private val dataStore: DataStore<Preferences>,
) : PinStateStore {
    override suspend fun savePinState(
        credential: PinCredential,
        attemptState: PinAttemptState,
    ) {
        dataStore.edit { preferences ->
            preferences[PinPreferenceKeys.PIN_HASH] = credential.hashBase64
            preferences[PinPreferenceKeys.PIN_SALT] = credential.saltBase64
            preferences[PinPreferenceKeys.PIN_HASH_VERSION] = credential.hashVersion
            preferences.writeAttemptState(attemptState)
        }
    }

    override suspend fun verifyAndUpdateAtomically(
        verification: (PinCredential?, PinAttemptState) -> PinStateUpdate,
    ): PinVerificationResult {
        var result: PinVerificationResult = PinVerificationResult.NotConfigured
        dataStore.edit { preferences ->
            val update =
                verification(
                    preferences.toPinCredential(),
                    preferences.toPinAttemptState(),
                )
            update.updatedAttemptState?.let { attemptState ->
                preferences.writeAttemptState(attemptState)
            }
            result = update.result
        }
        return result
    }

    private fun Preferences.toPinCredential(): PinCredential? {
        val hash = this[PinPreferenceKeys.PIN_HASH]?.takeUnless { it.isBlank() } ?: return null
        val salt = this[PinPreferenceKeys.PIN_SALT]?.takeUnless { it.isBlank() } ?: return null
        return PinCredential(
            hashBase64 = hash,
            saltBase64 = salt,
            hashVersion = this[PinPreferenceKeys.PIN_HASH_VERSION] ?: PinHasher.CURRENT_VERSION,
        )
    }

    private fun Preferences.toPinAttemptState(): PinAttemptState =
        PinAttemptState(
            failedAttempts = this[PinPreferenceKeys.FAILED_PIN_ATTEMPTS] ?: 0,
            lockoutUntil = this[PinPreferenceKeys.PIN_LOCKOUT_UNTIL],
        )

    private fun MutablePreferences.writeAttemptState(attemptState: PinAttemptState) {
        this[PinPreferenceKeys.FAILED_PIN_ATTEMPTS] = attemptState.failedAttempts
        if (attemptState.lockoutUntil == null) {
            remove(PinPreferenceKeys.PIN_LOCKOUT_UNTIL)
        } else {
            this[PinPreferenceKeys.PIN_LOCKOUT_UNTIL] = attemptState.lockoutUntil
        }
    }
}

internal object PinPreferenceKeys {
    val PIN_HASH = stringPreferencesKey("pinHash")
    val PIN_SALT = stringPreferencesKey("pinSalt")
    val PIN_HASH_VERSION = intPreferencesKey("pinHashVersion")
    val FAILED_PIN_ATTEMPTS = intPreferencesKey("failedPinAttempts")
    val PIN_LOCKOUT_UNTIL = longPreferencesKey("pinLockoutUntil")
}
