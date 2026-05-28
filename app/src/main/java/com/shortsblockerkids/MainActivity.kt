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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shortsblockerkids.accessibility.AccessibilityServiceStatus
import com.shortsblockerkids.core.billing.BillingUiState
import com.shortsblockerkids.core.billing.HttpBillingBackendClient
import com.shortsblockerkids.core.billing.PlayBillingRepository
import com.shortsblockerkids.core.storage.AppSettings
import com.shortsblockerkids.core.storage.SettingsRepository
import com.shortsblockerkids.feature.blocking.TemporaryAllowCompletion
import com.shortsblockerkids.feature.blocking.TemporaryAllowFlowController
import com.shortsblockerkids.feature.blocking.TemporaryAllowScreen
import com.shortsblockerkids.feature.dashboard.DashboardScreen
import com.shortsblockerkids.feature.debug.DetectorPlaygroundScreen
import com.shortsblockerkids.feature.onboarding.AccessibilityDisclosureDecision
import com.shortsblockerkids.feature.onboarding.AccessibilityDisclosureScreen
import com.shortsblockerkids.feature.onboarding.AccessibilityPermissionFlow
import com.shortsblockerkids.feature.onboarding.AccessibilitySettingsRequest
import com.shortsblockerkids.feature.onboarding.AccessibilitySetupDestination
import com.shortsblockerkids.feature.onboarding.EnableAccessibilityScreen
import com.shortsblockerkids.feature.onboarding.WelcomeScreen
import com.shortsblockerkids.feature.pin.PinEntryScreen
import com.shortsblockerkids.feature.pin.PinSetupScreen
import com.shortsblockerkids.feature.privacy.PrivacyPolicyScreen
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
    private lateinit var billingRepository: PlayBillingRepository
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsState = mutableStateOf(AppSettings())
    private val accessibilityEnabledState = mutableStateOf(false)
    private val temporaryAllowRequestState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(this)
        billingRepository =
            PlayBillingRepository(
                context = this,
                onEntitlementChanged = { snapshot ->
                    activityScope.launch {
                        settingsRepository.updateBillingEntitlement(snapshot)
                    }
                },
                billingBackendClient =
                    HttpBillingBackendClient.fromBaseUrl(BuildConfig.BILLING_BACKEND_BASE_URL),
                installId = runBlocking { settingsRepository.getOrCreateBillingInstallationId() },
                appVersion = BuildConfig.VERSION_NAME,
                clientOnlyModeRequested = BuildConfig.BILLING_CLIENT_ONLY_TEST_MODE,
                internalTestingBuild = BuildConfig.DEBUG,
                billingScope = activityScope,
            )
        temporaryAllowRequestState.value = intent.isTemporaryAllowRequest()
        loadInitialSettings()
        observeSettings()
        refreshState()

        setContent {
            ShortsBlockerKidsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val billingUiState by billingRepository.uiState.collectAsState()
                    ShortsBlockerKidsApp(
                        settings = settingsState.value,
                        isAccessibilityServiceEnabled = accessibilityEnabledState.value,
                        isTemporaryAllowRequested = temporaryAllowRequestState.value,
                        billingUiState = billingUiState,
                        repository = settingsRepository,
                        onSubscribe = {
                            billingRepository.launchPurchase(this@MainActivity)
                        },
                        onRestorePurchases = {
                            billingRepository.refreshPurchases()
                        },
                        onManageSubscription = {
                            billingRepository.openManageSubscription(this@MainActivity)
                        },
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onStateChanged = ::refreshState,
                        onTemporaryAllowFlowClosed = ::closeTemporaryAllowRequest,
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
        if (::billingRepository.isInitialized) {
            billingRepository.start()
        }
    }

    override fun onDestroy() {
        if (::billingRepository.isInitialized) {
            billingRepository.stop()
        }
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
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun closeTemporaryAllowRequest() {
        finishAndRemoveTask()
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
    isTemporaryAllowRequested: Boolean,
    billingUiState: BillingUiState,
    repository: SettingsRepository,
    onSubscribe: () -> Unit,
    onRestorePurchases: () -> Unit,
    onManageSubscription: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onStateChanged: () -> Unit,
    onTemporaryAllowFlowClosed: () -> Unit,
    onTemporaryAllowRequestConsumed: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val temporaryAllowFlowController =
        TemporaryAllowFlowController { minutes ->
            repository.setTemporaryAllowForMinutes(minutes)
        }
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
                            screen =
                                AccessibilityPermissionFlow
                                    .destinationAfterPinCreated()
                                    .toAppScreen()
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
                            screen =
                                AccessibilityPermissionFlow
                                    .destinationAfterDisclosure(
                                        AccessibilityDisclosureDecision.Accepted,
                                    ).toAppScreen()
                        }
                    },
                    onDecline = {
                        screen =
                            AccessibilityPermissionFlow
                                .destinationAfterDisclosure(
                                    AccessibilityDisclosureDecision.Declined,
                                ).toAppScreen()
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
                    billingUiState = billingUiState,
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
                    onPlatformEnabledChanged = { platformId, enabled ->
                        coroutineScope.launch {
                            repository.setPlatformEnabled(platformId, enabled)
                            onStateChanged()
                        }
                    },
                    onSubscribe = onSubscribe,
                    onRestorePurchases = onRestorePurchases,
                    onManageSubscription = onManageSubscription,
                    onOpenAccessibilitySettings = {
                        when (
                            AccessibilityPermissionFlow.settingsRequest(
                                settings.accessibilityDisclosureAccepted,
                            )
                        ) {
                            AccessibilitySettingsRequest.ShowDisclosure -> {
                                screen = AppScreen.AccessibilityDisclosure
                            }

                            AccessibilitySettingsRequest.OpenSystemSettings -> {
                                onOpenAccessibilitySettings()
                            }
                        }
                    },
                    onOpenPrivacyPolicy = {
                        screen = AppScreen.PrivacyPolicy
                    },
                    onOpenDebugQa =
                        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
                            { screen = AppScreen.DetectorQa }
                        } else {
                            null
                        },
                )

            AppScreen.PrivacyPolicy ->
                PrivacyPolicyScreen(
                    onBack = {
                        screen = AppScreen.Dashboard
                    },
                )

            AppScreen.TemporaryAllow ->
                TemporaryAllowScreen(
                    onDurationSelected = { minutes ->
                        coroutineScope.launch {
                            val completion = temporaryAllowFlowController.selectDuration(minutes)
                            pendingTemporaryAllow = false
                            pendingProtectionDisable = false
                            onStateChanged()
                            handleTemporaryAllowCompletion(
                                completion = completion,
                                onTemporaryAllowFlowClosed = onTemporaryAllowFlowClosed,
                            )
                        }
                    },
                    onCancel = {
                        val completion = temporaryAllowFlowController.cancel()
                        pendingTemporaryAllow = false
                        pendingProtectionDisable = false
                        handleTemporaryAllowCompletion(
                            completion = completion,
                            onTemporaryAllowFlowClosed = onTemporaryAllowFlowClosed,
                        )
                    },
                )

            AppScreen.DetectorQa ->
                DetectorPlaygroundScreen(
                    settings = settings,
                    isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                    billingUiState = billingUiState,
                    onBack = {
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
): AppScreen =
    AccessibilityPermissionFlow
        .destinationAfterParentUnlock(settings, pendingTemporaryAllow)
        .toAppScreen()

private fun AccessibilitySetupDestination.toAppScreen(): AppScreen =
    when (this) {
        AccessibilitySetupDestination.Disclosure -> AppScreen.AccessibilityDisclosure
        AccessibilitySetupDestination.EnableAccessibility -> AppScreen.EnableAccessibility
        AccessibilitySetupDestination.Dashboard -> AppScreen.Dashboard
        AccessibilitySetupDestination.TemporaryAllow -> AppScreen.TemporaryAllow
    }

private fun handleTemporaryAllowCompletion(
    completion: TemporaryAllowCompletion,
    onTemporaryAllowFlowClosed: () -> Unit,
) {
    when (completion) {
        TemporaryAllowCompletion.ReturnToForegroundApp -> onTemporaryAllowFlowClosed()
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
    PrivacyPolicy,
    TemporaryAllow,
    DetectorQa,
}
