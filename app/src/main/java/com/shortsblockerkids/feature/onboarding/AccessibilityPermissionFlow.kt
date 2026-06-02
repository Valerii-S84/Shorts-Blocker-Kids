package com.shortsblockerkids.feature.onboarding

import com.shortsblockerkids.core.storage.AppSettings

object AccessibilityPermissionFlow {
    fun destinationAfterPinCreated(): AccessibilitySetupDestination = AccessibilitySetupDestination.ProtectedApps

    fun destinationAfterParentUnlock(
        settings: AppSettings,
        pendingTemporaryAllow: Boolean,
    ): AccessibilitySetupDestination {
        if (!settings.accessibilityDisclosureAccepted) {
            return AccessibilitySetupDestination.Disclosure
        }

        return if (pendingTemporaryAllow) {
            AccessibilitySetupDestination.TemporaryAllow
        } else {
            AccessibilitySetupDestination.Dashboard
        }
    }

    fun destinationAfterDisclosure(decision: AccessibilityDisclosureDecision): AccessibilitySetupDestination =
        when (decision) {
            AccessibilityDisclosureDecision.Accepted -> AccessibilitySetupDestination.EnableAccessibility
            AccessibilityDisclosureDecision.Declined -> AccessibilitySetupDestination.Dashboard
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

enum class AccessibilitySetupDestination {
    ProtectedApps,
    Disclosure,
    EnableAccessibility,
    Dashboard,
    TemporaryAllow,
}
