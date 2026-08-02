package com.shortsblockerkids.presentation.onboarding

import com.shortsblockerkids.presentation.app.AppScreen

object AccessibilityPermissionFlow {
    fun destinationAfterPinCreated(): AppScreen = AppScreen.ProtectedApps

    fun destinationAfterParentUnlock(
        hasAcceptedAccessibilityDisclosure: Boolean,
        pendingTemporaryAllow: Boolean,
    ): AppScreen {
        if (!hasAcceptedAccessibilityDisclosure) {
            return AppScreen.ProtectedApps
        }

        return if (pendingTemporaryAllow) {
            AppScreen.TemporaryAllow
        } else {
            AppScreen.Dashboard
        }
    }

    fun destinationAfterDisclosure(decision: AccessibilityDisclosureDecision): AppScreen =
        when (decision) {
            AccessibilityDisclosureDecision.Accepted -> AppScreen.EnableAccessibility
            AccessibilityDisclosureDecision.Declined -> AppScreen.Dashboard
        }

    fun settingsRequest(hasAffirmativeAccessibilityConsent: Boolean): AccessibilitySettingsRequest =
        if (hasAffirmativeAccessibilityConsent) {
            AccessibilitySettingsRequest.OpenSystemSettings
        } else {
            AccessibilitySettingsRequest.ShowDisclosure
        }
}

enum class AccessibilityDisclosureDecision {
    Accepted,
    Declined,
}

enum class AccessibilitySettingsRequest {
    ShowDisclosure,
    OpenSystemSettings,
}
