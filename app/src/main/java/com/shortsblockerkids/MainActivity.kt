package com.shortsblockerkids

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shortsblockerkids.application.billing.SyncBillingEntitlementUseCase
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.port.AccessibilityServiceStatusPort
import com.shortsblockerkids.application.protection.RecordSuccessfulProtectionActivationUseCase
import com.shortsblockerkids.application.protection.SetTemporaryAllowUseCase
import com.shortsblockerkids.application.settings.ObserveSettingsUseCase
import com.shortsblockerkids.application.settings.SettingsReadState
import com.shortsblockerkids.composition.dashboard.DashboardUiStateAssembler
import com.shortsblockerkids.infrastructure.billing.GooglePlayBillingGateway
import com.shortsblockerkids.infrastructure.security.Pbkdf2PinHasher
import com.shortsblockerkids.infrastructure.storage.DataStoreSettingsStore
import com.shortsblockerkids.infrastructure.time.AndroidPinLockoutClock
import com.shortsblockerkids.infrastructure.time.SystemTimeProvider
import com.shortsblockerkids.platform.accessibility.diagnostics.accessibilityDiagnostics
import com.shortsblockerkids.platform.accessibility.status.AccessibilityServiceStatus
import com.shortsblockerkids.platform.locale.AppLanguage
import com.shortsblockerkids.platform.tamper.TamperProtectionStatus
import com.shortsblockerkids.presentation.app.ParentalSettingsSession
import com.shortsblockerkids.presentation.app.ShortsBlockerKidsApp
import com.shortsblockerkids.presentation.billing.BillingCoordinator
import com.shortsblockerkids.presentation.language.LanguagePicker
import com.shortsblockerkids.ui.theme.ShortsBlockerKidsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.localizedContext(newBase))
    }

    private lateinit var settingsStore: DataStoreSettingsStore
    internal lateinit var parentalSettingsSession: ParentalSettingsSession
        private set
    private lateinit var billingCoordinator: BillingCoordinator
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsState = mutableStateOf<AppSettingsSnapshot?>(null)
    private val settingsReadState = mutableStateOf<SettingsReadState>(SettingsReadState.Loading)
    private var settingsJob: Job? = null
    private val accessibilityEnabledState = mutableStateOf(false)
    private val tamperProtectionEnabledState = mutableStateOf(false)
    private val temporaryAllowRequestState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = DataStoreSettingsStore(this)
        val timeProvider = SystemTimeProvider()
        val pinHasher = Pbkdf2PinHasher()
        val createPinUseCase =
            CreatePinUseCase(
                pinStateStore = settingsStore,
                pinHasher = pinHasher,
            )
        val verifyPinUseCase =
            VerifyPinUseCase(
                pinStateStore = settingsStore,
                pinHasher = pinHasher,
                timeProvider = timeProvider,
                lockoutClock = AndroidPinLockoutClock(this),
            )
        val dashboardUiStateAssembler = DashboardUiStateAssembler(timeProvider, deadlineClock = AndroidPinLockoutClock(this))
        parentalSettingsSession =
            ParentalSettingsSession(
                createPin = createPinUseCase::invoke,
                verifyPin = verifyPinUseCase::invoke,
                writePlatform = settingsStore::setPlatformEnabled,
            )
        val protectionActivationUseCase =
            RecordSuccessfulProtectionActivationUseCase(
                timeProvider = timeProvider,
                accessibilityServiceStatusPort =
                    AccessibilityServiceStatusPort {
                        AccessibilityServiceStatus.isEnabled(applicationContext)
                    },
                protectionActivationStore = settingsStore,
            )
        val setTemporaryAllowUseCase =
            SetTemporaryAllowUseCase(
                timeProvider = timeProvider,
                temporaryAllowStore = settingsStore,
                deadlineClock = AndroidPinLockoutClock(this),
            )
        temporaryAllowRequestState.value = intent.isTemporaryAllowRequest()
        observeSettings()
        refreshState()

        setContent {
            ShortsBlockerKidsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (settingsReadState.value !is SettingsReadState.Ready) {
                        Column {
                            Text(
                                stringResource(
                                    if (settingsReadState.value is SettingsReadState.Loading) {
                                        R.string.settings_loading
                                    } else {
                                        R.string.settings_unavailable
                                    },
                                ),
                            )
                            val failure = settingsReadState.value as? SettingsReadState.Unavailable
                            if (failure != null && !failure.retryScheduled) {
                                Button(onClick = ::observeSettings) {
                                    Text(stringResource(R.string.settings_retry))
                                }
                            }
                        }
                        return@Surface
                    }
                    val billingUiState by billingCoordinator.uiState.collectAsState()
                    ShortsBlockerKidsApp(
                        languagePicker = {
                            LanguagePicker(AppLanguage.currentTag(this)) { tag ->
                                AppLanguage.select(this, tag)
                            }
                        },
                        dashboardUiStateProvider = {
                            dashboardUiStateAssembler.create(
                                settings = checkNotNull(settingsState.value),
                                billingUiState = billingUiState,
                                isAccessibilityServiceEnabled =
                                    accessibilityEnabledState.value,
                                isTamperProtectionEnabled =
                                    tamperProtectionEnabledState.value,
                            )
                        },
                        isTemporaryAllowRequested = temporaryAllowRequestState.value,
                        accessibilityDiagnosticsPort = accessibilityDiagnostics,
                        parentalSettingsSession = parentalSettingsSession,
                        recordSuccessfulProtectionActivationUseCase = protectionActivationUseCase,
                        setTemporaryAllowUseCase = setTemporaryAllowUseCase,
                        isDetectorQaVisible = BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED,
                        onProtectionEnabledChanged = { enabled ->
                            parentalSettingsSession.runAuthorized {
                                settingsStore.setProtectionEnabled(enabled)
                            }
                        },
                        onAccessibilityDisclosureAccepted = {
                            settingsStore.acceptAccessibilityDisclosure()
                        },
                        onSubscribe = {
                            billingCoordinator.launchPurchase()
                        },
                        onRestorePurchases = {
                            billingCoordinator.refreshPurchases()
                        },
                        onManageSubscription = {
                            billingCoordinator.openManageSubscription()
                        },
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onOpenTamperProtectionSettings = ::openTamperProtectionSettings,
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
        if (::settingsStore.isInitialized) {
            refreshState()
        }
        if (::billingCoordinator.isInitialized) {
            billingCoordinator.start()
        }
    }

    override fun onPause() {
        parentalSettingsSession.lock()
        super.onPause()
    }

    override fun onDestroy() {
        if (::billingCoordinator.isInitialized) {
            billingCoordinator.stop()
        }
        activityScope.cancel()
        super.onDestroy()
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsReadState.value = SettingsReadState.Loading
        settingsJob =
            activityScope.launch {
                ObserveSettingsUseCase(settingsStore)().collect { state ->
                    if (state is SettingsReadState.Ready) {
                        if (!::billingCoordinator.isInitialized) {
                            initializeBilling()
                            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                                billingCoordinator.start()
                            }
                        }
                        settingsState.value = state.snapshot
                    }
                    // Removing the app content also removes any PIN/settings navigation on failure.
                    settingsReadState.value = state
                }
            }
    }

    private fun initializeBilling() {
        val syncBillingEntitlementUseCase = SyncBillingEntitlementUseCase()
        billingCoordinator =
            BillingCoordinator(
                billingGateway = GooglePlayBillingGateway(this),
                syncBillingEntitlementUseCase = syncBillingEntitlementUseCase,
                onEntitlementChanged = { snapshot ->
                    settingsStore.updateBillingEntitlement(snapshot)
                },
                billingScope = activityScope,
            )
    }

    private fun refreshState() {
        accessibilityEnabledState.value = AccessibilityServiceStatus.isEnabled(this)
        tamperProtectionEnabledState.value = TamperProtectionStatus.isActive(this)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openTamperProtectionSettings() {
        startActivity(TamperProtectionStatus.enableIntent(this))
    }

    private fun closeTemporaryAllowRequest() {
        finishAndRemoveTask()
    }

    private fun Intent?.isTemporaryAllowRequest(): Boolean = this?.getBooleanExtra(EXTRA_OPEN_TEMPORARY_ALLOW_PIN, false) == true

    companion object {
        const val EXTRA_OPEN_TEMPORARY_ALLOW_PIN = "com.shortsblockerkids.OPEN_TEMPORARY_ALLOW_PIN"
    }
}
