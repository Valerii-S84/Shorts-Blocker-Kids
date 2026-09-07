package com.shortsblockerkids.infrastructure.storage

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.port.PinLockoutClock
import com.shortsblockerkids.application.port.PinLockoutTime
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.infrastructure.security.Pbkdf2PinHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
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

class PinLockoutPersistenceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private var wallTimeMillis = 1_000L
    private var elapsedRealtimeMillis = 1_000L
    private var bootCount = 1

    @After
    fun tearDown() =
        runBlocking {
            closeStores()
        }

    @Test
    fun reopeningStorePreservesLockoutDespiteWallClockJump() =
        runBlocking {
            val original = createStore()
            seedPin(original)
            verify(original, "4827")
            closeStores()

            wallTimeMillis += 86_400_000L
            elapsedRealtimeMillis += 1_000L
            val reopened = createStore()
            val result = verify(reopened, "4826") as PinVerificationResult.Locked

            assertEquals(29_000L, result.remainingMillis)
            assertEquals(5, reopened.data.first()[intPreferencesKey("failedPinAttempts")])
            elapsedRealtimeMillis += 29_000L
            assertEquals(PinVerificationResult.Success, verify(reopened, "4826"))
            assertLockoutCleared(reopened)
        }

    @Test
    fun rebootRearmIsPersistedAndDoesNotRestartOnNextAppLaunch() =
        runBlocking {
            val original = createStore()
            seedPin(original)
            verify(original, "4827")
            closeStores()

            bootCount = 2
            elapsedRealtimeMillis = 100_000L
            wallTimeMillis += 86_400_000L
            val afterReboot = createStore()
            assertEquals(30_000L, (verify(afterReboot, "4826") as PinVerificationResult.Locked).remainingMillis)
            closeStores()

            elapsedRealtimeMillis += 1_000L
            val reopened = createStore()
            assertEquals(29_000L, (verify(reopened, "4826") as PinVerificationResult.Locked).remainingMillis)
            assertEquals(2, reopened.data.first()[intPreferencesKey("pinLockoutBootCount")])
            elapsedRealtimeMillis += 29_000L
            assertEquals(PinVerificationResult.Success, verify(reopened, "4826"))
        }

    @Test
    fun legacyLockoutMigrationSurvivesStoreReopen() =
        runBlocking {
            val original = createStore()
            seedPin(original)
            original.edit {
                it[intPreferencesKey("failedPinAttempts")] = 6
                it[longPreferencesKey("pinLockoutUntil")] = 500L
            }
            wallTimeMillis += 86_400_000L

            assertEquals(60_000L, (verify(original, "4826") as PinVerificationResult.Locked).remainingMillis)
            closeStores()
            elapsedRealtimeMillis += 1_000L
            val reopened = createStore()

            assertEquals(59_000L, (verify(reopened, "4826") as PinVerificationResult.Locked).remainingMillis)
            assertEquals(6, reopened.data.first()[intPreferencesKey("failedPinAttempts")])
            elapsedRealtimeMillis += 59_000L
            assertEquals(PinVerificationResult.Success, verify(reopened, "4826"))
            assertLockoutCleared(reopened)
        }

    private fun createStore(): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = { File(temporaryFolder.root, "pin.preferences_pb").toOkioPath() },
                ),
            scope = scope,
        )
    }

    private suspend fun closeStores() {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
    }

    private suspend fun seedPin(dataStore: DataStore<Preferences>) {
        DataStorePinStateStore(dataStore).savePinState(
            credential =
                PinCredential(
                    hashBase64 = "JJfkH+VveNsEbC4vvoIyDBewEbKwjYEa29445Woz1lY=",
                    saltBase64 = "AAECAwQFBgcICQoLDA0ODw==",
                    hashVersion = 1,
                ),
            attemptState = PinAttemptState(failedAttempts = 4),
        )
    }

    private suspend fun verify(
        dataStore: DataStore<Preferences>,
        pin: String,
    ): PinVerificationResult =
        VerifyPinUseCase(
            pinStateStore = DataStorePinStateStore(dataStore),
            pinHasher = Pbkdf2PinHasher(),
            timeProvider = TimeProvider { wallTimeMillis },
            lockoutClock = PinLockoutClock { PinLockoutTime(elapsedRealtimeMillis, bootCount) },
        )(pin)

    private suspend fun assertLockoutCleared(dataStore: DataStore<Preferences>) {
        val preferences = dataStore.data.first()
        assertEquals(0, preferences[intPreferencesKey("failedPinAttempts")])
        assertNull(preferences[longPreferencesKey("pinLockoutUntil")])
        assertNull(preferences[longPreferencesKey("pinLockoutElapsedRealtimeUntil")])
        assertNull(preferences[intPreferencesKey("pinLockoutBootCount")])
    }
}
