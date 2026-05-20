package com.shortsblockerkids

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shortsblockerkids.BuildConfig
import com.shortsblockerkids.accessibility.AccessibilityServiceStatus
import com.shortsblockerkids.accessibility.RuntimeProtectionState
import com.shortsblockerkids.core.storage.AppSettings
import com.shortsblockerkids.core.storage.SettingsRepository
import com.shortsblockerkids.feature.blocking.TemporaryAllowScreen
import com.shortsblockerkids.feature.dashboard.DashboardScreen
import com.shortsblockerkids.feature.debug.DetectorPlaygroundScreen
import com.shortsblockerkids.feature.onboarding.AccessibilityDisclosureScreen
import com.shortsblockerkids.feature.onboarding.EnableAccessibilityScreen
import com.shortsblockerkids.feature.onboarding.WelcomeScreen
import com.shortsblockerkids.feature.pin.PinEntryScreen
import com.shortsblockerkids.feature.pin.PinSetupScreen
import com.shortsblockerkids.ui.theme.ShortsBlockerKidsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsState = mutableStateOf(AppSettings())
    private val accessibilityEnabledState = mutableStateOf(false)
    private val lastDetectorResultState = mutableStateOf<String?>(null)
    private val temporaryAllowRequestState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(this)
        temporaryAllowRequestState.value = intent.isTemporaryAllowRequest()
        loadInitialSettings()
        observeSettings()
        refreshState()

        setContent {
            ShortsBlockerKidsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShortsBlockerKidsApp(
                        settings = settingsState.value,
                        isAccessibilityServiceEnabled = accessibilityEnabledState.value,
                        lastDetectorResult = lastDetectorResultState.value,
                        isTemporaryAllowRequested = temporaryAllowRequestState.value,
                        repository = settingsRepository,
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onStateChanged = ::refreshState,
                        onTemporaryAllowRequestConsumed = {
                            temporaryAllowRequestState.value = false
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.isTemporaryAllowRequest()) {
            temporaryAllowRequestState.value = true
        }
        refreshState()
    }

    override fun onResume() {
        super.onResume()
        if (::settingsRepository.isInitialized) {
            refreshState()
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun observeSettings() {
        activityScope.launch {
            settingsRepository.readSettings().collect { settings ->
                val nowMillis = System.currentTimeMillis()
                if (settings.hasExpiredTemporaryAllow(nowMillis)) {
                    settingsRepository.clearExpiredTemporaryAllow(nowMillis)
                    settingsState.value = settings.copy(temporaryAllowUntil = null)
                } else {
                    settingsState.value = settings
                }
            }
        }
    }

    private fun loadInitialSettings() {
        val initialSettings =
            runBlocking {
                settingsRepository.readSettings().first()
            }
        settingsState.value = initialSettings
    }

    private fun refreshState() {
        accessibilityEnabledState.value = AccessibilityServiceStatus.isEnabled(this)
        lastDetectorResultState.value = RuntimeProtectionState.lastDetectorResultText()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun Intent?.isTemporaryAllowRequest(): Boolean = this?.getBooleanExtra(EXTRA_OPEN_TEMPORARY_ALLOW_PIN, false) == true

    companion object {
        const val EXTRA_OPEN_TEMPORARY_ALLOW_PIN = "com.shortsblockerkids.OPEN_TEMPORARY_ALLOW_PIN"
    }
}

@Composable
private fun ShortsBlockerKidsApp(
    settings: AppSettings,
    isAccessibilityServiceEnabled: Boolean,
    lastDetectorResult: String?,
    isTemporaryAllowRequested: Boolean,
    repository: SettingsRepository,
    onOpenAccessibilitySettings: () -> Unit,
    onStateChanged: () -> Unit,
    onTemporaryAllowRequestConsumed: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var screen by rememberSaveable {
        mutableStateOf(initialScreen(settings))
    }
    var isUnlocked by rememberSaveable {
        mutableStateOf(!settings.isPinCreated)
    }
    var pendingTemporaryAllow by rememberSaveable {
        mutableStateOf(false)
    }
    var pendingProtectionDisable by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(settings.isPinCreated, isUnlocked) {
        if (settings.isPinCreated && !isUnlocked) {
            screen = AppScreen.PinEntry
        }
    }

    LaunchedEffect(isTemporaryAllowRequested, settings.isPinCreated) {
        if (isTemporaryAllowRequested && settings.isPinCreated) {
            pendingTemporaryAllow = true
            pendingProtectionDisable = false
            isUnlocked = false
            screen = AppScreen.PinEntry
            onTemporaryAllowRequestConsumed()
        }
    }

    LaunchedEffect(
        screen,
        isAccessibilityServiceEnabled,
        settings.freeTestStartedAt,
        settings.protectionEnabled,
        settings.accessibilityDisclosureAccepted,
        settings.isPinCreated,
    ) {
        if (
            screen == AppScreen.Dashboard &&
            settings.shouldRecordProtectionActivation(isAccessibilityServiceEnabled)
        ) {
            repository.recordSuccessfulProtectionActivation()
            onStateChanged()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            AppScreen.Welcome ->
                WelcomeScreen(
                    onStart = { screen = AppScreen.PinSetup },
                )

            AppScreen.PinSetup ->
                PinSetupScreen(
                    onPinCreated = { pin ->
                        coroutineScope.launch {
                            repository.savePin(pin)
                            isUnlocked = true
                            onStateChanged()
                            screen = AppScreen.AccessibilityDisclosure
                        }
                    },
                )

            AppScreen.PinEntry ->
                PinEntryScreen(
                    onVerifyPin = { pin ->
                        val result = repository.verifyPin(pin)
                        onStateChanged()
                        result
                    },
                    onUnlocked = {
                        isUnlocked = true
                        if (pendingProtectionDisable) {
                            coroutineScope.launch {
                                repository.setProtectionEnabled(false)
                                pendingProtectionDisable = false
                                onStateChanged()
                                screen = AppScreen.Dashboard
                            }
                        } else {
                            screen = unlockedDestination(settings, pendingTemporaryAllow)
                        }
                    },
                )

            AppScreen.AccessibilityDisclosure ->
                AccessibilityDisclosureScreen(
                    onAccept = {
                        coroutineScope.launch {
                            repository.acceptAccessibilityDisclosure()
                            onStateChanged()
                            screen = AppScreen.EnableAccessibility
                        }
                    },
                )

            AppScreen.EnableAccessibility ->
                EnableAccessibilityScreen(
                    isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onEnabled = {
                        onStateChanged()
                        if (isAccessibilityServiceEnabled) {
                            coroutineScope.launch {
                                repository.recordSuccessfulProtectionActivation()
                                onStateChanged()
                                screen = AppScreen.Dashboard
                            }
                        }
                    },
                )

            AppScreen.Dashboard ->
                DashboardScreen(
                    settings = settings,
                    isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                    lastDetectorResult = lastDetectorResult,
                    onOpenDetectorPlayground =
                        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
                            { screen = AppScreen.DetectorPlayground }
                        } else {
                            null
                        },
                    onProtectionChanged = { enabled ->
                        if (enabled) {
                            coroutineScope.launch {
                                if (isAccessibilityServiceEnabled) {
                                    repository.recordSuccessfulProtectionActivation()
                                } else {
                                    repository.setProtectionEnabled(true)
                                }
                                onStateChanged()
                            }
                        } else {
                            pendingProtectionDisable = true
                            pendingTemporaryAllow = false
                            isUnlocked = false
                            screen = AppScreen.PinEntry
                        }
                    },
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                )

            AppScreen.DetectorPlayground ->
                DetectorPlaygroundScreen(
                    onBack = {
                        onStateChanged()
                        screen = AppScreen.Dashboard
                    },
                )

            AppScreen.TemporaryAllow ->
                TemporaryAllowScreen(
                    onDurationSelected = { minutes ->
                        coroutineScope.launch {
                            repository.setTemporaryAllowForMinutes(minutes)
                            pendingTemporaryAllow = false
                            pendingProtectionDisable = false
                            screen = AppScreen.Dashboard
                        }
                    },
                    onCancel = {
                        pendingTemporaryAllow = false
                        pendingProtectionDisable = false
                        screen = AppScreen.Dashboard
                    },
                )
        }
    }
}

private fun initialScreen(settings: AppSettings): AppScreen {
    if (!settings.isPinCreated) {
        return AppScreen.Welcome
    }

    return AppScreen.PinEntry
}

private fun unlockedDestination(
    settings: AppSettings,
    pendingTemporaryAllow: Boolean,
): AppScreen {
    if (pendingTemporaryAllow) {
        return AppScreen.TemporaryAllow
    }

    return if (settings.accessibilityDisclosureAccepted) {
        AppScreen.Dashboard
    } else {
        AppScreen.AccessibilityDisclosure
    }
}

private fun AppSettings.shouldRecordProtectionActivation(isAccessibilityServiceEnabled: Boolean): Boolean =
    isAccessibilityServiceEnabled &&
        protectionEnabled &&
        accessibilityDisclosureAccepted &&
        isPinCreated &&
        freeTestStartedAt == null

private enum class AppScreen {
    Welcome,
    PinSetup,
    PinEntry,
    AccessibilityDisclosure,
    EnableAccessibility,
    Dashboard,
    DetectorPlayground,
    TemporaryAllow,
}
