package com.shortsblockerkids.infrastructure.storage

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.port.PinHashingPort
import com.shortsblockerkids.application.port.PinLockoutClock
import com.shortsblockerkids.application.port.PinLockoutTime
import com.shortsblockerkids.application.port.ProtectionActivationOperation
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.application.protection.canProtect
import com.shortsblockerkids.application.protection.hasBillingEntitlement
import com.shortsblockerkids.domain.detection.SupportedPlatform
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.pin.PinValidationResult
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionMode
import com.shortsblockerkids.infrastructure.security.Pbkdf2PinHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Base64
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class DataStoreSettingsStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        runBlocking {
            cancelOpenStores()
        }
    }

    @Test
    fun storeNameAndNonServerPreferenceKeysRemainByteCompatible() {
        assertEquals("shorts_blocker_settings", SETTINGS_STORE_NAME)
        assertEquals(
            listOf(
                "protectionEnabled",
                "accessibilityDisclosureAccepted",
                "selectedMode",
                "enabledPlatformIds",
                "temporaryAllowUntil",
                "free_test_started_at",
                "free_test_duration_days",
                "play_subscription_active",
                "pinHash",
                "pinSalt",
                "pinHashVersion",
                "failedPinAttempts",
                "pinLockoutUntil",
            ),
            listOf(
                DataStoreSettingsMapper.protectionEnabledKey.name,
                DataStoreSettingsMapper.accessibilityDisclosureAcceptedKey.name,
                DataStoreSettingsMapper.selectedModeKey.name,
                DataStoreSettingsMapper.enabledPlatformIdsKey.name,
                DataStoreSettingsMapper.temporaryAllowUntilKey.name,
                DataStoreSettingsMapper.freeTestStartedAtKey.name,
                DataStoreSettingsMapper.freeTestDurationDaysKey.name,
                DataStoreSettingsMapper.playSubscriptionActiveKey.name,
                PinPreferenceKeys.PIN_HASH.name,
                PinPreferenceKeys.PIN_SALT.name,
                PinPreferenceKeys.PIN_HASH_VERSION.name,
                PinPreferenceKeys.FAILED_PIN_ATTEMPTS.name,
                PinPreferenceKeys.PIN_LOCKOUT_UNTIL.name,
            ),
        )
    }

    @Test
    fun createsPinHashAndVerifiesPinWithoutPlainTextStorage() =
        runBlocking {
            val dataStore = createDataStore("pin")
            val repository = DataStoreSettingsStore(dataStore)

            repository.savePin("4826")
            val settings = repository.readSettings().first()
            val storedSettings = repository.readStoredSettings().first()
            val rawPreferences = dataStore.data.first().asMap()

            assertTrue(settings.protectionConfiguration.isPinConfigured)
            assertNotNull(storedSettings.pinSalt)
            assertFalse(storedSettings.pinHash.orEmpty().contains("4826"))
            assertEquals(
                setOf("pinHash", "pinSalt", "pinHashVersion", "failedPinAttempts"),
                rawPreferences.keys.map { key -> key.name }.toSet(),
            )
            assertTrue(
                rawPreferences.values.none { value -> value.toString().contains("4826") },
            )
            assertEquals(PinVerificationResult.Success, repository.verifyPin("4826"))
            assertTrue(repository.verifyPin("4827") is PinVerificationResult.Failure)
        }

    @Test
    fun contextConstructorUsesApplicationContextBackedSettingsStore() =
        runBlocking {
            val repository =
                DataStoreSettingsStore(
                    FileBackedContext(temporaryFolder.newFolder("context-store")),
                    deadlineClock = PinLockoutClock { PinLockoutTime(1_000L, 1) },
                )

            repository.setProtectionEnabled(false)

            assertFalse(
                repository
                    .readSettings()
                    .first()
                    .protectionConfiguration
                    .isEnabled,
            )
        }

    @Test
    fun hardCodedLegacyCredentialWithoutVersionVerifies() =
        runBlocking {
            val dataStore = createDataStore("legacy-pin")
            dataStore.writeLegacyPinCredential()
            val repository = DataStoreSettingsStore(dataStore)

            assertEquals(PinVerificationResult.Success, repository.verifyPin(LEGACY_PIN))
            assertNull(dataStore.data.first()[intPreferencesKey("pinHashVersion")])
        }

    @Test
    fun unknownHashVersionKeepsNonDispatchVerification() =
        runBlocking {
            val dataStore = createDataStore("unknown-pin-version")
            dataStore.writeLegacyPinCredential(hashVersion = 999)
            val repository = DataStoreSettingsStore(dataStore)

            assertEquals(PinVerificationResult.Success, repository.verifyPin(LEGACY_PIN))
            assertEquals(999, dataStore.data.first()[intPreferencesKey("pinHashVersion")])
        }

    @Test
    fun newPinKeepsLegacyBase64Representation() =
        runBlocking {
            val dataStore = createDataStore("pin-format")
            val repository = DataStoreSettingsStore(dataStore)

            repository.savePin(LEGACY_PIN)

            val stored = dataStore.data.first()
            val salt = requireNotNull(stored[stringPreferencesKey("pinSalt")])
            val hash = requireNotNull(stored[stringPreferencesKey("pinHash")])
            val saltBytes = Base64.getDecoder().decode(salt)
            val hashBytes = Base64.getDecoder().decode(hash)
            assertEquals(16, saltBytes.size)
            assertEquals(32, hashBytes.size)
            assertEquals(salt, Base64.getEncoder().encodeToString(saltBytes))
            assertEquals(hash, Base64.getEncoder().encodeToString(hashBytes))
            assertEquals(
                PinHashingPort.CURRENT_VERSION,
                stored[intPreferencesKey("pinHashVersion")],
            )
            assertFalse(hash.contains(LEGACY_PIN))
        }

    @Test
    fun freeTestStartsOnlyAfterProtectionActivation() =
        runBlocking {
            val repository = createRepository("free-test-start")

            assertEquals(
                null,
                repository
                    .readSettings()
                    .first()
                    .entitlement.freeTestStartedAtMillis,
            )

            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.setProtectionEnabled(true)

            assertEquals(
                null,
                repository
                    .readSettings()
                    .first()
                    .entitlement.freeTestStartedAtMillis,
            )

            repository.setProtectionEnabled(false)
            repository.completeProtectionActivationForTest(nowMillis = 5_000L)
            val settings = repository.readSettings().first()

            assertTrue(settings.protectionConfiguration.isEnabled)
            assertEquals(5_000L, settings.entitlement.freeTestStartedAtMillis)
            assertEquals(FreeTestPolicy.DEFAULT_DURATION_DAYS, settings.entitlement.freeTestDurationDays)
        }

    @Test
    fun prerequisitesNotMetActivationLeavesStoredSettingsUntouched() =
        runBlocking {
            val repository = createRepository("activation-prerequisites-not-met")

            repository.setProtectionEnabled(false)
            val operation =
                repository.completeProtectionActivation {
                    ProtectionActivationOperation.PrerequisitesNotMet
                }
            val settings = repository.readSettings().first()

            assertEquals(ProtectionActivationOperation.PrerequisitesNotMet, operation)
            assertFalse(settings.protectionConfiguration.isEnabled)
            assertEquals(null, settings.entitlement.freeTestStartedAtMillis)
        }

    @Test
    fun repeatedActivationPreservesFreeTestStartAndDurationAndReenablesProtection() =
        runBlocking {
            val dataStore = createDataStore("free-test-repeat")
            val repository = DataStoreSettingsStore(dataStore)
            repository.completeProtectionActivationForTest(nowMillis = 5_000L)
            dataStore.edit { preferences ->
                preferences[intPreferencesKey("free_test_duration_days")] = 30
            }
            repository.setProtectionEnabled(false)

            repository.completeProtectionActivationForTest(nowMillis = 10_000L)
            val settings = repository.readSettings().first()

            assertTrue(settings.protectionConfiguration.isEnabled)
            assertEquals(5_000L, settings.entitlement.freeTestStartedAtMillis)
            assertEquals(30, settings.entitlement.freeTestDurationDays)
        }

    @Test
    fun alreadyStartedActivationReenablesProtectionWithoutRestartingFreeTest() =
        runBlocking {
            val repository = createRepository("activation-already-started")
            repository.completeProtectionActivationForTest(nowMillis = 5_000L)
            repository.setProtectionEnabled(false)

            val operation =
                repository.completeProtectionActivation {
                    ProtectionActivationOperation.AlreadyStarted
                }

            val settings = repository.readSettings().first()
            assertEquals(ProtectionActivationOperation.AlreadyStarted, operation)
            assertTrue(settings.protectionConfiguration.isEnabled)
            assertEquals(5_000L, settings.entitlement.freeTestStartedAtMillis)
        }

    @Test
    fun synchronousDataStoreMutatorsApplySettingsWithoutSuspension() =
        runBlocking {
            val dataStore = ImmediatePreferencesDataStore()
            val repository = DataStoreSettingsStore(dataStore)

            repository.setProtectionEnabled(false)
            repository.setDisclosureAccepted(true)
            repository.setSelectedMode(ProtectionMode.BLOCK_SHORTS)
            repository.setPlatformEnabled(SupportedPlatform.TIKTOK.id, false)
            repository.updateBillingEntitlement(
                BillingEntitlementSnapshot(
                    state = BillingEntitlementState.ACTIVE,
                ),
            )
            repository.setTemporaryAllowUntil(9_000L)
            val removed =
                repository.removeTemporaryAllowIf { allowUntilMillis ->
                    allowUntilMillis > 10_000L
                }

            val settings = repository.readSettings().first()
            assertFalse(settings.protectionConfiguration.isEnabled)
            assertTrue(settings.protectionConfiguration.isAccessibilityDisclosureAccepted)
            assertFalse(
                settings.protectionConfiguration.isPlatformEnabled(SupportedPlatform.TIKTOK.id),
            )
            assertTrue(settings.entitlement.isPaidProtectionAllowed)
            assertEquals(9_000L, settings.protectionConfiguration.temporaryAllowUntilMillis)
            assertFalse(removed)
        }

    @Test
    fun suspendingDataStoreMutatorsResumeSettingsEdits() =
        runBlocking {
            val dataStore = SuspendingPreferencesDataStore()
            val repository = DataStoreSettingsStore(dataStore)

            repository.setProtectionEnabled(false)
            repository.completeProtectionActivation {
                ProtectionActivationOperation.Record(nowMillis = 7_000L)
            }
            repository.updateBillingEntitlement(isActive = false)
            repository.setDisclosureAccepted(false)
            repository.setSelectedMode(ProtectionMode.BLOCK_SHORTS)
            repository.setPlatformEnabled(SupportedPlatform.FACEBOOK_REELS.id, true)
            repository.setTemporaryAllowUntil(null)
            val removed = repository.removeTemporaryAllowIf { true }

            val settings = repository.readSettings().first()
            assertTrue(settings.protectionConfiguration.isEnabled)
            assertEquals(7_000L, settings.entitlement.freeTestStartedAtMillis)
            assertFalse(settings.entitlement.isPaidProtectionAllowed)
            assertTrue(settings.protectionConfiguration.isPlatformEnabled(SupportedPlatform.FACEBOOK_REELS.id))
            assertEquals(null, settings.protectionConfiguration.temporaryAllowUntilMillis)
            assertFalse(removed)
        }

    @Test
    fun dataStoreEditFailuresPropagateFromSettingsMutators() {
        val repository = DataStoreSettingsStore(FailingPreferencesDataStore())
        val mutators =
            listOf<suspend () -> Unit>(
                { repository.setProtectionEnabled(true) },
                {
                    repository.completeProtectionActivation {
                        ProtectionActivationOperation.AlreadyStarted
                    }
                },
                { repository.updateBillingEntitlement(isActive = true) },
                {
                    repository.updateBillingEntitlement(
                        BillingEntitlementSnapshot(
                            state = BillingEntitlementState.INACTIVE,
                        ),
                    )
                },
                { repository.setDisclosureAccepted(true) },
                { repository.setSelectedMode(ProtectionMode.BLOCK_SHORTS) },
                { repository.setPlatformEnabled(SupportedPlatform.YOUTUBE_SHORTS.id, true) },
                { repository.setTemporaryAllowUntil(1_000L) },
                { repository.removeTemporaryAllowIf { true } },
            )

        mutators.forEach { mutator ->
            val failure =
                assertThrows(IllegalStateException::class.java) {
                    runBlocking {
                        mutator()
                    }
                }
            assertEquals("edit failed", failure.message)
        }
    }

    @Test
    fun suspendingDataStoreEditFailuresPropagateFromSettingsMutators() {
        val repository = DataStoreSettingsStore(SuspendingFailingPreferencesDataStore())
        val mutators =
            listOf<suspend () -> Unit>(
                { repository.setProtectionEnabled(true) },
                {
                    repository.completeProtectionActivation {
                        ProtectionActivationOperation.AlreadyStarted
                    }
                },
                { repository.updateBillingEntitlement(isActive = true) },
                {
                    repository.updateBillingEntitlement(
                        BillingEntitlementSnapshot(
                            state = BillingEntitlementState.INACTIVE,
                        ),
                    )
                },
                { repository.setDisclosureAccepted(true) },
                { repository.setSelectedMode(ProtectionMode.BLOCK_SHORTS) },
                { repository.setPlatformEnabled(SupportedPlatform.YOUTUBE_SHORTS.id, true) },
                { repository.setTemporaryAllowUntil(1_000L) },
                { repository.removeTemporaryAllowIf { true } },
            )

        mutators.forEach { mutator ->
            val failure =
                assertThrows(IllegalStateException::class.java) {
                    runBlocking {
                        mutator()
                    }
                }
            assertEquals("suspending edit failed", failure.message)
        }
    }

    @Test
    fun appRestartDoesNotResetFreeTestTimer() =
        runBlocking {
            val firstRepository = createRepository("free-test-restart")
            firstRepository.completeProtectionActivationForTest(nowMillis = 5_000L)
            cancelOpenStores()

            val restartedRepository = createRepository("free-test-restart")
            restartedRepository.completeProtectionActivationForTest(nowMillis = 10_000L)
            val settings = restartedRepository.readSettings().first()

            assertEquals(5_000L, settings.entitlement.freeTestStartedAtMillis)
            assertEquals(FreeTestPolicy.DEFAULT_DURATION_DAYS, settings.entitlement.freeTestDurationDays)
        }

    @Test
    fun persistsProtectionStateAndTemporaryAllowExpiry() =
        runBlocking {
            val repository = createRepository("settings")

            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.completeProtectionActivationForTest(nowMillis = 1_000L)
            repository.setTemporaryAllowUntil(2_000L)

            val allowedSettings = repository.readSettings().first()
            assertFalse(allowedSettings.canProtect(nowMillis = 1_500L))
            assertTrue(allowedSettings.canProtect(nowMillis = 2_500L))

            repository.setProtectionEnabled(false)
            assertFalse(repository.readSettings().first().canProtect(nowMillis = 2_500L))
        }

    @Test
    fun persistsSelectedProtectionMode() =
        runBlocking {
            val repository = createRepository("selected-mode")

            repository.setSelectedMode(ProtectionMode.BLOCK_SHORTS)

            assertEquals(
                ProtectionMode.BLOCK_SHORTS,
                repository
                    .readSettings()
                    .first()
                    .protectionConfiguration.mode,
            )
        }

    @Test
    fun persistsEnabledProtectedAppsAcrossRestart() =
        runBlocking {
            val firstRepository = createRepository("enabled-platforms")

            firstRepository.setPlatformEnabled(SupportedPlatform.TIKTOK.id, false)
            firstRepository.setPlatformEnabled(SupportedPlatform.FACEBOOK_REELS.id, false)
            val firstSettings = firstRepository.readSettings().first()

            assertFalse(
                firstSettings.protectionConfiguration.isPlatformEnabled(SupportedPlatform.TIKTOK.id),
            )
            assertFalse(
                firstSettings.protectionConfiguration.isPlatformEnabled(
                    SupportedPlatform.FACEBOOK_REELS.id,
                ),
            )
            assertTrue(
                firstSettings.protectionConfiguration.isPlatformEnabled(
                    SupportedPlatform.YOUTUBE_SHORTS.id,
                ),
            )
            cancelOpenStores()

            val restartedRepository = createRepository("enabled-platforms")
            val restartedSettings = restartedRepository.readSettings().first()

            assertEquals(
                firstSettings.protectionConfiguration.enabledPlatformIds,
                restartedSettings.protectionConfiguration.enabledPlatformIds,
            )
        }

    @Test
    fun disablingEveryProtectedAppPreventsProtectionUntilOneIsEnabledAgain() =
        runBlocking {
            val repository = createRepository("all-platforms-disabled")
            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.completeProtectionActivationForTest(nowMillis = 1_000L)

            ProtectionConfiguration.DEFAULT_ENABLED_PLATFORM_IDS.forEach { platformId ->
                repository.setPlatformEnabled(platformId, false)
            }
            val disabledSettings = repository.readSettings().first()

            assertEquals(emptySet<String>(), disabledSettings.protectionConfiguration.enabledPlatformIds)
            assertFalse(disabledSettings.canProtect(nowMillis = 1_500L))

            repository.setPlatformEnabled(SupportedPlatform.INSTAGRAM_REELS.id, true)
            val reenabledSettings = repository.readSettings().first()

            assertEquals(
                setOf(SupportedPlatform.INSTAGRAM_REELS.id),
                reenabledSettings.protectionConfiguration.enabledPlatformIds,
            )
            assertTrue(reenabledSettings.canProtect(nowMillis = 1_500L))
        }

    @Test
    fun unknownStoredPlatformIdsAreIgnored() =
        runBlocking {
            val dataStore = createDataStore("unknown-platform")
            dataStore.edit { preferences ->
                preferences[stringSetPreferencesKey("enabledPlatformIds")] =
                    setOf(SupportedPlatform.YOUTUBE_SHORTS.id, "unknown_platform")
            }
            val repository = DataStoreSettingsStore(dataStore)

            assertEquals(
                setOf(SupportedPlatform.YOUTUBE_SHORTS.id),
                repository
                    .readSettings()
                    .first()
                    .protectionConfiguration.enabledPlatformIds,
            )
        }

    @Test
    fun unknownProtectedPlatformIdIsRejected() {
        val repository = createRepository("unknown-platform-id")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.setPlatformEnabled("unknown_platform", true)
            }
        }
    }

    @Test
    fun persistsPlayBillingEntitlementWithoutServerTimeLimit() =
        runBlocking {
            val repository = createRepository("billing")

            repository.updateBillingEntitlement(isActive = true)
            val activeSettings = repository.readSettings().first()
            assertTrue(repository.readStoredSettings().first().billingSubscriptionActive)
            assertTrue(activeSettings.hasBillingEntitlement())

            repository.updateBillingEntitlement(isActive = false)
            val inactiveSettings = repository.readSettings().first()
            assertFalse(repository.readStoredSettings().first().billingSubscriptionActive)
            assertFalse(inactiveSettings.hasBillingEntitlement())
        }

    @Test
    fun legacyDataStoreFileKeepsNonServerDataAfterPlayWriteAndReopen() =
        runBlocking {
            val dataStore = createDataStore("legacy-play-upgrade")
            dataStore.edit { preferences ->
                preferences[booleanPreferencesKey("protectionEnabled")] = false
                preferences[booleanPreferencesKey("accessibilityDisclosureAccepted")] = true
                preferences[stringPreferencesKey("selectedMode")] = ProtectionMode.BLOCK_SHORTS.name
                preferences[stringSetPreferencesKey("enabledPlatformIds")] = setOf(SupportedPlatform.YOUTUBE_SHORTS.id)
                preferences[longPreferencesKey("temporaryAllowUntil")] = 9_000L
                preferences[DataStoreSettingsMapper.temporaryAllowElapsedRealtimeDeadlineKey] = 8_000L
                preferences[DataStoreSettingsMapper.temporaryAllowBootCountKey] = 2
                preferences[longPreferencesKey("free_test_started_at")] = 1_000L
                preferences[intPreferencesKey("free_test_duration_days")] = 30
                preferences[PinPreferenceKeys.PIN_HASH] = "preserved-hash"
                preferences[PinPreferenceKeys.PIN_SALT] = "preserved-salt"
                preferences[PinPreferenceKeys.PIN_HASH_VERSION] = PinHashingPort.CURRENT_VERSION
                preferences[PinPreferenceKeys.FAILED_PIN_ATTEMPTS] = 2
                preferences[PinPreferenceKeys.PIN_LOCKOUT_UNTIL] = 10_000L
                preferences[stringPreferencesKey("billing_installation_id")] = "old-install"
                preferences[stringPreferencesKey("billing_entitlement_state")] = "ACTIVE"
                preferences[booleanPreferencesKey("billing_subscription_active")] = true
                preferences[longPreferencesKey("billing_last_verified_at")] = 2_000L
                preferences[longPreferencesKey("billing_active_until_millis")] = 3_000L
            }
            val before =
                dataStore.data
                    .first()
                    .asMap()
                    .toMap()
            val repository = DataStoreSettingsStore(dataStore)
            val beforePlay = repository.readSettings().first()
            assertFalse(beforePlay.hasBillingEntitlement())
            assertFalse(beforePlay.protectionConfiguration.isEnabled)
            assertTrue(beforePlay.protectionConfiguration.isAccessibilityDisclosureAccepted)
            assertTrue(beforePlay.protectionConfiguration.isPinConfigured)
            assertEquals(setOf(SupportedPlatform.YOUTUBE_SHORTS.id), beforePlay.protectionConfiguration.enabledPlatformIds)
            assertEquals(9_000L, beforePlay.protectionConfiguration.temporaryAllowUntilMillis)
            assertEquals(8_000L, beforePlay.protectionConfiguration.temporaryAllowElapsedRealtimeDeadlineMillis)
            assertEquals(2, beforePlay.protectionConfiguration.temporaryAllowBootCount)
            assertEquals(1_000L, beforePlay.entitlement.freeTestStartedAtMillis)
            assertEquals(30, beforePlay.entitlement.freeTestDurationDays)

            repository.updateBillingEntitlement(isActive = true)
            val after =
                dataStore.data
                    .first()
                    .asMap()
                    .toMap()
            assertEquals(before, after.filterKeys { it != DataStoreSettingsMapper.playSubscriptionActiveKey })
            assertEquals(true, after[DataStoreSettingsMapper.playSubscriptionActiveKey])
            assertTrue(repository.readSettings().first().hasBillingEntitlement())
            cancelOpenStores()

            val reopenedStore = createDataStore("legacy-play-upgrade")
            val reopened = DataStoreSettingsStore(reopenedStore)
            val reopenedSettings = reopened.readSettings().first()
            val reopenedPreferences =
                reopenedStore.data
                    .first()
                    .asMap()
                    .toMap()
            assertEquals(after, reopenedPreferences)
            assertTrue(reopenedSettings.hasBillingEntitlement())
            assertEquals(beforePlay.protectionConfiguration, reopenedSettings.protectionConfiguration)
            assertEquals(beforePlay.entitlement.freeTestStartedAtMillis, reopenedSettings.entitlement.freeTestStartedAtMillis)
            assertEquals(beforePlay.entitlement.freeTestDurationDays, reopenedSettings.entitlement.freeTestDurationDays)
            val stored = reopened.readStoredSettings().first()
            assertEquals("preserved-hash", stored.pinHash)
            assertEquals("preserved-salt", stored.pinSalt)
            assertEquals(PinHashingPort.CURRENT_VERSION, stored.pinHashVersion)
            assertEquals(2, stored.failedPinAttempts)
            assertEquals(10_000L, stored.pinLockoutUntil)
        }

    @Test
    fun storesExactTemporaryAllowTimestampUnderExistingKey() =
        runBlocking {
            val dataStore = createDataStore("allow-timestamp")
            val repository = DataStoreSettingsStore(dataStore)

            repository.setTemporaryAllowUntil(901_234L)

            val storedPreferences = dataStore.data.first()
            assertEquals(
                901_234L,
                storedPreferences[longPreferencesKey("temporaryAllowUntil")],
            )
            assertEquals(
                901_234L,
                repository
                    .readSettings()
                    .first()
                    .protectionConfiguration.temporaryAllowUntilMillis,
            )
        }

    @Test
    fun acceptAccessibilityDisclosureStoresAcceptedDisclosureState() =
        runBlocking {
            val repository = createRepository("accept-disclosure")

            repository.acceptAccessibilityDisclosure()

            assertTrue(
                repository
                    .readSettings()
                    .first()
                    .protectionConfiguration
                    .isAccessibilityDisclosureAccepted,
            )
        }

    @Test
    fun removeTemporaryAllowIfPhysicallyRemovesMatchingValue() =
        runBlocking {
            val repository = createRepository("expired-allow")

            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.completeProtectionActivationForTest(nowMillis = 1_000L)
            repository.setTemporaryAllowUntil(2_000L)

            val removed =
                repository.removeTemporaryAllowIf { allowUntilMillis ->
                    allowUntilMillis <= 2_500L
                }
            val settings = repository.readSettings().first()

            assertTrue(removed)
            assertEquals(null, settings.protectionConfiguration.temporaryAllowUntilMillis)
            assertTrue(settings.canProtect(nowMillis = 1_500L))
        }

    @Test
    fun removeTemporaryAllowIfLeavesMissingOrNonMatchingValueUntouched() =
        runBlocking {
            val repository = createRepository("clear-allow-no-op")

            val missingRemoved = repository.removeTemporaryAllowIf { true }
            assertEquals(
                null,
                repository
                    .readSettings()
                    .first()
                    .protectionConfiguration.temporaryAllowUntilMillis,
            )

            repository.setTemporaryAllowUntil(3_000L)
            val activeRemoved =
                repository.removeTemporaryAllowIf { allowUntilMillis ->
                    allowUntilMillis <= 2_500L
                }

            assertFalse(missingRemoved)
            assertFalse(activeRemoved)
            assertEquals(
                3_000L,
                repository
                    .readSettings()
                    .first()
                    .protectionConfiguration.temporaryAllowUntilMillis,
            )
        }

    @Test
    fun clearingTemporaryAllowRemovesStoredExpiry() =
        runBlocking {
            val repository = createRepository("clear-allow")

            repository.setTemporaryAllowUntil(3_000L)
            repository.setTemporaryAllowUntil(null)

            assertEquals(
                null,
                repository
                    .readSettings()
                    .first()
                    .protectionConfiguration.temporaryAllowUntilMillis,
            )
        }

    @Test
    fun escalatesPinLockoutAcrossFailedAttempts() =
        runBlocking {
            val repository = createRepository("lockout")
            repository.savePin("4826")

            repeat(4) { attempt ->
                val result = repository.verifyPin("1111", nowMillis = 1_000L + attempt)
                assertTrue(result is PinVerificationResult.Failure)
            }

            val fifth = repository.verifyPin("1111", nowMillis = 2_000L)
            assertEquals(
                PinVerificationResult.Locked(
                    untilMillis = 32_000L,
                    remainingMillis = 30_000L,
                ),
                fifth,
            )
            assertEquals(5, repository.readStoredSettings().first().failedPinAttempts)

            val sixth = repository.verifyPin("1111", nowMillis = 32_001L)
            assertEquals(
                PinVerificationResult.Locked(
                    untilMillis = 92_001L,
                    remainingMillis = 60_000L,
                ),
                sixth,
            )
            assertEquals(6, repository.readStoredSettings().first().failedPinAttempts)
        }

    @Test
    fun verifyPinWithoutSavedPinReportsNotConfigured() =
        runBlocking {
            val repository = createRepository("pin-not-configured")

            assertEquals(PinVerificationResult.NotConfigured, repository.verifyPin("4826"))
        }

    @Test
    fun activePinLockoutRejectsEvenCorrectPinWithoutIncreasingAttempts() =
        runBlocking {
            val repository = createRepository("active-lockout")
            repository.savePin("4826")

            repeat(5) { attempt ->
                repository.verifyPin("1111", nowMillis = 1_000L + attempt)
            }
            val lockedBeforeRetry = repository.readStoredSettings().first()

            assertEquals(
                PinVerificationResult.Locked(
                    untilMillis = 31_004L,
                    remainingMillis = 29_004L,
                ),
                repository.verifyPin("4826", nowMillis = 2_000L),
            )
            assertEquals(
                lockedBeforeRetry.failedPinAttempts,
                repository.readStoredSettings().first().failedPinAttempts,
            )
        }

    @Test
    fun pinRemainsRequiredForSettingsAccess() =
        runBlocking {
            val repository = createRepository("settings-pin")

            repository.savePin("4826")

            assertTrue(repository.verifyPin("0000") is PinVerificationResult.Failure)
            assertEquals(PinVerificationResult.Success, repository.verifyPin("4826"))
        }

    @Test
    fun correctPinCanUnlockBlockingFlow() =
        runBlocking {
            val repository = createRepository("correct-pin-unlock")

            repository.savePin("4826")

            assertEquals(PinVerificationResult.Success, repository.verifyPin("4826"))
        }

    @Test
    fun incorrectPinDoesNotUnlockBlockingFlow() =
        runBlocking {
            val repository = createRepository("incorrect-pin-unlock")

            repository.savePin("4826")

            assertTrue(repository.verifyPin("4827") is PinVerificationResult.Failure)
        }

    @Test
    fun incorrectPinDoesNotSaveTemporaryAllowOrDisableBlocking() =
        runBlocking {
            val repository = createRepository("incorrect-pin-no-allow")
            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.completeProtectionActivationForTest(nowMillis = 1_000L)

            assertTrue(repository.verifyPin("4827") is PinVerificationResult.Failure)
            val settings = repository.readSettings().first()

            assertEquals(null, settings.protectionConfiguration.temporaryAllowUntilMillis)
            assertTrue(settings.canProtect(nowMillis = 1_500L))
        }

    @Test
    fun emptyPinDoesNotUnlockBlockingFlow() =
        runBlocking {
            val repository = createRepository("empty-pin-unlock")

            repository.savePin("4826")

            assertEquals(PinVerificationResult.InvalidInput, repository.verifyPin(""))
        }

    @Test
    fun repeatedEmptyPinAttemptsKeepProtectionActiveWithoutTemporaryAllow() =
        runBlocking {
            val repository = createRepository("empty-pin-protection")
            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.completeProtectionActivationForTest(nowMillis = 1_000L)

            repeat(5) { attempt ->
                assertEquals(
                    PinVerificationResult.InvalidInput,
                    repository.verifyPin("", nowMillis = 2_000L + attempt),
                )
            }
            val settings = repository.readSettings().first()

            assertEquals(null, settings.protectionConfiguration.temporaryAllowUntilMillis)
            assertTrue(settings.protectionConfiguration.isEnabled)
            assertTrue(settings.canProtect(nowMillis = 2_500L))
        }

    @Test
    fun repeatedEmptyPinDoesNotIncrementAttemptsOrLockOutCorrectPin() =
        runBlocking {
            val repository = createRepository("empty-pin-lockout")
            repository.savePin("4826")

            repeat(5) { attempt ->
                assertEquals(
                    PinVerificationResult.InvalidInput,
                    repository.verifyPin("", nowMillis = 1_000L + attempt),
                )
            }
            val storedSettings = repository.readStoredSettings().first()

            assertEquals(0, storedSettings.failedPinAttempts)
            assertNull(storedSettings.pinLockoutUntil)
            assertEquals(
                PinVerificationResult.Success,
                repository.verifyPin("4826", nowMillis = 2_000L),
            )
        }

    @Test
    fun partialPinDoesNotUnlockBlockingFlow() =
        runBlocking {
            val repository = createRepository("partial-pin-unlock")

            repository.savePin("4826")

            assertEquals(PinVerificationResult.InvalidInput, repository.verifyPin("482"))
        }

    @Test
    fun corruptedPinMetadataDoesNotCrashVerification() =
        runBlocking {
            val dataStore = createDataStore("corrupted-pin")
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("pinHash")] = "not-base64"
                preferences[stringPreferencesKey("pinSalt")] = "not-base64"
            }
            val repository = DataStoreSettingsStore(dataStore)

            assertEquals(PinVerificationResult.NotConfigured, repository.verifyPin("4826"))
        }

    @Test
    fun appRestartDoesNotBreakPinState() =
        runBlocking {
            val firstRepository = createRepository("pin-restart")
            firstRepository.savePin("4826")
            cancelOpenStores()

            val restartedRepository = createRepository("pin-restart")

            assertEquals(PinVerificationResult.Success, restartedRepository.verifyPin("4826"))
            assertTrue(restartedRepository.verifyPin("4827") is PinVerificationResult.Failure)
        }

    private fun createRepository(name: String): DataStoreSettingsStore {
        val dataStore = createDataStore(name)
        return DataStoreSettingsStore(dataStore)
    }

    private suspend fun DataStoreSettingsStore.savePin(pin: String) {
        val result =
            CreatePinUseCase(
                pinStateStore = this,
                pinHasher = Pbkdf2PinHasher(),
            )(pin, pin)
        assertEquals(PinValidationResult.Valid, result)
    }

    private suspend fun DataStoreSettingsStore.completeProtectionActivationForTest(nowMillis: Long) {
        completeProtectionActivation {
            ProtectionActivationOperation.Record(nowMillis)
        }
    }

    private suspend fun DataStoreSettingsStore.verifyPin(
        pin: String,
        nowMillis: Long = 1_000L,
    ): PinVerificationResult =
        VerifyPinUseCase(
            pinStateStore = this,
            pinHasher = Pbkdf2PinHasher(),
            timeProvider = TimeProvider { nowMillis },
            lockoutClock = PinLockoutClock { PinLockoutTime(nowMillis, 1) },
        )(pin)

    private suspend fun DataStore<Preferences>.writeLegacyPinCredential(hashVersion: Int? = null) {
        edit { preferences ->
            preferences[stringPreferencesKey("pinHash")] = LEGACY_HASH_BASE64
            preferences[stringPreferencesKey("pinSalt")] = LEGACY_SALT_BASE64
            if (hashVersion == null) {
                preferences.remove(intPreferencesKey("pinHashVersion"))
            } else {
                preferences[intPreferencesKey("pinHashVersion")] = hashVersion
            }
        }
    }

    private fun createDataStore(name: String): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = { File(temporaryFolder.root, "$name.preferences_pb").toOkioPath() },
                ),
            scope = scope,
        )
    }

    private class ImmediatePreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private class SuspendingPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            suspendCoroutine { continuation ->
                Thread {
                    try {
                        val updated =
                            runBlocking {
                                transform(state.value)
                            }
                        state.value = updated
                        continuation.resume(updated)
                    } catch (error: Throwable) {
                        continuation.resumeWithException(error)
                    }
                }.start()
            }
    }

    private class FailingPreferencesDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            throw IllegalStateException("edit failed")
    }

    private class SuspendingFailingPreferencesDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            suspendCoroutine { continuation ->
                Thread {
                    continuation.resumeWithException(
                        IllegalStateException("suspending edit failed"),
                    )
                }.start()
            }
    }

    private class FileBackedContext(
        private val root: File,
    ) : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this

        override fun getFilesDir(): File = root
    }

    private suspend fun cancelOpenStores() {
        scopes.forEach { scope ->
            scope.cancel()
            scope.coroutineContext[Job]?.join()
        }
        scopes.clear()
    }

    private companion object {
        const val LEGACY_PIN = "4826"
        const val LEGACY_SALT_BASE64 = "AAECAwQFBgcICQoLDA0ODw=="
        const val LEGACY_HASH_BASE64 = "JJfkH+VveNsEbC4vvoIyDBewEbKwjYEa29445Woz1lY="
    }
}
