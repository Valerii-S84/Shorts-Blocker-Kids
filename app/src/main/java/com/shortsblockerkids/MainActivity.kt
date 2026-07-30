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
import com.shortsblockerkids.accessibility.AccessibilityServiceStatus
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.protection.ClearExpiredTemporaryAllowUseCase
import com.shortsblockerkids.application.protection.RecordSuccessfulProtectionActivationUseCase
import com.shortsblockerkids.application.protection.SetTemporaryAllowUseCase
import com.shortsblockerkids.core.billing.HttpBillingBackendClient
import com.shortsblockerkids.core.billing.PlayBillingRepository
import com.shortsblockerkids.core.storage.AppSettings
import com.shortsblockerkids.core.storage.SettingsRepository
import com.shortsblockerkids.core.tamper.TamperProtectionStatus
import com.shortsblockerkids.infrastructure.storage.SettingsPinAccessAdapter
import com.shortsblockerkids.infrastructure.time.SystemTimeProvider
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
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var billingRepository: PlayBillingRepository
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsState = mutableStateOf(AppSettings())
    private val accessibilityEnabledState = mutableStateOf(false)
    private val tamperProtectionEnabledState = mutableStateOf(false)
    private val temporaryAllowRequestState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(this)
        val pinAccessAdapter = SettingsPinAccessAdapter(settingsRepository)
        val createPinUseCase = CreatePinUseCase(pinAccessAdapter)
        val verifyPinUseCase = VerifyPinUseCase(pinAccessAdapter)
        val protectionActivationUseCase =
            RecordSuccessfulProtectionActivationUseCase(
                timeProvider = SystemTimeProvider(),
                protectionActivationStore = settingsRepository,
            )
        val setTemporaryAllowUseCase =
            SetTemporaryAllowUseCase(
                timeProvider = SystemTimeProvider(),
                temporaryAllowStore = settingsRepository,
            )
        val clearExpiredTemporaryAllowUseCase =
            ClearExpiredTemporaryAllowUseCase(
                timeProvider = SystemTimeProvider(),
                temporaryAllowStore = settingsRepository,
            )
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
        observeSettings(clearExpiredTemporaryAllowUseCase)
        refreshState()

        setContent {
            ShortsBlockerKidsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val billingUiState by billingRepository.uiState.collectAsState()
                    ShortsBlockerKidsApp(
                        settings = settingsState.value,
                        isAccessibilityServiceEnabled = accessibilityEnabledState.value,
                        isTamperProtectionEnabled = tamperProtectionEnabledState.value,
                        isTemporaryAllowRequested = temporaryAllowRequestState.value,
                        billingUiState = billingUiState,
                        createPinUseCase = createPinUseCase,
                        verifyPinUseCase = verifyPinUseCase,
                        recordSuccessfulProtectionActivationUseCase = protectionActivationUseCase,
                        setTemporaryAllowUseCase = setTemporaryAllowUseCase,
                        isDetectorQaVisible = BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED,
                        onProtectionEnabledChanged = { enabled ->
                            settingsRepository.setProtectionEnabled(enabled)
                        },
                        onPlatformEnabledChanged = { platformId, enabled ->
                            settingsRepository.setPlatformEnabled(platformId, enabled)
                        },
                        onAccessibilityDisclosureAccepted = {
                            settingsRepository.acceptAccessibilityDisclosure()
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

    private fun observeSettings(clearExpiredTemporaryAllowUseCase: ClearExpiredTemporaryAllowUseCase) {
        activityScope.launch {
            settingsRepository.readSettings().collect { settings ->
                val temporaryAllowRemoved = clearExpiredTemporaryAllowUseCase()
                settingsState.value =
                    if (temporaryAllowRemoved) {
                        settings.copy(temporaryAllowUntil = null)
                    } else {
                        settings
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
