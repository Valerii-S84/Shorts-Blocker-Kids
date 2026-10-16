package com.shortsblockerkids.accessibility

import com.shortsblockerkids.platform.accessibility.overlay.BlockedSurfaceDepartureObserver
import com.shortsblockerkids.platform.accessibility.overlay.ObservedPlaybackWindow
import com.shortsblockerkids.platform.accessibility.overlay.hasLeftBlockedSurface
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises the production window policy; device tests cover Android service wiring. */
class AutomaticProtectionWiringTest {
    @Test
    fun departureRequiresTwoMatchingMaterialObservations() {
        val observer = observer()
        val windows = listOf(window("launcher.app"))
        assertFalse(observer.observe(windows))
        assertTrue(observer.observe(windows))
    }

    @Test
    fun changingIrrelevantTreeMetadataDoesNotPreventDepartureConfirmation() {
        val observer = observer()
        assertFalse(observer.observe(listOf(window("video.app").copy(treeSignature = 100))))
        assertTrue(observer.observe(listOf(window("video.app").copy(treeSignature = 200))))
    }

    @Test
    fun unavailableObservationResetsPendingConfirmation() {
        val observer = observer()
        val windows = listOf(window("launcher.app"))
        assertFalse(observer.observe(windows))
        assertFalse(observer.observe(listOf(window("video.app").copy(shouldBlock = null))))
        assertFalse(observer.observe(windows))
        assertTrue(observer.observe(windows))
    }

    @Test
    fun protectedObservationResetsPendingConfirmation() {
        val observer = observer()
        val windows = listOf(window("launcher.app"))
        assertFalse(observer.observe(windows))
        assertFalse(observer.observe(listOf(window("video.app").copy(shouldBlock = true))))
        assertFalse(observer.observe(windows))
        assertTrue(observer.observe(windows))
    }

    @Test
    fun changingWindowIdentityCannotConfirmDeparture() {
        val observer = observer()
        assertFalse(observer.observe(listOf(window("launcher.app"))))
        assertFalse(observer.observe(listOf(window("launcher.app").copy(windowId = 2))))
        assertTrue(observer.observe(listOf(window("launcher.app").copy(windowId = 2))))
    }

    @Test
    fun newCapturedEpisodeDiscardsPriorConfirmation() {
        val observer = observer()
        val windows = listOf(window("launcher.app"))
        assertFalse(observer.observe(windows))
        observer.capture("second.video.app")
        assertFalse(observer.observe(windows))
        assertTrue(observer.observe(windows))
    }

    @Test
    fun noCapturedEpisodeCannotReleaseEvenWhenProtectionIsDisabled() {
        assertFalse(BlockedSurfaceDepartureObserver().observe(emptyList(), isTargetEligible = false))
    }

    @Test
    fun disablingCapturedTargetReleasesPriorPictureInPictureBarrier() {
        val observer = observer()
        val windows = listOf(window("video.app").copy(isPictureInPicture = true, shouldBlock = true))
        assertFalse(observer.observe(windows))
        assertTrue(observer.observe(windows, isTargetEligible = false))
    }

    @Test
    fun positiveLauncherApplicationConfirmsDeparture() {
        assertTrue(hasLeftBlockedSurface("video.app", listOf(window("launcher.app"))))
    }

    @Test
    fun completeNormalScreenInSamePackageCanReleasePriorDetection() {
        assertTrue(hasLeftBlockedSurface("video.app", listOf(window("video.app"))))
    }

    @Test
    fun absentWindowsDoNotConfirmDeparture() {
        assertFalse(hasLeftBlockedSurface("video.app", emptyList()))
    }

    @Test
    fun uncapturedPackageDoesNotConfirmDeparture() {
        assertFalse(hasLeftBlockedSurface(null, listOf(window("launcher.app"))))
        assertFalse(hasLeftBlockedSurface("", listOf(window("launcher.app"))))
    }

    @Test
    fun unreadablePackageDoesNotConfirmDeparture() {
        assertFalse(hasLeftBlockedSurface("video.app", listOf(window(null))))
        assertFalse(hasLeftBlockedSurface("video.app", listOf(window(""))))
    }

    @Test
    fun partialOrUnavailableTreeDoesNotConfirmDeparture() {
        assertFalse(hasLeftBlockedSurface("video.app", listOf(window("video.app").copy(shouldBlock = null))))
    }

    @Test
    fun noKnownActiveApplicationDoesNotConfirmDeparture() {
        assertFalse(hasLeftBlockedSurface("video.app", listOf(window("launcher.app").copy(isActive = false))))
    }

    @Test
    fun protectedSurfaceRetainsBarrier() {
        assertFalse(hasLeftBlockedSurface("video.app", listOf(window("video.app").copy(shouldBlock = true))))
    }

    @Test
    fun blockedPictureInPictureRetainsBarrierOverLauncher() {
        val pip = window("video.app").copy(windowId = 2, isActive = false, isPictureInPicture = true)
        assertFalse(hasLeftBlockedSurface("video.app", listOf(window("launcher.app"), pip)))
    }

    @Test
    fun protectedSplitScreenWindowRetainsBarrierOverAnotherApplication() {
        val split = window("video.app").copy(windowId = 2, isActive = false, shouldBlock = true)
        assertFalse(hasLeftBlockedSurface("video.app", listOf(window("other.app"), split)))
    }

    @Test
    fun unavailableSplitScreenWindowRetainsBarrier() {
        val split = window("video.app").copy(windowId = 2, isActive = false, shouldBlock = null)
        assertFalse(hasLeftBlockedSurface("video.app", listOf(window("other.app"), split)))
    }

    @Test
    fun newProtectedPackageCannotBeReleasedByOldPackageDeparture() {
        assertFalse(hasLeftBlockedSurface("video.app", listOf(window("second.video.app").copy(shouldBlock = true))))
    }

    private fun window(packageName: String?): ObservedPlaybackWindow =
        ObservedPlaybackWindow(windowId = 1, packageName = packageName, isActive = true)

    private fun observer(): BlockedSurfaceDepartureObserver = BlockedSurfaceDepartureObserver().apply { capture("video.app") }
}
