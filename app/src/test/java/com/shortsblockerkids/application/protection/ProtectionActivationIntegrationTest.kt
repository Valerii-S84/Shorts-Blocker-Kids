package com.shortsblockerkids.application.protection

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.port.AccessibilityServiceStatusPort
import com.shortsblockerkids.application.port.ProtectionActivationOperation
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.infrastructure.storage.DataStoreSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class ProtectionActivationIntegrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    @Test
    fun allPrerequisitesWriteActivationToDataStore() =
        runBlocking {
            val repository = createConfiguredRepository("all-prerequisites", Scenario())
            val timeProvider = RecordingTimeProvider(NOW_MILLIS)
            val useCase = createUseCase(repository, timeProvider, isAccessibilityEnabled = true)

            val result =
                useCase(ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION)
            val settings = repository.readSettings().first()

            assertEquals(ProtectionActivationResult.ACTIVATED, result)
            assertEquals(1, timeProvider.readCount)
            assertTrue(settings.protectionConfiguration.isEnabled)
            assertEquals(NOW_MILLIS, settings.entitlement.freeTestStartedAtMillis)
            assertEquals(
                FreeTestPolicy.DEFAULT_DURATION_DAYS,
                settings.entitlement.freeTestDurationDays,
            )
        }

    @Test
    fun missingAccessibilityLeavesDataStoreUnchanged() =
        assertNoActivation(
            name = "missing-accessibility",
            scenario = Scenario(isAccessibilityEnabled = false),
        )

    @Test
    fun disabledProtectionLeavesDataStoreUnchanged() =
        assertNoActivation(
            name = "disabled-protection",
            scenario = Scenario(isProtectionEnabled = false),
        )

    @Test
    fun missingDisclosureLeavesDataStoreUnchanged() =
        assertNoActivation(
            name = "missing-disclosure",
            scenario = Scenario(isDisclosureAccepted = false),
        )

    @Test
    fun missingPinLeavesDataStoreUnchanged() =
        assertNoActivation(
            name = "missing-pin",
            scenario = Scenario(isPinConfigured = false),
        )

    @Test
    fun repeatedIntentLeavesDataStoreUnchanged() =
        runBlocking {
            val repository = createConfiguredRepository("repeated-intent", Scenario())
            val timeProvider = RecordingTimeProvider(NOW_MILLIS)
            val useCase = createUseCase(repository, timeProvider, isAccessibilityEnabled = true)
            assertEquals(
                ProtectionActivationResult.ACTIVATED,
                useCase(ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION),
            )
            val settingsAfterActivation = repository.readSettings().first()

            val repeatedResult =
                useCase(ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION)
            val settingsAfterRepeat = repository.readSettings().first()

            assertEquals(ProtectionActivationResult.ALREADY_STARTED, repeatedResult)
            assertEquals(settingsAfterActivation, settingsAfterRepeat)
            assertEquals(1, timeProvider.readCount)
        }

    @Test
    fun repeatedIntentWithMissingPrerequisiteIsRejectedWithoutStorageChange() =
        runBlocking {
            val repository = createConfiguredRepository("repeat-missing", Scenario())
            repository.completeProtectionActivation {
                ProtectionActivationOperation.Record(1_000L)
            }
            val settingsBefore = repository.readSettings().first()
            val timeProvider = RecordingTimeProvider(NOW_MILLIS)
            val useCase = createUseCase(repository, timeProvider, isAccessibilityEnabled = false)

            val result =
                useCase(ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION)
            val settingsAfter = repository.readSettings().first()

            assertEquals(ProtectionActivationResult.PREREQUISITES_NOT_MET, result)
            assertEquals(settingsBefore, settingsAfter)
            assertEquals(0, timeProvider.readCount)
        }

    @Test
    fun enableProtectionIntentCompletesActivationAtomically() =
        runBlocking {
            val repository =
                createConfiguredRepository(
                    name = "enable-protection-intent",
                    scenario = Scenario(isProtectionEnabled = false),
                )
            val timeProvider = RecordingTimeProvider(NOW_MILLIS)
            val useCase = createUseCase(repository, timeProvider, isAccessibilityEnabled = true)

            val result = useCase(ProtectionActivationIntent.ENABLE_PROTECTION)
            val settings = repository.readSettings().first()

            assertEquals(ProtectionActivationResult.ACTIVATED, result)
            assertTrue(settings.protectionConfiguration.isEnabled)
            assertEquals(NOW_MILLIS, settings.entitlement.freeTestStartedAtMillis)
            assertEquals(1, timeProvider.readCount)
        }

    @Test
    fun enableProtectionIntentWithoutAccessibilityLeavesDataStoreUnchanged() =
        assertNoActivation(
            name = "enable-protection-missing-accessibility",
            scenario =
                Scenario(
                    isAccessibilityEnabled = false,
                    isProtectionEnabled = false,
                ),
            intent = ProtectionActivationIntent.ENABLE_PROTECTION,
        )

    @Test
    fun enableProtectionIntentWithoutDisclosureLeavesDataStoreUnchanged() =
        assertNoActivation(
            name = "enable-protection-missing-disclosure",
            scenario =
                Scenario(
                    isProtectionEnabled = false,
                    isDisclosureAccepted = false,
                ),
            intent = ProtectionActivationIntent.ENABLE_PROTECTION,
        )

    @Test
    fun enableProtectionIntentWithoutPinLeavesDataStoreUnchanged() =
        assertNoActivation(
            name = "enable-protection-missing-pin",
            scenario =
                Scenario(
                    isProtectionEnabled = false,
                    isPinConfigured = false,
                ),
            intent = ProtectionActivationIntent.ENABLE_PROTECTION,
        )

    @Test
    fun repeatedEnableProtectionIntentLeavesDataStoreUnchanged() =
        runBlocking {
            val repository =
                createConfiguredRepository(
                    name = "repeated-enable-protection",
                    scenario = Scenario(isProtectionEnabled = false),
                )
            val timeProvider = RecordingTimeProvider(NOW_MILLIS)
            val useCase = createUseCase(repository, timeProvider, isAccessibilityEnabled = true)
            assertEquals(
                ProtectionActivationResult.ACTIVATED,
                useCase(ProtectionActivationIntent.ENABLE_PROTECTION),
            )
            val settingsAfterActivation = repository.readSettings().first()

            val repeatedResult = useCase(ProtectionActivationIntent.ENABLE_PROTECTION)
            val settingsAfterRepeat = repository.readSettings().first()

            assertEquals(ProtectionActivationResult.ALREADY_STARTED, repeatedResult)
            assertEquals(settingsAfterActivation, settingsAfterRepeat)
            assertEquals(1, timeProvider.readCount)
        }

    @Test
    fun repeatedEnableProtectionIntentReenablesProtectionWithoutRewritingActivation() =
        runBlocking {
            val repository =
                createConfiguredRepository(
                    name = "repeated-enable-protection-disabled",
                    scenario = Scenario(isProtectionEnabled = false),
                )
            repository.completeProtectionActivation {
                ProtectionActivationOperation.Record(1_000L)
            }
            repository.setProtectionEnabled(false)
            val timeProvider = RecordingTimeProvider(NOW_MILLIS)
            val useCase = createUseCase(repository, timeProvider, isAccessibilityEnabled = true)

            val result = useCase(ProtectionActivationIntent.ENABLE_PROTECTION)
            val settings = repository.readSettings().first()

            assertEquals(ProtectionActivationResult.ALREADY_STARTED, result)
            assertTrue(settings.protectionConfiguration.isEnabled)
            assertEquals(1_000L, settings.entitlement.freeTestStartedAtMillis)
            assertEquals(0, timeProvider.readCount)
        }

    @Test
    fun concurrentEnableProtectionIntentsRecordExactlyOneActivation() =
        runBlocking {
            val repository =
                createConfiguredRepository(
                    name = "concurrent-enable-protection",
                    scenario = Scenario(isProtectionEnabled = false),
                )
            val timeProvider = RecordingTimeProvider(NOW_MILLIS)
            val useCase = createUseCase(repository, timeProvider, isAccessibilityEnabled = true)

            val results =
                listOf(
                    async(Dispatchers.Default) {
                        useCase(ProtectionActivationIntent.ENABLE_PROTECTION)
                    },
                    async(Dispatchers.Default) {
                        useCase(ProtectionActivationIntent.ENABLE_PROTECTION)
                    },
                ).awaitAll()
            val settings = repository.readSettings().first()

            assertEquals(1, results.count { it == ProtectionActivationResult.ACTIVATED })
            assertEquals(1, results.count { it == ProtectionActivationResult.ALREADY_STARTED })
            assertEquals(1, timeProvider.readCount)
            assertTrue(settings.protectionConfiguration.isEnabled)
            assertEquals(NOW_MILLIS, settings.entitlement.freeTestStartedAtMillis)
        }

    private fun assertNoActivation(
        name: String,
        scenario: Scenario,
        intent: ProtectionActivationIntent =
            ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION,
    ) = runBlocking {
        val repository = createConfiguredRepository(name, scenario)
        val settingsBefore = repository.readSettings().first()
        val timeProvider = RecordingTimeProvider(NOW_MILLIS)
        val useCase =
            createUseCase(
                repository = repository,
                timeProvider = timeProvider,
                isAccessibilityEnabled = scenario.isAccessibilityEnabled,
            )

        val result = useCase(intent)
        val settingsAfter = repository.readSettings().first()

        assertEquals(ProtectionActivationResult.PREREQUISITES_NOT_MET, result)
        assertEquals(settingsBefore, settingsAfter)
        assertEquals(0, timeProvider.readCount)
    }

    private suspend fun createConfiguredRepository(
        name: String,
        scenario: Scenario,
    ): DataStoreSettingsStore {
        val repository = DataStoreSettingsStore(createDataStore(name))
        repository.setProtectionEnabled(scenario.isProtectionEnabled)
        repository.setDisclosureAccepted(scenario.isDisclosureAccepted)
        if (scenario.isPinConfigured) {
            repository.savePinState(
                credential =
                    PinCredential(
                        hashBase64 = "configured-hash",
                        saltBase64 = "configured-salt",
                        hashVersion = 1,
                    ),
                attemptState = PinAttemptState(),
            )
        }
        return repository
    }

    private fun createUseCase(
        repository: DataStoreSettingsStore,
        timeProvider: RecordingTimeProvider,
        isAccessibilityEnabled: Boolean,
    ): RecordSuccessfulProtectionActivationUseCase =
        RecordSuccessfulProtectionActivationUseCase(
            timeProvider = timeProvider,
            accessibilityServiceStatusPort =
                AccessibilityServiceStatusPort { isAccessibilityEnabled },
            protectionActivationStore = repository,
        )

    private fun createDataStore(name: String): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath =
                        { File(temporaryFolder.root, "$name.preferences_pb").toOkioPath() },
                ),
            scope = scope,
        )
    }

    private data class Scenario(
        val isAccessibilityEnabled: Boolean = true,
        val isProtectionEnabled: Boolean = true,
        val isDisclosureAccepted: Boolean = true,
        val isPinConfigured: Boolean = true,
    )

    private class RecordingTimeProvider(
        private val timestampMillis: Long,
    ) : TimeProvider {
        private val reads = AtomicInteger()

        val readCount: Int
            get() = reads.get()

        override fun currentTimeMillis(): Long {
            reads.incrementAndGet()
            return timestampMillis
        }
    }

    private companion object {
        const val NOW_MILLIS = 12_345L
    }
}
