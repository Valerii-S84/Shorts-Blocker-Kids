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
import com.shortsblockerkids.application.billing.BillingSyncConfiguration
import com.shortsblockerkids.application.billing.SyncBillingEntitlementUseCase
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.port.AccessibilityServiceStatusPort
import com.shortsblockerkids.application.protection.ClearExpiredTemporaryAllowUseCase
import com.shortsblockerkids.application.protection.RecordSuccessfulProtectionActivationUseCase
import com.shortsblockerkids.application.protection.SetTemporaryAllowUseCase
import com.shortsblockerkids.composition.dashboard.DashboardUiStateAssembler
import com.shortsblockerkids.infrastructure.billing.GooglePlayBillingGateway
import com.shortsblockerkids.infrastructure.billing.HttpBillingVerificationAdapter
import com.shortsblockerkids.infrastructure.billing.PlayBillingConfig
import com.shortsblockerkids.infrastructure.security.Pbkdf2PinHasher
import com.shortsblockerkids.infrastructure.storage.DataStoreSettingsStore
import com.shortsblockerkids.infrastructure.time.SystemTimeProvider
import com.shortsblockerkids.platform.accessibility.status.AccessibilityServiceStatus
import com.shortsblockerkids.platform.tamper.TamperProtectionStatus
import com.shortsblockerkids.presentation.app.ShortsBlockerKidsApp
import com.shortsblockerkids.presentation.billing.BillingCoordinator
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
    private lateinit var billingCoordinator: BillingCoordinator
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsState = mutableStateOf(AppSettingsSnapshot())
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
            )
        val dashboardUiStateAssembler = DashboardUiStateAssembler(timeProvider)
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
            )
        val clearExpiredTemporaryAllowUseCase =
            ClearExpiredTemporaryAllowUseCase(
                timeProvider = timeProvider,
                temporaryAllowStore = settingsStore,
            )
        val syncBillingEntitlementUseCase =
            SyncBillingEntitlementUseCase(
                billingVerificationPort =
                    HttpBillingVerificationAdapter.fromBaseUrl(
                        BuildConfig.BILLING_BACKEND_BASE_URL,
                    ),
                timeProvider = timeProvider,
                configuration =
                    BillingSyncConfiguration(
                        installId =
                            runBlocking { settingsStore.getOrCreateBillingInstallationId() },
                        packageName = applicationContext.packageName,
                        productId = PlayBillingConfig.MONTHLY_SUBSCRIPTION_PRODUCT_ID,
                        appVersion = BuildConfig.VERSION_NAME,
                        clientOnlyModeRequested = BuildConfig.BILLING_CLIENT_ONLY_TEST_MODE,
                        internalTestingBuild = BuildConfig.DEBUG,
                    ),
            )
        billingCoordinator =
            BillingCoordinator(
                billingGateway = GooglePlayBillingGateway(this),
                syncBillingEntitlementUseCase = syncBillingEntitlementUseCase,
                onEntitlementChanged = { snapshot ->
                    activityScope.launch {
                        settingsStore.updateBillingEntitlement(snapshot)
                    }
                },
                billingScope = activityScope,
            )
        temporaryAllowRequestState.value = intent.isTemporaryAllowRequest()
        loadInitialSettings()
        observeSettings(clearExpiredTemporaryAllowUseCase)
        refreshState()

        setContent {
            ShortsBlockerKidsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val billingUiState by billingCoordinator.uiState.collectAsState()
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

    override fun onDestroy() {
        if (::billingCoordinator.isInitialized) {
            billingCoordinator.stop()
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
