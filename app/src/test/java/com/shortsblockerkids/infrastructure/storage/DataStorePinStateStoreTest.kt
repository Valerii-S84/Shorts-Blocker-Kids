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
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.port.PinHashingPort
import com.shortsblockerkids.application.port.PinStateUpdate
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.infrastructure.security.Pbkdf2PinHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun verificationUseCaseUsesOneUpdateWithoutPreRead() =
        runBlocking {
            val delegate = createDataStore("atomic")
            delegate.writeKnownPinState(failedAttempts = 4)
            val dataStore = RecordingDataStore(delegate)

            val result = verifyPinUseCase(dataStore, nowMillis = 1_000L)("4827")

            assertEquals(
                PinVerificationResult.Locked(
                    untilMillis = 31_000L,
                    remainingMillis = 30_000L,
                ),
                result,
            )
            assertEquals(1, dataStore.updateDataCount)
            assertEquals(0, dataStore.dataReadCount)
            val preferences = delegate.data.first()
            assertEquals(5, preferences[intPreferencesKey("failedPinAttempts")])
            assertEquals(31_000L, preferences[longPreferencesKey("pinLockoutUntil")])
        }

    @Test
    fun invalidVerificationInputUsesOneUpdateWithoutPreReadOrAttemptMutation() =
        runBlocking {
            val delegate = createDataStore("invalid-input-atomic")
            delegate.writeKnownPinState(failedAttempts = 4)
            val dataStore = RecordingDataStore(delegate)

            val result = verifyPinUseCase(dataStore, nowMillis = 1_000L)("")

            assertEquals(PinVerificationResult.InvalidInput, result)
            assertEquals(1, dataStore.updateDataCount)
            assertEquals(0, dataStore.dataReadCount)
            val preferences = delegate.data.first()
            assertEquals(4, preferences[intPreferencesKey("failedPinAttempts")])
            assertNull(preferences[longPreferencesKey("pinLockoutUntil")])
        }

    @Test
    fun concurrentFailuresUseLatestAttemptStateWithoutLostUpdates() =
        runBlocking {
            val dataStore = createDataStore("concurrent-failures")
            dataStore.writeKnownPinState()
            val useCase = verifyPinUseCase(dataStore, nowMillis = 1_000L)

            val results =
                List(4) {
                    async(Dispatchers.Default) { useCase("4827") }
                }.awaitAll()

            assertTrue(results.all { it is PinVerificationResult.Failure })
            assertEquals(
                setOf(1, 2, 3, 4),
                results
                    .map { it as PinVerificationResult.Failure }
                    .map(PinVerificationResult.Failure::remainingAttempts)
                    .toSet(),
            )
            val preferences = dataStore.data.first()
            assertEquals(4, preferences[intPreferencesKey("failedPinAttempts")])
            assertNull(preferences[longPreferencesKey("pinLockoutUntil")])
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
                    assertEquals(PinHashingPort.CURRENT_VERSION, credential?.hashVersion)
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

    private fun verifyPinUseCase(
        dataStore: DataStore<Preferences>,
        nowMillis: Long,
    ): VerifyPinUseCase =
        VerifyPinUseCase(
            pinStateStore = DataStorePinStateStore(dataStore),
            pinHasher = Pbkdf2PinHasher(),
            timeProvider = TimeProvider { nowMillis },
        )

    private suspend fun DataStore<Preferences>.writeKnownPinState(
        failedAttempts: Int = 0,
        hashVersion: Int? = PinHashingPort.CURRENT_VERSION,
    ) {
        edit { preferences ->
            preferences[stringPreferencesKey("pinHash")] = LEGACY_HASH_BASE64
            preferences[stringPreferencesKey("pinSalt")] = LEGACY_SALT_BASE64
            if (hashVersion == null) {
                preferences.remove(intPreferencesKey("pinHashVersion"))
            } else {
                preferences[intPreferencesKey("pinHashVersion")] = hashVersion
            }
            preferences[intPreferencesKey("failedPinAttempts")] = failedAttempts
            preferences.remove(longPreferencesKey("pinLockoutUntil"))
        }
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

    private companion object {
        const val LEGACY_SALT_BASE64 = "AAECAwQFBgcICQoLDA0ODw=="
        const val LEGACY_HASH_BASE64 = "JJfkH+VveNsEbC4vvoIyDBewEbKwjYEa29445Woz1lY="
    }
}
