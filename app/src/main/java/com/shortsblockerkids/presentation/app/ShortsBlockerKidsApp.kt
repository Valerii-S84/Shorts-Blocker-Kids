@file:JvmName("MainActivityKt")

package com.shortsblockerkids.presentation.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.protection.ProtectionActivationIntent
import com.shortsblockerkids.application.protection.ProtectionActivationResult
import com.shortsblockerkids.application.protection.RecordSuccessfulProtectionActivationUseCase
import com.shortsblockerkids.application.protection.SetTemporaryAllowUseCase
import com.shortsblockerkids.presentation.blocking.TemporaryAllowCompletion
import com.shortsblockerkids.presentation.blocking.TemporaryAllowFlowController
import com.shortsblockerkids.presentation.blocking.TemporaryAllowScreen
import com.shortsblockerkids.presentation.dashboard.DashboardBillingCallbacks
import com.shortsblockerkids.presentation.dashboard.DashboardCallbacks
import com.shortsblockerkids.presentation.dashboard.DashboardNavigationCallbacks
import com.shortsblockerkids.presentation.dashboard.DashboardProtectionCallbacks
import com.shortsblockerkids.presentation.dashboard.DashboardScreen
import com.shortsblockerkids.presentation.dashboard.DashboardUiState
import com.shortsblockerkids.presentation.debug.DetectorPlaygroundScreen
import com.shortsblockerkids.presentation.onboarding.AccessibilityDisclosureScreen
import com.shortsblockerkids.presentation.onboarding.EnableAccessibilityScreen
import com.shortsblockerkids.presentation.onboarding.ProtectedAppsCallbacks
import com.shortsblockerkids.presentation.onboarding.ProtectedAppsScreen
import com.shortsblockerkids.presentation.onboarding.WelcomeScreen
import com.shortsblockerkids.presentation.pin.PinEntryScreen
import com.shortsblockerkids.presentation.pin.PinSetupScreen
import com.shortsblockerkids.presentation.privacy.PrivacyPolicyScreen
import com.shortsblockerkids.presentation.tamper.TamperProtectionDisclosureScreen
import kotlinx.coroutines.launch

@Composable
internal fun ShortsBlockerKidsApp(
    dashboardUiStateProvider: () -> DashboardUiState,
    isTemporaryAllowRequested: Boolean,
    isDetectorQaVisible: Boolean,
    createPinUseCase: CreatePinUseCase,
    verifyPinUseCase: VerifyPinUseCase,
    recordSuccessfulProtectionActivationUseCase: RecordSuccessfulProtectionActivationUseCase,
    setTemporaryAllowUseCase: SetTemporaryAllowUseCase,
    onProtectionEnabledChanged: suspend (Boolean) -> Unit,
    onPlatformEnabledChanged: suspend (String, Boolean) -> Unit,
    onAccessibilityDisclosureAccepted: suspend () -> Unit,
    onSubscribe: () -> Unit,
    onRestorePurchases: () -> Unit,
    onManageSubscription: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenTamperProtectionSettings: () -> Unit,
    onStateChanged: () -> Unit,
    onTemporaryAllowFlowClosed: () -> Unit,
    onTemporaryAllowRequestConsumed: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val temporaryAllowFlowController =
        TemporaryAllowFlowController(setTemporaryAllowUseCase)
    val coordinator =
        rememberSaveable(
            saver =
                listSaver<ShortsBlockerKidsCoordinator, Any>(
                    save = {
                        listOf(
                            it.currentScreen.name,
                            it.isUnlocked,
                            it.pendingTemporaryAllow,
                            it.pendingProtectionDisable,
                        )
                    },
                    restore = {
                        ShortsBlockerKidsCoordinator(
                            currentScreen = AppScreen.valueOf(it[0] as String),
                            isUnlocked = it[1] as Boolean,
                            pendingTemporaryAllow = it[2] as Boolean,
                            pendingProtectionDisable = it[3] as Boolean,
                        )
                    },
                ),
        ) {
            ShortsBlockerKidsCoordinator(
                isPinCreated = dashboardUiStateProvider().setup.isPinConfigured,
            )
        }
    val currentScreen = coordinator.currentScreen
    val dashboardUiState = dashboardUiStateProvider()

    LaunchedEffect(dashboardUiState.setup.isPinConfigured, coordinator.isUnlocked) {
        coordinator.onPinConfigurationObserved(dashboardUiState.setup.isPinConfigured)
    }

    LaunchedEffect(isTemporaryAllowRequested, dashboardUiState.setup.isPinConfigured) {
        if (
            isTemporaryAllowRequested &&
            coordinator.onTemporaryAllowRequested(dashboardUiState.setup.isPinConfigured)
        ) {
            onTemporaryAllowRequestConsumed()
        }
    }

    LaunchedEffect(currentScreen, dashboardUiState) {
        if (currentScreen == AppScreen.Dashboard) {
            when (
                recordSuccessfulProtectionActivationUseCase(
                    ProtectionActivationIntent.COMPLETE_CURRENT_CONFIGURATION,
                )
            ) {
                ProtectionActivationResult.ACTIVATED -> onStateChanged()
                ProtectionActivationResult.ALREADY_STARTED,
                ProtectionActivationResult.PREREQUISITES_NOT_MET,
                -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.Welcome ->
                WelcomeScreen(
                    onStart = coordinator::onWelcomeStarted,
                )

            AppScreen.PinSetup ->
                PinSetupScreen(
                    createPinUseCase = createPinUseCase,
                    onPinCreated = {
                        onStateChanged()
                        coordinator.onPinCreated()
                    },
                )

            AppScreen.PinEntry ->
                PinEntryScreen(
                    verifyPinUseCase = verifyPinUseCase,
                    onStateChanged = onStateChanged,
                    onUnlocked = {
                        val shouldDisableProtection =
                            coordinator.onParentUnlocked(
                                dashboardUiState.setup.isAccessibilityDisclosureAccepted,
                            )
                        if (shouldDisableProtection) {
                            coroutineScope.launch {
                                onProtectionEnabledChanged(false)
                                onStateChanged()
                                coordinator.onProtectionDisableCompleted()
                            }
                        }
                    },
                )

            AppScreen.ProtectedApps ->
                ProtectedAppsScreen(
                    items = dashboardUiState.platforms.protected,
                    callbacks =
                        ProtectedAppsCallbacks(
                            onPlatformEnabledChanged = { platformId, enabled ->
                                coroutineScope.launch {
                                    onPlatformEnabledChanged(platformId, enabled)
                                    onStateChanged()
                                }
                            },
                            onContinue = coordinator::onProtectedAppsContinued,
                        ),
                )

            AppScreen.AccessibilityDisclosure ->
                AccessibilityDisclosureScreen(
                    onAccept = {
                        coroutineScope.launch {
                            onAccessibilityDisclosureAccepted()
                            onStateChanged()
                            coordinator.onDisclosureAccepted()
                        }
                    },
                    onDecline = coordinator::onDisclosureDeclined,
                )

            AppScreen.EnableAccessibility ->
                EnableAccessibilityScreen(
                    isAccessibilityServiceEnabled =
                        dashboardUiState.setup.isAccessibilityServiceEnabled,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onEnabled = {
                        onStateChanged()
                        coroutineScope.launch {
                            when (
                                recordSuccessfulProtectionActivationUseCase(
                                    ProtectionActivationIntent.ENABLE_PROTECTION,
                                )
                            ) {
                                ProtectionActivationResult.ACTIVATED,
                                ProtectionActivationResult.ALREADY_STARTED,
                                -> {
                                    onStateChanged()
                                    coordinator.onAccessibilityEnablementCompleted()
                                }

                                ProtectionActivationResult.PREREQUISITES_NOT_MET ->
                                    onStateChanged()
                            }
                        }
                    },
                )

            AppScreen.Dashboard ->
                DashboardScreen(
                    uiState = dashboardUiState,
                    callbacks =
                        DashboardCallbacks(
                            protection =
                                DashboardProtectionCallbacks(
                                    onProtectionChanged = { enabled ->
                                        if (enabled) {
                                            coroutineScope.launch {
                                                when (
                                                    recordSuccessfulProtectionActivationUseCase(
                                                        ProtectionActivationIntent.ENABLE_PROTECTION,
                                                    )
                                                ) {
                                                    ProtectionActivationResult.ACTIVATED ->
                                                        onStateChanged()

                                                    ProtectionActivationResult.ALREADY_STARTED ->
                                                        onStateChanged()

                                                    ProtectionActivationResult.PREREQUISITES_NOT_MET ->
                                                        onStateChanged()
                                                }
                                            }
                                        } else {
                                            coordinator.onProtectionDisableRequested()
                                        }
                                    },
                                    onPlatformEnabledChanged = { platformId, enabled ->
                                        coroutineScope.launch {
                                            onPlatformEnabledChanged(platformId, enabled)
                                            onStateChanged()
                                        }
                                    },
                                ),
                            billing =
                                DashboardBillingCallbacks(
                                    onSubscribe = onSubscribe,
                                    onRestorePurchases = onRestorePurchases,
                                    onManageSubscription = onManageSubscription,
                                ),
                            navigation =
                                DashboardNavigationCallbacks(
                                    onOpenAccessibilitySettings = {
                                        if (
                                            coordinator.onAccessibilitySettingsRequested(
                                                dashboardUiState.setup
                                                    .isAccessibilityDisclosureAccepted,
                                            )
                                        ) {
                                            onOpenAccessibilitySettings()
                                        }
                                    },
                                    onOpenPrivacyPolicy = coordinator::onPrivacyPolicyRequested,
                                    onOpenTamperProtection =
                                        coordinator::onTamperProtectionRequested,
                                    onOpenDebugQa =
                                        if (isDetectorQaVisible) {
                                            coordinator::onDetectorQaRequested
                                        } else {
                                            null
                                        },
                                ),
                        ),
                )

            AppScreen.PrivacyPolicy ->
                PrivacyPolicyScreen(
                    onBack = coordinator::onDashboardRequested,
                )

            AppScreen.TamperProtectionDisclosure ->
                TamperProtectionDisclosureScreen(
                    isTamperProtectionEnabled = dashboardUiState.setup.isTamperProtectionEnabled,
                    onEnableTamperProtection = onOpenTamperProtectionSettings,
                    onBack = {
                        onStateChanged()
                        coordinator.onDashboardRequested()
                    },
                )

            AppScreen.TemporaryAllow ->
                TemporaryAllowScreen(
                    onDurationSelected = { duration ->
                        coroutineScope.launch {
                            val completion = temporaryAllowFlowController.selectDuration(duration)
                            coordinator.onTemporaryAllowFinished()
                            onStateChanged()
                            handleTemporaryAllowCompletion(
                                completion = completion,
                                onTemporaryAllowFlowClosed = onTemporaryAllowFlowClosed,
                            )
                        }
                    },
                    onCancel = {
                        val completion = temporaryAllowFlowController.cancel()
                        coordinator.onTemporaryAllowFinished()
                        handleTemporaryAllowCompletion(
                            completion = completion,
                            onTemporaryAllowFlowClosed = onTemporaryAllowFlowClosed,
                        )
                    },
                )

            AppScreen.DetectorQa ->
                DetectorPlaygroundScreen(
                    uiState = dashboardUiState,
                    onBack = coordinator::onDashboardRequested,
                )
        }
    }
}

private fun handleTemporaryAllowCompletion(
    completion: TemporaryAllowCompletion,
    onTemporaryAllowFlowClosed: () -> Unit,
) {
    when (completion) {
        TemporaryAllowCompletion.ReturnToForegroundApp -> onTemporaryAllowFlowClosed()
    }
}
