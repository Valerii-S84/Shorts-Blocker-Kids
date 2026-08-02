package com.shortsblockerkids.presentation.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shortsblockerkids.presentation.onboarding.AccessibilityDisclosureDecision
import com.shortsblockerkids.presentation.onboarding.AccessibilityPermissionFlow
import com.shortsblockerkids.presentation.onboarding.AccessibilitySettingsRequest

class ShortsBlockerKidsCoordinator internal constructor(
    currentScreen: AppScreen,
    isUnlocked: Boolean,
    pendingTemporaryAllow: Boolean,
    pendingProtectionDisable: Boolean,
) {
    constructor(isPinCreated: Boolean) : this(
        currentScreen =
            if (isPinCreated) {
                AppScreen.PinEntry
            } else {
                AppScreen.Welcome
            },
        isUnlocked = !isPinCreated,
        pendingTemporaryAllow = false,
        pendingProtectionDisable = false,
    )

    var currentScreen by mutableStateOf(currentScreen)
        private set

    var isUnlocked by mutableStateOf(isUnlocked)
        private set

    var pendingTemporaryAllow by mutableStateOf(pendingTemporaryAllow)
        private set

    var pendingProtectionDisable by mutableStateOf(pendingProtectionDisable)
        private set

    fun onPinConfigurationObserved(isPinCreated: Boolean) {
        if (isPinCreated && !isUnlocked) {
            currentScreen = AppScreen.PinEntry
        }
    }

    fun onWelcomeStarted() {
        currentScreen = AppScreen.PinSetup
    }

    fun onPinCreated() {
        isUnlocked = true
        currentScreen = AccessibilityPermissionFlow.destinationAfterPinCreated()
    }

    fun onProtectedAppsContinued() {
        currentScreen = AppScreen.AccessibilityDisclosure
    }

    fun onDisclosureAccepted() {
        currentScreen =
            AccessibilityPermissionFlow.destinationAfterDisclosure(
                AccessibilityDisclosureDecision.Accepted,
            )
    }

    fun onDisclosureDeclined() {
        currentScreen =
            AccessibilityPermissionFlow.destinationAfterDisclosure(
                AccessibilityDisclosureDecision.Declined,
            )
    }

    fun onAccessibilitySettingsRequested(hasAffirmativeAccessibilityConsent: Boolean): Boolean =
        when (
            AccessibilityPermissionFlow.settingsRequest(
                hasAffirmativeAccessibilityConsent,
            )
        ) {
            AccessibilitySettingsRequest.ShowDisclosure -> {
                currentScreen = AppScreen.AccessibilityDisclosure
                false
            }

            AccessibilitySettingsRequest.OpenSystemSettings -> true
        }

    fun onAccessibilityEnablementCompleted() {
        currentScreen = AppScreen.Dashboard
    }

    fun onParentUnlocked(hasAcceptedAccessibilityDisclosure: Boolean): Boolean {
        isUnlocked = true
        if (pendingProtectionDisable) {
            return true
        }

        currentScreen =
            AccessibilityPermissionFlow.destinationAfterParentUnlock(
                hasAcceptedAccessibilityDisclosure = hasAcceptedAccessibilityDisclosure,
                pendingTemporaryAllow = pendingTemporaryAllow,
            )
        return false
    }

    fun onProtectionDisableRequested() {
        pendingProtectionDisable = true
        pendingTemporaryAllow = false
        isUnlocked = false
        currentScreen = AppScreen.PinEntry
    }

    fun onProtectionDisableCompleted() {
        pendingProtectionDisable = false
        currentScreen = AppScreen.Dashboard
    }

    fun onTemporaryAllowRequested(isPinCreated: Boolean): Boolean {
        if (!isPinCreated) {
            return false
        }

        pendingTemporaryAllow = true
        pendingProtectionDisable = false
        isUnlocked = false
        currentScreen = AppScreen.PinEntry
        return true
    }

    fun onTemporaryAllowFinished() {
        pendingTemporaryAllow = false
        pendingProtectionDisable = false
    }

    fun onPrivacyPolicyRequested() {
        currentScreen = AppScreen.PrivacyPolicy
    }

    fun onTamperProtectionRequested() {
        currentScreen = AppScreen.TamperProtectionDisclosure
    }

    fun onDetectorQaRequested() {
        currentScreen = AppScreen.DetectorQa
    }

    fun onDashboardRequested() {
        currentScreen = AppScreen.Dashboard
    }
}
