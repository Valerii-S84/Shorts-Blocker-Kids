package com.shortsblockerkids.presentation.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsBlockerKidsCoordinatorTest {
    @Test
    fun firstLaunchWithoutPinStartsUnlockedOnWelcome() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = false)

        assertEquals(AppScreen.Welcome, coordinator.currentScreen)
        assertTrue(coordinator.isUnlocked)
        assertFalse(coordinator.pendingTemporaryAllow)
        assertFalse(coordinator.pendingProtectionDisable)

        coordinator.onWelcomeStarted()

        assertEquals(AppScreen.PinSetup, coordinator.currentScreen)
    }

    @Test
    fun startupWithPinRequiresPinEntry() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)

        assertEquals(AppScreen.PinEntry, coordinator.currentScreen)
        assertFalse(coordinator.isUnlocked)
    }

    @Test
    fun pinCreationUnlocksAndShowsProtectedApps() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = false)

        coordinator.onPinCreated()

        assertTrue(coordinator.isUnlocked)
        assertEquals(AppScreen.ProtectedApps, coordinator.currentScreen)
    }

    @Test
    fun pinObservationReturnsLockedCoordinatorToPinEntry() {
        val lockedCoordinator =
            ShortsBlockerKidsCoordinator(
                currentScreen = AppScreen.Dashboard,
                isUnlocked = false,
                pendingTemporaryAllow = false,
                pendingProtectionDisable = false,
            )

        lockedCoordinator.onPinConfigurationObserved(isPinCreated = true)

        assertEquals(AppScreen.PinEntry, lockedCoordinator.currentScreen)

        val unlockedCoordinator =
            ShortsBlockerKidsCoordinator(
                currentScreen = AppScreen.Dashboard,
                isUnlocked = true,
                pendingTemporaryAllow = false,
                pendingProtectionDisable = false,
            )

        unlockedCoordinator.onPinConfigurationObserved(isPinCreated = true)
        assertEquals(AppScreen.Dashboard, unlockedCoordinator.currentScreen)

        unlockedCoordinator.onPinConfigurationObserved(isPinCreated = false)
        assertEquals(AppScreen.Dashboard, unlockedCoordinator.currentScreen)
    }

    @Test
    fun protectedAppsContinueShowsDisclosure() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = false)

        coordinator.onProtectedAppsContinued()

        assertEquals(AppScreen.AccessibilityDisclosure, coordinator.currentScreen)
    }

    @Test
    fun disclosureAcceptAndDeclineUseExistingDestinations() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)

        coordinator.onDisclosureAccepted()

        assertEquals(AppScreen.EnableAccessibility, coordinator.currentScreen)

        coordinator.onDisclosureDeclined()

        assertEquals(AppScreen.Dashboard, coordinator.currentScreen)
    }

    @Test
    fun accessibilitySettingsOpenOnlyWithAffirmativeConsent() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)

        val shouldOpenWithoutConsent =
            coordinator.onAccessibilitySettingsRequested(
                hasAffirmativeAccessibilityConsent = false,
            )

        assertFalse(shouldOpenWithoutConsent)
        assertEquals(AppScreen.AccessibilityDisclosure, coordinator.currentScreen)

        coordinator.onDashboardRequested()
        val shouldOpenWithConsent =
            coordinator.onAccessibilitySettingsRequested(
                hasAffirmativeAccessibilityConsent = true,
            )

        assertTrue(shouldOpenWithConsent)
        assertEquals(AppScreen.Dashboard, coordinator.currentScreen)
    }

    @Test
    fun accessibilityEnablementCompletionShowsDashboard() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)

        coordinator.onAccessibilityEnablementCompleted()

        assertEquals(AppScreen.Dashboard, coordinator.currentScreen)
    }

    @Test
    fun parentUnlockWithoutAcceptedDisclosureShowsProtectedApps() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)

        val requiresProtectionDisable =
            coordinator.onParentUnlocked(
                hasAcceptedAccessibilityDisclosure = false,
            )

        assertFalse(requiresProtectionDisable)
        assertTrue(coordinator.isUnlocked)
        assertEquals(AppScreen.ProtectedApps, coordinator.currentScreen)
    }

    @Test
    fun parentUnlockWithAcceptedDisclosureShowsDashboard() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)

        val requiresProtectionDisable =
            coordinator.onParentUnlocked(
                hasAcceptedAccessibilityDisclosure = true,
            )

        assertFalse(requiresProtectionDisable)
        assertTrue(coordinator.isUnlocked)
        assertEquals(AppScreen.Dashboard, coordinator.currentScreen)
    }

    @Test
    fun parentUnlockWithTemporaryRequestShowsTemporaryAllow() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)
        coordinator.onTemporaryAllowRequested(isPinCreated = true)

        val requiresProtectionDisable =
            coordinator.onParentUnlocked(
                hasAcceptedAccessibilityDisclosure = true,
            )

        assertFalse(requiresProtectionDisable)
        assertTrue(coordinator.isUnlocked)
        assertTrue(coordinator.pendingTemporaryAllow)
        assertEquals(AppScreen.TemporaryAllow, coordinator.currentScreen)
    }

    @Test
    fun protectionDisableRequiresPinAndCompletesOnDashboard() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)
        coordinator.onParentUnlocked(hasAcceptedAccessibilityDisclosure = true)

        coordinator.onProtectionDisableRequested()

        assertTrue(coordinator.pendingProtectionDisable)
        assertFalse(coordinator.pendingTemporaryAllow)
        assertFalse(coordinator.isUnlocked)
        assertEquals(AppScreen.PinEntry, coordinator.currentScreen)

        val requiresProtectionDisable =
            coordinator.onParentUnlocked(
                hasAcceptedAccessibilityDisclosure = true,
            )

        assertTrue(requiresProtectionDisable)
        assertTrue(coordinator.isUnlocked)
        assertTrue(coordinator.pendingProtectionDisable)
        assertEquals(AppScreen.PinEntry, coordinator.currentScreen)

        coordinator.onProtectionDisableCompleted()

        assertFalse(coordinator.pendingProtectionDisable)
        assertEquals(AppScreen.Dashboard, coordinator.currentScreen)
    }

    @Test
    fun temporaryRequestIsConsumedOnlyAfterPinCreation() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = false)
        var consumeCount = 0

        if (coordinator.onTemporaryAllowRequested(isPinCreated = false)) {
            consumeCount += 1
        }
        assertEquals(AppScreen.Welcome, coordinator.currentScreen)

        if (coordinator.onTemporaryAllowRequested(isPinCreated = true)) {
            consumeCount += 1
        }

        assertEquals(1, consumeCount)
        assertTrue(coordinator.pendingTemporaryAllow)
        assertFalse(coordinator.pendingProtectionDisable)
        assertFalse(coordinator.isUnlocked)
        assertEquals(AppScreen.PinEntry, coordinator.currentScreen)
    }

    @Test
    fun temporaryAndDisableRequestsClearEachOther() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)

        coordinator.onProtectionDisableRequested()
        coordinator.onTemporaryAllowRequested(isPinCreated = true)

        assertTrue(coordinator.pendingTemporaryAllow)
        assertFalse(coordinator.pendingProtectionDisable)

        coordinator.onProtectionDisableRequested()

        assertFalse(coordinator.pendingTemporaryAllow)
        assertTrue(coordinator.pendingProtectionDisable)
    }

    @Test
    fun temporaryAllowFinishClearsPendingStatesWithoutChangingRoute() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)
        coordinator.onTemporaryAllowRequested(isPinCreated = true)
        coordinator.onParentUnlocked(hasAcceptedAccessibilityDisclosure = true)

        coordinator.onTemporaryAllowFinished()

        assertFalse(coordinator.pendingTemporaryAllow)
        assertFalse(coordinator.pendingProtectionDisable)
        assertEquals(AppScreen.TemporaryAllow, coordinator.currentScreen)
    }

    @Test
    fun privacyTamperAndDetectorRoutesReturnToDashboard() {
        val coordinator = ShortsBlockerKidsCoordinator(isPinCreated = true)

        coordinator.onPrivacyPolicyRequested()
        assertEquals(AppScreen.PrivacyPolicy, coordinator.currentScreen)
        coordinator.onDashboardRequested()
        assertEquals(AppScreen.Dashboard, coordinator.currentScreen)

        coordinator.onTamperProtectionRequested()
        assertEquals(AppScreen.TamperProtectionDisclosure, coordinator.currentScreen)
        coordinator.onDashboardRequested()
        assertEquals(AppScreen.Dashboard, coordinator.currentScreen)

        coordinator.onDetectorQaRequested()
        assertEquals(AppScreen.DetectorQa, coordinator.currentScreen)
        coordinator.onDashboardRequested()
        assertEquals(AppScreen.Dashboard, coordinator.currentScreen)
    }
}
