package com.shortsblockerkids.infrastructure.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.shortsblockerkids.application.settings.ObserveSettingsUseCase
import com.shortsblockerkids.application.settings.SettingsReadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SettingsReadFailureTest {
    @Test
    fun readFailureRecoveryNeverWritesDefaultsOrReplacesPinMetadata() =
        runBlocking {
            val preferences =
                mutablePreferencesOf(
                    DataStoreSettingsMapper.protectionEnabledKey to true,
                    PinPreferenceKeys.PIN_HASH to "preserved-hash",
                    PinPreferenceKeys.PIN_SALT to "preserved-salt",
                )
            val before = preferences.asMap().toMap()
            var attempts = 0
            var writes = 0
            val store =
                object : DataStore<Preferences> {
                    override val data: Flow<Preferences> =
                        flow {
                            if (attempts++ == 0) throw IOException("transient read")
                            emit(preferences)
                        }

                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                        writes++
                        error("Read recovery must not write")
                    }
                }
            val states = ObserveSettingsUseCase(DataStoreSettingsStore(store), retryDelaysMillis = listOf(1L))().toList()

            assertEquals(0, writes)
            assertEquals(before, preferences.asMap())
            assertTrue((states.last() as SettingsReadState.Ready).snapshot.protectionConfiguration.isPinConfigured)
        }
}
