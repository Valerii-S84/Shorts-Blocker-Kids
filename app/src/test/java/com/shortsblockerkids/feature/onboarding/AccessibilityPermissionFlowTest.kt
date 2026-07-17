package com.shortsblockerkids.feature.onboarding

import com.shortsblockerkids.core.storage.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityPermissionFlowTest {
    @Test
    fun pinCreationShowsProtectedAppSelectionBeforeDisclosure() {
        assertEquals(
            AccessibilitySetupDestination.ProtectedApps,
            AccessibilityPermissionFlow.destinationAfterPinCreated(),
        )
    }

    @Test
    fun userCanDeclineDisclosureWithoutOpeningSettings() {
        assertEquals(
            AccessibilitySetupDestination.Dashboard,
            AccessibilityPermissionFlow.destinationAfterDisclosure(
                AccessibilityDisclosureDecision.Declined,
            ),
        )
        assertEquals(
            AccessibilitySettingsRequest.ShowDisclosure,
            AccessibilityPermissionFlow.settingsRequest(
                hasAffirmativeAccessibilityConsent = false,
            ),
        )
    }

    @Test
    fun accessibilitySettingsOpenOnlyAfterAffirmativeConsent() {
        assertEquals(
            AccessibilitySettingsRequest.ShowDisclosure,
            AccessibilityPermissionFlow.settingsRequest(
                hasAffirmativeAccessibilityConsent = false,
            ),
        )
        assertEquals(
            AccessibilitySettingsRequest.OpenSystemSettings,
            AccessibilityPermissionFlow.settingsRequest(
                hasAffirmativeAccessibilityConsent = true,
            ),
        )
    }

    @Test
    fun resumedProtectionSetupReturnsToAppSelectionBeforeDisclosure() {
        val settingsWithoutConsent = pinCreatedSettings(accessibilityDisclosureAccepted = false)
        val settingsWithConsent = pinCreatedSettings(accessibilityDisclosureAccepted = true)

        assertEquals(
            AccessibilitySetupDestination.ProtectedApps,
            AccessibilityPermissionFlow.destinationAfterParentUnlock(
                settings = settingsWithoutConsent,
                pendingTemporaryAllow = false,
            ),
        )
        assertEquals(
            AccessibilitySetupDestination.ProtectedApps,
            AccessibilityPermissionFlow.destinationAfterParentUnlock(
                settings = settingsWithoutConsent,
                pendingTemporaryAllow = true,
            ),
        )
        assertEquals(
            AccessibilitySetupDestination.Dashboard,
            AccessibilityPermissionFlow.destinationAfterParentUnlock(
                settings = settingsWithConsent,
                pendingTemporaryAllow = false,
            ),
        )
        assertEquals(
            AccessibilitySetupDestination.TemporaryAllow,
            AccessibilityPermissionFlow.destinationAfterParentUnlock(
                settings = settingsWithConsent,
                pendingTemporaryAllow = true,
            ),
        )
    }

    private fun pinCreatedSettings(accessibilityDisclosureAccepted: Boolean): AppSettings =
        AppSettings(
            accessibilityDisclosureAccepted = accessibilityDisclosureAccepted,
            pinHash = "hash",
            pinSalt = "salt",
        )
}
