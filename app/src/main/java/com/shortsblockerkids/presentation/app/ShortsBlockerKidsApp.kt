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
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.pin.VerifyPinUseCase
import com.shortsblockerkids.application.protection.RecordSuccessfulProtectionActivationUseCase
import com.shortsblockerkids.application.protection.SetTemporaryAllowUseCase
import com.shortsblockerkids.core.billing.BillingUiState
import com.shortsblockerkids.domain.protection.ProtectionActivationPolicy
import com.shortsblockerkids.feature.blocking.TemporaryAllowCompletion
import com.shortsblockerkids.feature.blocking.TemporaryAllowFlowController
import com.shortsblockerkids.feature.blocking.TemporaryAllowScreen
import com.shortsblockerkids.feature.dashboard.DashboardScreen
import com.shortsblockerkids.feature.debug.DetectorPlaygroundScreen
import com.shortsblockerkids.feature.onboarding.AccessibilityDisclosureScreen
import com.shortsblockerkids.feature.onboarding.EnableAccessibilityScreen
import com.shortsblockerkids.feature.onboarding.ProtectedAppsScreen
import com.shortsblockerkids.feature.onboarding.WelcomeScreen
import com.shortsblockerkids.feature.pin.PinEntryScreen
import com.shortsblockerkids.feature.pin.PinSetupScreen
import com.shortsblockerkids.feature.privacy.PrivacyPolicyScreen
import com.shortsblockerkids.feature.tamper.TamperProtectionDisclosureScreen
import kotlinx.coroutines.launch

@Composable
internal fun ShortsBlockerKidsApp(
    settings: AppSettingsSnapshot,
    isAccessibilityServiceEnabled: Boolean,
    isTamperProtectionEnabled: Boolean,
    isTemporaryAllowRequested: Boolean,
    isDetectorQaVisible: Boolean,
    billingUiState: BillingUiState,
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
                isPinCreated = settings.protectionConfiguration.isPinConfigured,
            )
        }

    LaunchedEffect(settings.protectionConfiguration.isPinConfigured, coordinator.isUnlocked) {
        coordinator.onPinConfigurationObserved(settings.protectionConfiguration.isPinConfigured)
    }

    LaunchedEffect(isTemporaryAllowRequested, settings.protectionConfiguration.isPinConfigured) {
        if (
            isTemporaryAllowRequested &&
            coordinator.onTemporaryAllowRequested(settings.protectionConfiguration.isPinConfigured)
        ) {
            onTemporaryAllowRequestConsumed()
        }
    }

    LaunchedEffect(
        coordinator.currentScreen,
        isAccessibilityServiceEnabled,
        settings.entitlement.freeTestStartedAtMillis,
        settings.protectionConfiguration.isEnabled,
        settings.protectionConfiguration.isAccessibilityDisclosureAccepted,
        settings.protectionConfiguration.isPinConfigured,
    ) {
        if (
            coordinator.currentScreen == AppScreen.Dashboard &&
            ProtectionActivationPolicy.shouldStartFreeTest(
                isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                isProtectionEnabled = settings.protectionConfiguration.isEnabled,
                isAccessibilityDisclosureAccepted =
                    settings.protectionConfiguration.isAccessibilityDisclosureAccepted,
                isPinConfigured = settings.protectionConfiguration.isPinConfigured,
                isFreeTestAlreadyStarted = settings.entitlement.freeTestStartedAtMillis != null,
            )
        ) {
            recordSuccessfulProtectionActivationUseCase()
            onStateChanged()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (coordinator.currentScreen) {
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
                                settings.protectionConfiguration.isAccessibilityDisclosureAccepted,
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
                    protectionConfiguration = settings.protectionConfiguration,
                    onPlatformEnabledChanged = { platformId, enabled ->
                        coroutineScope.launch {
                            onPlatformEnabledChanged(platformId, enabled)
                            onStateChanged()
                        }
                    },
                    onContinue = coordinator::onProtectedAppsContinued,
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
                    isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onEnabled = {
                        onStateChanged()
                        if (isAccessibilityServiceEnabled) {
                            coroutineScope.launch {
                                recordSuccessfulProtectionActivationUseCase()
                                onStateChanged()
                                coordinator.onAccessibilityEnablementCompleted()
                            }
                        }
                    },
                )

            AppScreen.Dashboard ->
                DashboardScreen(
                    settings = settings,
                    isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                    isTamperProtectionEnabled = isTamperProtectionEnabled,
                    billingUiState = billingUiState,
                    onProtectionChanged = { enabled ->
                        if (enabled) {
                            coroutineScope.launch {
                                if (isAccessibilityServiceEnabled) {
                                    recordSuccessfulProtectionActivationUseCase()
                                } else {
                                    onProtectionEnabledChanged(true)
                                }
                                onStateChanged()
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
                    onSubscribe = onSubscribe,
                    onRestorePurchases = onRestorePurchases,
                    onManageSubscription = onManageSubscription,
                    onOpenAccessibilitySettings = {
                        if (
                            coordinator.onAccessibilitySettingsRequested(
                                settings.protectionConfiguration.isAccessibilityDisclosureAccepted,
                            )
                        ) {
                            onOpenAccessibilitySettings()
                        }
                    },
                    onOpenPrivacyPolicy = coordinator::onPrivacyPolicyRequested,
                    onOpenTamperProtection = coordinator::onTamperProtectionRequested,
                    onOpenDebugQa =
                        if (isDetectorQaVisible) {
                            coordinator::onDetectorQaRequested
                        } else {
                            null
                        },
                )

            AppScreen.PrivacyPolicy ->
                PrivacyPolicyScreen(
                    onBack = coordinator::onDashboardRequested,
                )

            AppScreen.TamperProtectionDisclosure ->
                TamperProtectionDisclosureScreen(
                    isTamperProtectionEnabled = isTamperProtectionEnabled,
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
                    settings = settings,
                    isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                    billingUiState = billingUiState,
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
