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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.port.PinHashingPort
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

class DataStoreSettingsStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun storeNameAndPreferenceKeysRemainByteCompatible() {
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
                "billing_installation_id",
                "billing_entitlement_state",
                "billing_subscription_active",
                "billing_last_verified_at",
                "billing_active_until_millis",
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
                DataStoreSettingsMapper.billingInstallationIdKey.name,
                DataStoreSettingsMapper.billingEntitlementStateKey.name,
                DataStoreSettingsMapper.billingSubscriptionActiveKey.name,
                DataStoreSettingsMapper.billingLastVerifiedAtKey.name,
                DataStoreSettingsMapper.billingActiveUntilMillisKey.name,
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
    fun persistsBillingEntitlementSnapshot() =
        runBlocking {
            val repository = createRepository("billing")

            repository.updateBillingEntitlement(isActive = true, checkedAtMillis = 4_000L)
            val activeSettings = repository.readSettings().first()
            val activeStoredSettings = repository.readStoredSettings().first()

            assertTrue(activeStoredSettings.billingSubscriptionActive)
            assertEquals(4_000L, activeSettings.entitlement.paidLastVerifiedAtMillis)
            assertTrue(activeSettings.hasBillingEntitlement(nowMillis = 4_000L))

            repository.updateBillingEntitlement(isActive = false, checkedAtMillis = 5_000L)
            val inactiveSettings = repository.readSettings().first()
            val inactiveStoredSettings = repository.readStoredSettings().first()

            assertFalse(inactiveStoredSettings.billingSubscriptionActive)
            assertEquals(5_000L, inactiveSettings.entitlement.paidLastVerifiedAtMillis)
            assertFalse(inactiveSettings.hasBillingEntitlement(nowMillis = 5_000L))
        }

    @Test
    fun persistsBackendBillingEntitlementLifecycleState() =
        runBlocking {
            val repository = createRepository("billing-state")

            repository.updateBillingEntitlement(
                BillingEntitlementSnapshot(
                    state = BillingEntitlementState.CANCELED_ACTIVE,
                    checkedAtMillis = 4_000L,
                    activeUntilMillis = 8_000L,
                ),
            )
            val settings = repository.readSettings().first()
            val storedSettings = repository.readStoredSettings().first()

            assertTrue(storedSettings.billingSubscriptionActive)
            assertEquals(
                BillingEntitlementState.CANCELED_ACTIVE,
                storedSettings.billingEntitlementState,
            )
            assertEquals(8_000L, settings.entitlement.paidActiveUntilMillis)
            assertTrue(settings.hasBillingEntitlement(nowMillis = 4_500L))
        }

    @Test
    fun createsStableRandomBillingInstallationId() =
        runBlocking {
            val firstRepository = createRepository("billing-install")
            val firstInstallId = firstRepository.getOrCreateBillingInstallationId()
            val sameInstallId = firstRepository.getOrCreateBillingInstallationId()
            cancelOpenStores()

            val restartedRepository = createRepository("billing-install")
            val restartedInstallId = restartedRepository.getOrCreateBillingInstallationId()

            assertTrue(firstInstallId.isNotBlank())
            assertEquals(firstInstallId, sameInstallId)
            assertEquals(firstInstallId, restartedInstallId)
        }

    @Test
    fun blankBillingInstallationIdIsReplacedWithStableRandomId() =
        runBlocking {
            val dataStore = createDataStore("blank-billing-install")
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("billing_installation_id")] = " "
            }
            val repository = DataStoreSettingsStore(dataStore)

            val installId = repository.getOrCreateBillingInstallationId()

            assertTrue(installId.isNotBlank())
            assertEquals(installId, repository.readStoredSettings().first().billingInstallationId)
            assertEquals(installId, repository.getOrCreateBillingInstallationId())
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

    private fun cancelOpenStores() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    private companion object {
        const val LEGACY_PIN = "4826"
        const val LEGACY_SALT_BASE64 = "AAECAwQFBgcICQoLDA0ODw=="
        const val LEGACY_HASH_BASE64 = "JJfkH+VveNsEbC4vvoIyDBewEbKwjYEa29445Woz1lY="
    }
}
