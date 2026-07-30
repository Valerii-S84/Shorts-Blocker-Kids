package com.shortsblockerkids.feature.onboarding

import com.shortsblockerkids.presentation.app.AppScreen
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityPermissionFlowTest {
    @Test
    fun pinCreationShowsProtectedAppSelectionBeforeDisclosure() {
        assertEquals(
            AppScreen.ProtectedApps,
            AccessibilityPermissionFlow.destinationAfterPinCreated(),
        )
    }

    @Test
    fun userCanDeclineDisclosureWithoutOpeningSettings() {
        assertEquals(
            AppScreen.Dashboard,
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
        assertEquals(
            AppScreen.ProtectedApps,
            AccessibilityPermissionFlow.destinationAfterParentUnlock(
                hasAcceptedAccessibilityDisclosure = false,
                pendingTemporaryAllow = false,
            ),
        )
        assertEquals(
            AppScreen.ProtectedApps,
            AccessibilityPermissionFlow.destinationAfterParentUnlock(
                hasAcceptedAccessibilityDisclosure = false,
                pendingTemporaryAllow = true,
            ),
        )
        assertEquals(
            AppScreen.Dashboard,
            AccessibilityPermissionFlow.destinationAfterParentUnlock(
                hasAcceptedAccessibilityDisclosure = true,
                pendingTemporaryAllow = false,
            ),
        )
        assertEquals(
            AppScreen.TemporaryAllow,
            AccessibilityPermissionFlow.destinationAfterParentUnlock(
                hasAcceptedAccessibilityDisclosure = true,
                pendingTemporaryAllow = true,
            ),
        )
    }
}
