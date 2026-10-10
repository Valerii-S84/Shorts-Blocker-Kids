package com.shortsblockerkids.presentation.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.shortsblockerkids.infrastructure.storage.DataStoreSettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SettingsWriteUiCoordinatorTest {
    @Test
    fun failedDataStoreWriteDoesNotPublishSuccessAndExplicitRetryRecovers() =
        runBlocking {
            var failWrite = true
            var writes = 0
            var failures = 0
            var completions = 0
            var saved: Preferences = mutablePreferencesOf()
            val dataStore =
                object : DataStore<Preferences> {
                    override val data: Flow<Preferences> = flow { emit(saved) }

                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                        writes++
                        if (failWrite) throw IOException("write failed")
                        return transform(saved).also { saved = it }
                    }
                }
            val store = DataStoreSettingsStore(dataStore)
            val coordinator = SettingsWriteUiCoordinator { failures++ }

            suspend fun changeProtection() =
                coordinator.run {
                    store.setProtectionEnabled(true)
                    completions++
                }

            assertFalse(changeProtection())
            assertEquals(1, writes)
            assertEquals(0, completions)
            assertTrue(saved.asMap().isEmpty())
            failWrite = false
            assertTrue(changeProtection())
            assertEquals(2, writes)
            assertEquals(1, completions)
            assertEquals(1, failures)
        }

    @Test
    fun concurrentChangesKeepWriteAndSuccessCallbacksInSubmissionOrder() =
        runBlocking {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()
            val coordinator = SettingsWriteUiCoordinator { error("unexpected failure") }
            val first =
                async {
                    coordinator.run {
                        events += "first-write"
                        entered.complete(Unit)
                        release.await()
                        events += "first-complete"
                    }
                }
            entered.await()
            val queued = CompletableDeferred<Unit>()
            val second =
                async {
                    queued.complete(Unit)
                    coordinator.run {
                        events += "second-write"
                        events += "second-complete"
                    }
                }
            queued.await()
            assertEquals(listOf("first-write"), events)
            release.complete(Unit)
            assertTrue(first.await())
            assertTrue(second.await())
            assertEquals(listOf("first-write", "first-complete", "second-write", "second-complete"), events)
        }

    @Test
    fun lifecycleCancellationDoesNotReportFailureOrCompleteActionAndReleasesWriter() =
        runBlocking {
            val entered = CompletableDeferred<Unit>()
            var failures = 0
            var completions = 0
            val coordinator = SettingsWriteUiCoordinator { failures++ }
            val job =
                launch {
                    coordinator.run {
                        entered.complete(Unit)
                        awaitCancellation()
                    }
                    completions++
                }
            entered.await()
            job.cancelAndJoin()

            assertEquals(0, failures)
            assertEquals(0, completions)
            assertTrue(coordinator.run { completions++ })
            assertEquals(1, completions)
        }

    @Test
    fun cancelledQueuedActionDoesNotReachStorage() =
        runBlocking {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var queuedWrites = 0
            val coordinator = SettingsWriteUiCoordinator { error("unexpected failure") }
            val active =
                launch {
                    coordinator.run {
                        entered.complete(Unit)
                        release.await()
                    }
                }
            entered.await()
            val waiting = CompletableDeferred<Unit>()
            val queued =
                launch {
                    waiting.complete(Unit)
                    coordinator.run { queuedWrites++ }
                }
            waiting.await()
            queued.cancelAndJoin()
            release.complete(Unit)
            active.join()

            assertEquals(0, queuedWrites)
        }
}
