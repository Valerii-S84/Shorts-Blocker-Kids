package com.shortsblockerkids.infrastructure.storage

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.application.port.PinStateUpdate
import com.shortsblockerkids.core.security.PinHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStorePinStateStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun saveUsesLegacyKeysAndResetsAttemptState() =
        runBlocking {
            val dataStore = createDataStore("save")
            dataStore.edit { preferences ->
                preferences[intPreferencesKey("failedPinAttempts")] = 4
                preferences[longPreferencesKey("pinLockoutUntil")] = 9_000L
            }

            DataStorePinStateStore(dataStore).savePinState(
                credential =
                    PinCredential(
                        hashBase64 = "encoded-hash",
                        saltBase64 = "encoded-salt",
                        hashVersion = 7,
                    ),
                attemptState = PinAttemptState(),
            )

            val preferences = dataStore.data.first()
            assertEquals("encoded-hash", preferences[stringPreferencesKey("pinHash")])
            assertEquals("encoded-salt", preferences[stringPreferencesKey("pinSalt")])
            assertEquals(7, preferences[intPreferencesKey("pinHashVersion")])
            assertEquals(0, preferences[intPreferencesKey("failedPinAttempts")])
            assertNull(preferences[longPreferencesKey("pinLockoutUntil")])
        }

    @Test
    fun verificationUsesOneUpdateWithoutPreRead() =
        runBlocking {
            val delegate = createDataStore("atomic")
            delegate.edit { preferences ->
                preferences[stringPreferencesKey("pinHash")] = "encoded-hash"
                preferences[stringPreferencesKey("pinSalt")] = "encoded-salt"
                preferences[intPreferencesKey("pinHashVersion")] = 1
                preferences[intPreferencesKey("failedPinAttempts")] = 4
            }
            val dataStore = RecordingDataStore(delegate)
            var observedCredential: PinCredential? = null
            var observedAttemptState: PinAttemptState? = null

            val result =
                DataStorePinStateStore(dataStore).verifyAndUpdateAtomically {
                    credential,
                    attemptState,
                    ->
                    observedCredential = credential
                    observedAttemptState = attemptState
                    PinStateUpdate(
                        result = PinVerificationResult.Locked(untilMillis = 31_000L),
                        updatedAttemptState =
                            PinAttemptState(
                                failedAttempts = 5,
                                lockoutUntil = 31_000L,
                            ),
                    )
                }

            assertEquals(
                PinCredential(
                    hashBase64 = "encoded-hash",
                    saltBase64 = "encoded-salt",
                    hashVersion = 1,
                ),
                observedCredential,
            )
            assertEquals(PinAttemptState(failedAttempts = 4), observedAttemptState)
            assertEquals(PinVerificationResult.Locked(untilMillis = 31_000L), result)
            assertEquals(1, dataStore.updateDataCount)
            assertEquals(0, dataStore.dataReadCount)
            val preferences = delegate.data.first()
            assertEquals(5, preferences[intPreferencesKey("failedPinAttempts")])
            assertEquals(31_000L, preferences[longPreferencesKey("pinLockoutUntil")])
        }

    @Test
    fun absentVersionAndNullUpdatePreserveStoredState() =
        runBlocking {
            val dataStore = createDataStore("preserve")
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("pinHash")] = "encoded-hash"
                preferences[stringPreferencesKey("pinSalt")] = "encoded-salt"
                preferences[intPreferencesKey("failedPinAttempts")] = 6
                preferences[longPreferencesKey("pinLockoutUntil")] = 61_000L
            }

            val result =
                DataStorePinStateStore(dataStore).verifyAndUpdateAtomically {
                    credential,
                    attemptState,
                    ->
                    assertEquals(PinHasher.CURRENT_VERSION, credential?.hashVersion)
                    assertEquals(
                        PinAttemptState(
                            failedAttempts = 6,
                            lockoutUntil = 61_000L,
                        ),
                        attemptState,
                    )
                    PinStateUpdate(PinVerificationResult.NotConfigured)
                }

            assertEquals(PinVerificationResult.NotConfigured, result)
            val preferences = dataStore.data.first()
            assertNull(preferences[intPreferencesKey("pinHashVersion")])
            assertEquals(6, preferences[intPreferencesKey("failedPinAttempts")])
            assertEquals(61_000L, preferences[longPreferencesKey("pinLockoutUntil")])
        }

    private fun createDataStore(name: String): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = {
                        File(temporaryFolder.root, "$name.preferences_pb").toOkioPath()
                    },
                ),
            scope = scope,
        )
    }

    private class RecordingDataStore(
        private val delegate: DataStore<Preferences>,
    ) : DataStore<Preferences> {
        var dataReadCount: Int = 0
            private set
        var updateDataCount: Int = 0
            private set

        override val data: Flow<Preferences>
            get() {
                dataReadCount += 1
                return delegate.data
            }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            updateDataCount += 1
            return delegate.updateData(transform)
        }
    }
}
