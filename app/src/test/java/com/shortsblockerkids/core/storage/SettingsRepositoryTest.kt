package com.shortsblockerkids.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.shortsblockerkids.core.billing.BillingEntitlementSnapshot
import com.shortsblockerkids.core.billing.BillingEntitlementState
import com.shortsblockerkids.core.entitlement.FreeTestPolicy
import com.shortsblockerkids.core.model.ProtectionMode
import com.shortsblockerkids.core.security.PinVerificationResult
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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun createsPinHashAndVerifiesPinWithoutPlainTextStorage() =
        runBlocking {
            val repository = createRepository("pin")

            repository.savePin("4826")
            val settings = repository.readSettings().first()

            assertTrue(settings.isPinCreated)
            assertNotNull(settings.pinSalt)
            assertFalse(settings.pinHash.orEmpty().contains("4826"))
            assertEquals(PinVerificationResult.Success, repository.verifyPin("4826"))
            assertTrue(repository.verifyPin("4827") is PinVerificationResult.Failure)
        }

    @Test
    fun freeTestStartsOnlyAfterProtectionActivation() =
        runBlocking {
            val repository = createRepository("free-test-start")

            assertEquals(null, repository.readSettings().first().freeTestStartedAt)

            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.setProtectionEnabled(true)

            assertEquals(null, repository.readSettings().first().freeTestStartedAt)

            repository.recordSuccessfulProtectionActivation(nowMillis = 5_000L)
            val settings = repository.readSettings().first()

            assertEquals(5_000L, settings.freeTestStartedAt)
            assertEquals(FreeTestPolicy.DEFAULT_DURATION_DAYS, settings.freeTestDurationDays)
        }

    @Test
    fun appRestartDoesNotResetFreeTestTimer() =
        runBlocking {
            val firstRepository = createRepository("free-test-restart")
            firstRepository.recordSuccessfulProtectionActivation(nowMillis = 5_000L)
            cancelOpenStores()

            val restartedRepository = createRepository("free-test-restart")
            restartedRepository.recordSuccessfulProtectionActivation(nowMillis = 10_000L)
            val settings = restartedRepository.readSettings().first()

            assertEquals(5_000L, settings.freeTestStartedAt)
            assertEquals(FreeTestPolicy.DEFAULT_DURATION_DAYS, settings.freeTestDurationDays)
        }

    @Test
    fun persistsProtectionStateAndTemporaryAllowExpiry() =
        runBlocking {
            val repository = createRepository("settings")

            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.recordSuccessfulProtectionActivation(nowMillis = 1_000L)
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

            assertEquals(ProtectionMode.BLOCK_SHORTS, repository.readSettings().first().selectedMode)
        }

    @Test
    fun persistsEnabledProtectedAppsAcrossRestart() =
        runBlocking {
            val firstRepository = createRepository("enabled-platforms")

            firstRepository.setPlatformEnabled(AppSettings.TIKTOK_PLATFORM_ID, false)
            firstRepository.setPlatformEnabled(AppSettings.FACEBOOK_REELS_PLATFORM_ID, false)
            val firstSettings = firstRepository.readSettings().first()

            assertFalse(firstSettings.isPlatformEnabled(AppSettings.TIKTOK_PLATFORM_ID))
            assertFalse(firstSettings.isPlatformEnabled(AppSettings.FACEBOOK_REELS_PLATFORM_ID))
            assertTrue(firstSettings.isPlatformEnabled(AppSettings.YOUTUBE_SHORTS_PLATFORM_ID))
            cancelOpenStores()

            val restartedRepository = createRepository("enabled-platforms")
            val restartedSettings = restartedRepository.readSettings().first()

            assertEquals(firstSettings.enabledPlatformIds, restartedSettings.enabledPlatformIds)
        }

    @Test
    fun disablingEveryProtectedAppPreventsProtectionUntilOneIsEnabledAgain() =
        runBlocking {
            val repository = createRepository("all-platforms-disabled")
            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.recordSuccessfulProtectionActivation(nowMillis = 1_000L)

            AppSettings.DEFAULT_ENABLED_PLATFORM_IDS.forEach { platformId ->
                repository.setPlatformEnabled(platformId, false)
            }
            val disabledSettings = repository.readSettings().first()

            assertEquals(emptySet<String>(), disabledSettings.enabledPlatformIds)
            assertFalse(disabledSettings.canProtect(nowMillis = 1_500L))

            repository.setPlatformEnabled(AppSettings.INSTAGRAM_REELS_PLATFORM_ID, true)
            val reenabledSettings = repository.readSettings().first()

            assertEquals(setOf(AppSettings.INSTAGRAM_REELS_PLATFORM_ID), reenabledSettings.enabledPlatformIds)
            assertTrue(reenabledSettings.canProtect(nowMillis = 1_500L))
        }

    @Test
    fun unknownStoredPlatformIdsAreIgnored() =
        runBlocking {
            val dataStore = createDataStore("unknown-platform")
            dataStore.edit { preferences ->
                preferences[stringSetPreferencesKey("enabledPlatformIds")] =
                    setOf(AppSettings.YOUTUBE_SHORTS_PLATFORM_ID, "unknown_platform")
            }
            val repository = SettingsRepository(dataStore)

            assertEquals(
                setOf(AppSettings.YOUTUBE_SHORTS_PLATFORM_ID),
                repository.readSettings().first().enabledPlatformIds,
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

            assertTrue(activeSettings.billingSubscriptionActive)
            assertEquals(4_000L, activeSettings.billingLastVerifiedAt)
            assertTrue(activeSettings.hasBillingEntitlement(nowMillis = 4_000L))

            repository.updateBillingEntitlement(isActive = false, checkedAtMillis = 5_000L)
            val inactiveSettings = repository.readSettings().first()

            assertFalse(inactiveSettings.billingSubscriptionActive)
            assertEquals(5_000L, inactiveSettings.billingLastVerifiedAt)
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

            assertTrue(settings.billingSubscriptionActive)
            assertEquals(BillingEntitlementState.CANCELED_ACTIVE, settings.billingEntitlementState)
            assertEquals(8_000L, settings.billingActiveUntilMillis)
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
            val repository = SettingsRepository(dataStore)

            val installId = repository.getOrCreateBillingInstallationId()

            assertTrue(installId.isNotBlank())
            assertEquals(installId, repository.readSettings().first().billingInstallationId)
            assertEquals(installId, repository.getOrCreateBillingInstallationId())
        }

    @Test
    fun validPinTemporaryAllowSelectionStoresExpiryForFiveTenAndFifteenMinutes() =
        runBlocking {
            val repository = createRepository("allow-durations")
            val nowMillis = 1_000L
            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.recordSuccessfulProtectionActivation(nowMillis = nowMillis)

            assertEquals(PinVerificationResult.Success, repository.verifyPin("4826"))

            listOf(5, 10, 15).forEach { minutes ->
                repository.setTemporaryAllowForMinutes(minutes, nowMillis)
                val settings = repository.readSettings().first()

                assertEquals(
                    nowMillis + minutes * 60_000L,
                    settings.temporaryAllowUntil,
                )
                assertFalse(settings.canProtect(nowMillis = nowMillis + 1L))
            }
        }

    @Test
    fun clearsExpiredTemporaryAllowBeforeClockRollbackCanReactivateIt() =
        runBlocking {
            val repository = createRepository("expired-allow")

            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.recordSuccessfulProtectionActivation(nowMillis = 1_000L)
            repository.setTemporaryAllowUntil(2_000L)

            repository.clearExpiredTemporaryAllow(nowMillis = 2_500L)
            val settings = repository.readSettings().first()

            assertEquals(null, settings.temporaryAllowUntil)
            assertTrue(settings.canProtect(nowMillis = 1_500L))
        }

    @Test
    fun clearExpiredTemporaryAllowLeavesMissingOrActiveAllowUntouched() =
        runBlocking {
            val repository = createRepository("clear-allow-no-op")

            repository.clearExpiredTemporaryAllow(nowMillis = 2_500L)
            assertEquals(null, repository.readSettings().first().temporaryAllowUntil)

            repository.setTemporaryAllowUntil(3_000L)
            repository.clearExpiredTemporaryAllow(nowMillis = 2_500L)

            assertEquals(3_000L, repository.readSettings().first().temporaryAllowUntil)
        }

    @Test
    fun clearingTemporaryAllowRemovesStoredExpiry() =
        runBlocking {
            val repository = createRepository("clear-allow")

            repository.setTemporaryAllowUntil(3_000L)
            repository.setTemporaryAllowUntil(null)

            assertEquals(null, repository.readSettings().first().temporaryAllowUntil)
        }

    @Test
    fun rejectsNonPositiveTemporaryAllowDurations() {
        val repository = createRepository("invalid-allow-duration")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.setTemporaryAllowForMinutes(0, nowMillis = 1_000L)
            }
        }
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
            assertEquals(PinVerificationResult.Locked(untilMillis = 32_000L), fifth)
            assertEquals(5, repository.readSettings().first().failedPinAttempts)

            val sixth = repository.verifyPin("1111", nowMillis = 32_001L)
            assertEquals(PinVerificationResult.Locked(untilMillis = 92_001L), sixth)
            assertEquals(6, repository.readSettings().first().failedPinAttempts)
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
            val lockedBeforeRetry = repository.readSettings().first()

            assertEquals(
                PinVerificationResult.Locked(untilMillis = 31_004L),
                repository.verifyPin("4826", nowMillis = 2_000L),
            )
            assertEquals(lockedBeforeRetry.failedPinAttempts, repository.readSettings().first().failedPinAttempts)
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
            repository.recordSuccessfulProtectionActivation(nowMillis = 1_000L)

            assertTrue(repository.verifyPin("4827") is PinVerificationResult.Failure)
            val settings = repository.readSettings().first()

            assertEquals(null, settings.temporaryAllowUntil)
            assertTrue(settings.canProtect(nowMillis = 1_500L))
        }

    @Test
    fun emptyPinDoesNotUnlockBlockingFlow() =
        runBlocking {
            val repository = createRepository("empty-pin-unlock")

            repository.savePin("4826")

            assertTrue(repository.verifyPin("") is PinVerificationResult.Failure)
        }

    @Test
    fun repeatedEmptyPinAttemptsKeepProtectionActiveWithoutTemporaryAllow() =
        runBlocking {
            val repository = createRepository("empty-pin-protection")
            repository.savePin("4826")
            repository.setDisclosureAccepted(true)
            repository.recordSuccessfulProtectionActivation(nowMillis = 1_000L)

            repeat(5) { attempt ->
                repository.verifyPin("", nowMillis = 2_000L + attempt)
            }
            val settings = repository.readSettings().first()

            assertEquals(null, settings.temporaryAllowUntil)
            assertTrue(settings.protectionEnabled)
            assertTrue(settings.canProtect(nowMillis = 2_500L))
        }

    @Test
    fun emptyPinLockoutRejectsCorrectPinUntilLockoutExpires() =
        runBlocking {
            val repository = createRepository("empty-pin-lockout")
            repository.savePin("4826")

            repeat(5) { attempt ->
                repository.verifyPin("", nowMillis = 1_000L + attempt)
            }

            assertTrue(repository.verifyPin("4826", nowMillis = 2_000L) is PinVerificationResult.Locked)
        }

    @Test
    fun partialPinDoesNotUnlockBlockingFlow() =
        runBlocking {
            val repository = createRepository("partial-pin-unlock")

            repository.savePin("4826")

            assertTrue(repository.verifyPin("482") is PinVerificationResult.Failure)
        }

    @Test
    fun corruptedPinMetadataDoesNotCrashVerification() =
        runBlocking {
            val dataStore = createDataStore("corrupted-pin")
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("pinHash")] = "not-base64"
                preferences[stringPreferencesKey("pinSalt")] = "not-base64"
            }
            val repository = SettingsRepository(dataStore)

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

    private fun createRepository(name: String): SettingsRepository {
        val dataStore = createDataStore(name)
        return SettingsRepository(dataStore)
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
}
