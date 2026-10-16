package com.shortsblockerkids.infrastructure.storage

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import com.shortsblockerkids.application.port.PinLockoutClock
import com.shortsblockerkids.application.port.PinLockoutTime
import com.shortsblockerkids.application.port.ProtectionActivationOperation
import com.shortsblockerkids.application.protection.isTemporarilyAllowed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MonotonicTemporaryAllowStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun closeStores() {
        runBlocking { cancelOpenStores() }
    }

    @Test
    fun persistedElapsedDeadlineOverridesPastWallDeadline() {
        runBlocking {
            val store = createStore("past-wall")
            store.setTemporaryAllowDeadline(500L, 4_000L, 7)

            val snapshot = store.readSettings().first()
            assertEquals(500L, snapshot.protectionConfiguration.temporaryAllowUntilMillis)
            assertEquals(4_000L, snapshot.protectionConfiguration.temporaryAllowElapsedRealtimeDeadlineMillis)
            assertEquals(7, snapshot.protectionConfiguration.temporaryAllowBootCount)
            assertTrue(snapshot.isTemporarilyAllowed(10_000L, 1_000L))
        }
    }

    @Test
    fun reopeningStoreRestoresSameBootMonotonicAllowance() {
        runBlocking {
            createStore("reopened").setTemporaryAllowDeadline(500L, 4_000L, 7)
            cancelOpenStores()

            val snapshot = createStore("reopened").readSettings().first()
            assertEquals(4_000L, snapshot.protectionConfiguration.temporaryAllowElapsedRealtimeDeadlineMillis)
            assertTrue(snapshot.isTemporarilyAllowed(10_000L, 1_000L))
        }
    }

    @Test
    fun conditionalRemovalPreservesFutureAllowanceAndClearsAllExpiredMetadata() {
        runBlocking {
            val store = createStore("conditional-removal")
            store.setTemporaryAllowDeadline(2_000L, 4_000L, 7)
            assertFalse(
                store.removeTemporaryAllowDeadlineIf { wall, elapsed, boot ->
                    assertEquals(2_000L, wall)
                    assertEquals(4_000L, elapsed)
                    assertEquals(7, boot)
                    false
                },
            )
            assertTrue(store.readSettings().first().isTemporarilyAllowed(10_000L, 1_000L))
            assertTrue(store.removeTemporaryAllowDeadlineIf { _, elapsed, _ -> elapsed != null && elapsed <= 4_000L })

            val protection = store.readSettings().first().protectionConfiguration
            assertNull(protection.temporaryAllowUntilMillis)
            assertNull(protection.temporaryAllowElapsedRealtimeDeadlineMillis)
            assertNull(protection.temporaryAllowBootCount)
        }
    }

    @Test
    fun newBootDoesNotRestoreAllowanceFromFutureWallDeadline() {
        runBlocking {
            var boot = 7
            val store = createStore("reboot") { boot }
            store.setTemporaryAllowDeadline(20_000L, 4_000L, 7)
            boot = 8

            val snapshot = store.readSettings().first()
            assertNull(snapshot.protectionConfiguration.temporaryAllowUntilMillis)
            assertNull(snapshot.protectionConfiguration.temporaryAllowElapsedRealtimeDeadlineMillis)
            assertFalse(snapshot.isTemporarilyAllowed(1_000L, 1_000L))
        }
    }

    @Test
    fun activationDecisionCannotRestoreAllowanceFromPreviousBoot() {
        runBlocking {
            var boot = 7
            val store = createStore("activation-after-reboot") { boot }
            store.setTemporaryAllowDeadline(20_000L, 4_000L, 7)
            boot = 8

            val operation =
                store.completeProtectionActivation { snapshot ->
                    assertNull(snapshot.protectionConfiguration.temporaryAllowUntilMillis)
                    assertNull(snapshot.protectionConfiguration.temporaryAllowElapsedRealtimeDeadlineMillis)
                    assertEquals(7, snapshot.protectionConfiguration.temporaryAllowBootCount)
                    assertFalse(snapshot.isTemporarilyAllowed(1_000L, 1_000L))
                    ProtectionActivationOperation.PrerequisitesNotMet
                }

            assertEquals(ProtectionActivationOperation.PrerequisitesNotMet, operation)
        }
    }

    private fun createStore(
        name: String,
        bootCount: () -> Int = { 7 },
    ): DataStoreSettingsStore {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        val dataStore =
            PreferenceDataStoreFactory.create(
                storage =
                    OkioStorage(
                        fileSystem = FileSystem.SYSTEM,
                        serializer = PreferencesSerializer,
                        producePath = { File(temporaryFolder.root, "$name.preferences_pb").toOkioPath() },
                    ),
                scope = scope,
            )
        return DataStoreSettingsStore(dataStore, PinLockoutClock { PinLockoutTime(1_000L, bootCount()) })
    }

    private suspend fun cancelOpenStores() {
        scopes.forEach { scope ->
            scope.cancel()
            scope.coroutineContext[Job]?.join()
        }
        scopes.clear()
    }
}
