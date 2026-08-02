package com.shortsblockerkids

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.protection.ClearExpiredTemporaryAllowUseCase
import com.shortsblockerkids.application.protection.RecordSuccessfulProtectionActivationUseCase
import com.shortsblockerkids.application.protection.SetTemporaryAllowUseCase
import com.shortsblockerkids.composition.dashboard.DashboardUiStateAssembler
import com.shortsblockerkids.core.billing.HttpBillingBackendClient
import com.shortsblockerkids.core.billing.PlayBillingRepository
import com.shortsblockerkids.infrastructure.storage.DataStoreSettingsStore
import com.shortsblockerkids.infrastructure.storage.SettingsPinAccessAdapter
import com.shortsblockerkids.infrastructure.time.SystemTimeProvider
import com.shortsblockerkids.platform.accessibility.status.AccessibilityServiceStatus
import com.shortsblockerkids.platform.tamper.TamperProtectionStatus
import com.shortsblockerkids.presentation.app.ShortsBlockerKidsApp
import com.shortsblockerkids.ui.theme.ShortsBlockerKidsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private lateinit var settingsStore: DataStoreSettingsStore
    private lateinit var billingRepository: PlayBillingRepository
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsState = mutableStateOf(AppSettingsSnapshot())
    private val accessibilityEnabledState = mutableStateOf(false)
    private val tamperProtectionEnabledState = mutableStateOf(false)
    private val temporaryAllowRequestState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = DataStoreSettingsStore(this)
        val pinAccessAdapter = SettingsPinAccessAdapter(pinStateStore = settingsStore)
        val createPinUseCase = CreatePinUseCase(pinAccessAdapter)
        val verifyPinUseCase = VerifyPinUseCase(pinAccessAdapter)
        val timeProvider = SystemTimeProvider()
        val dashboardUiStateAssembler = DashboardUiStateAssembler(timeProvider)
        val protectionActivationUseCase =
            RecordSuccessfulProtectionActivationUseCase(
                timeProvider = timeProvider,
                protectionActivationStore = settingsStore,
            )
        val setTemporaryAllowUseCase =
            SetTemporaryAllowUseCase(
                timeProvider = timeProvider,
                temporaryAllowStore = settingsStore,
            )
        val clearExpiredTemporaryAllowUseCase =
            ClearExpiredTemporaryAllowUseCase(
                timeProvider = timeProvider,
                temporaryAllowStore = settingsStore,
            )
        billingRepository =
            PlayBillingRepository(
                context = this,
                onEntitlementChanged = { snapshot ->
                    activityScope.launch {
                        settingsStore.updateBillingEntitlement(snapshot)
                    }
                },
                billingBackendClient =
                    HttpBillingBackendClient.fromBaseUrl(BuildConfig.BILLING_BACKEND_BASE_URL),
                installId = runBlocking { settingsStore.getOrCreateBillingInstallationId() },
                appVersion = BuildConfig.VERSION_NAME,
                clientOnlyModeRequested = BuildConfig.BILLING_CLIENT_ONLY_TEST_MODE,
                internalTestingBuild = BuildConfig.DEBUG,
                billingScope = activityScope,
            )
        temporaryAllowRequestState.value = intent.isTemporaryAllowRequest()
        loadInitialSettings()
        observeSettings(clearExpiredTemporaryAllowUseCase)
        refreshState()

        setContent {
            ShortsBlockerKidsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val billingUiState by billingRepository.uiState.collectAsState()
                    ShortsBlockerKidsApp(
                        dashboardUiStateProvider = {
                            dashboardUiStateAssembler.create(
                                settings = settingsState.value,
                                billingUiState = billingUiState,
                                isAccessibilityServiceEnabled =
                                    accessibilityEnabledState.value,
                                isTamperProtectionEnabled =
                                    tamperProtectionEnabledState.value,
                            )
                        },
                        isTemporaryAllowRequested = temporaryAllowRequestState.value,
                        createPinUseCase = createPinUseCase,
                        verifyPinUseCase = verifyPinUseCase,
                        recordSuccessfulProtectionActivationUseCase = protectionActivationUseCase,
                        setTemporaryAllowUseCase = setTemporaryAllowUseCase,
                        isDetectorQaVisible = BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED,
                        onProtectionEnabledChanged = { enabled ->
                            settingsStore.setProtectionEnabled(enabled)
                        },
                        onPlatformEnabledChanged = { platformId, enabled ->
                            settingsStore.setPlatformEnabled(platformId, enabled)
                        },
                        onAccessibilityDisclosureAccepted = {
                            settingsStore.acceptAccessibilityDisclosure()
                        },
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

    private fun observeSettings(clearExpiredTemporaryAllowUseCase: ClearExpiredTemporaryAllowUseCase) {
        activityScope.launch {
            settingsStore.readSettings().collect { settings ->
                val temporaryAllowRemoved = clearExpiredTemporaryAllowUseCase()
                settingsState.value =
                    if (temporaryAllowRemoved) {
                        settings.copy(
                            protectionConfiguration =
                                settings.protectionConfiguration.copy(
                                    temporaryAllowUntilMillis = null,
                                ),
                        )
                    } else {
                        settings
                    }
            }
        }
    }

    private fun loadInitialSettings() {
        val initialSettings =
            runBlocking {
                settingsStore.readSettings().first()
            }
        settingsState.value = initialSettings
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
