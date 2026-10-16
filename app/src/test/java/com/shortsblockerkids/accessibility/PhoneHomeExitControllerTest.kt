package com.shortsblockerkids.accessibility

import com.shortsblockerkids.platform.accessibility.overlay.BlockedSurfaceDepartureObserver
import com.shortsblockerkids.platform.accessibility.overlay.DelayedActionScheduler
import com.shortsblockerkids.platform.accessibility.overlay.ObservedPlaybackWindow
import com.shortsblockerkids.platform.accessibility.overlay.PhoneHomeExitController
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneHomeExitControllerTest {
    @Test
    fun observedDepartureCompletesWhenAnotherNavigationActionIsUnnecessary() {
        val scenario = ExitScenario()
        scenario.actionAccepted = false
        scenario.closed = true
        scenario.controller.exitShorts()
        assertEquals(true, scenario.controller.isExitInProgress)
        scenario.tick()
        scenario.tick()
        assertEquals(1, scenario.completed)
        assertEquals(0, scenario.failed)
        assertEquals(1, scenario.actions)
        assertEquals(false, scenario.controller.isExitInProgress)
    }

    @Test
    fun waitsForTwoConfirmedClosedObservationsBeforeDismissing() {
        val scenario = ExitScenario()
        scenario.closed = true
        scenario.controller.exitShorts()
        assertEquals(0, scenario.completed)
        scenario.tick()
        assertEquals(0, scenario.completed)
        scenario.tick()
        assertEquals(1, scenario.completed)
    }

    @Test
    fun acceptedBackActionDoesNotDismissWhileVideoOrPipRemains() {
        val scenario = ExitScenario()
        scenario.controller.exitShorts()
        repeat(PhoneHomeExitController.MAX_EXIT_ATTEMPTS) { scenario.tick() }
        assertEquals(0, scenario.completed)
        assertEquals(1, scenario.failed)
        assertEquals(PhoneHomeExitController.MAX_EXIT_ATTEMPTS, scenario.actions)
    }

    @Test
    fun pictureInPictureAppearingDuringTransitionKeepsOverlay() {
        val scenario = ExitScenario()
        scenario.closed = true
        scenario.controller.exitShorts()
        scenario.tick()
        scenario.closed = false
        scenario.tick()
        assertEquals(0, scenario.completed)
        assertEquals(2, scenario.actions)
    }

    @Test
    fun failedActionKeepsOverlayAndAllowsRetry() {
        val scenario = ExitScenario()
        scenario.actionAccepted = false
        scenario.controller.exitShorts()
        repeat(PhoneHomeExitController.MAX_EXIT_ATTEMPTS) { scenario.tick() }
        assertEquals(1, scenario.failed)
        assertEquals(0, scenario.completed)
        scenario.actionAccepted = true
        scenario.closed = true
        scenario.controller.exitShorts()
        scenario.tick()
        scenario.tick()
        assertEquals(1, scenario.completed)
    }

    @Test
    fun temporarilyUnavailableWindowIsRetriedBeforeFailing() {
        val scenario = ExitScenario()
        scenario.actionAccepted = false
        scenario.controller.exitShorts()
        assertEquals(0, scenario.failed)
        scenario.actionAccepted = true
        scenario.closed = true
        repeat(3) { scenario.tick() }
        assertEquals(1, scenario.completed)
        assertEquals(0, scenario.failed)
    }

    @Test
    fun cancelledExitCannotDismissNewOverlay() {
        val scenario = ExitScenario()
        scenario.closed = true
        scenario.controller.exitShorts()
        scenario.tick()
        scenario.controller.cancel()
        scenario.tick()
        assertEquals(0, scenario.completed)
    }

    @Test
    fun stableWindowDepartureCanCancelPendingExitWithoutRunningItsStaleCheck() {
        val scenario = ExitScenario()
        val departure = BlockedSurfaceDepartureObserver()
        departure.capture("com.google.android.youtube")
        val launcher =
            ObservedPlaybackWindow(
                windowId = 1,
                packageName = "com.android.launcher",
                isActive = true,
                shouldBlock = false,
            )
        scenario.controller.exitShorts()

        assertEquals(true, scenario.controller.isExitInProgress)
        assertEquals(false, departure.observe(listOf(launcher)))
        assertEquals(true, departure.observe(listOf(launcher)))
        scenario.controller.cancel()
        scenario.tick()

        assertEquals(false, scenario.controller.isExitInProgress)
        assertEquals(1, scenario.actions)
        assertEquals(0, scenario.completed)
        assertEquals(0, scenario.failed)
    }

    @Test
    fun cancelBeforeAcceptedExitCheckDropsScheduledCheck() {
        val scenario = ExitScenario()
        scenario.closed = true
        scenario.controller.exitShorts()

        scenario.controller.cancel()
        scenario.tick()

        assertEquals(0, scenario.completed)
        assertEquals(0, scenario.failed)
    }

    @Test
    fun cancelBeforeFailedActionRetryDropsScheduledRetry() {
        val scenario = ExitScenario()
        scenario.actionAccepted = false
        scenario.controller.exitShorts()

        scenario.controller.cancel()
        scenario.actionAccepted = true
        scenario.closed = true
        scenario.tick()

        assertEquals(0, scenario.completed)
        assertEquals(0, scenario.failed)
        assertEquals(1, scenario.actions)
    }

    @Test
    fun repeatedTapDoesNotStartOverlappingExitActions() {
        val scenario = ExitScenario()
        scenario.controller.exitShorts()
        scenario.controller.exitShorts()
        assertEquals(1, scenario.actions)
    }

    @Test
    fun throwingPlatformActionsExhaustRetryAndRemainRecoverable() {
        val scenario = ExitScenario()
        scenario.actionThrows = true
        scenario.controller.exitShorts()
        repeat(PhoneHomeExitController.MAX_EXIT_ATTEMPTS) { scenario.tick() }
        assertEquals(1, scenario.failed)
        assertEquals(0, scenario.completed)
        scenario.actionThrows = false
        scenario.closed = true
        scenario.controller.exitShorts()
        repeat(2) { scenario.tick() }
        assertEquals(1, scenario.completed)
    }

    @Test
    fun throwingClosureObservationRetainsBarrierUntilRetryExhaustion() {
        val scenario = ExitScenario()
        scenario.closureThrows = true
        scenario.controller.exitShorts()
        repeat(PhoneHomeExitController.MAX_EXIT_ATTEMPTS) { scenario.tick() }
        assertEquals(1, scenario.failed)
        assertEquals(0, scenario.completed)
    }

    @Test
    fun schedulerFailureIsReportedAndDoesNotPermanentlySuppressNewAttempt() {
        var failures = 0
        var actions = 0
        val controller =
            PhoneHomeExitController(
                exitAction = {
                    actions++
                    true
                },
                isPlaybackClosed = { false },
                delayedActionScheduler = DelayedActionScheduler { _, _ -> throw IllegalStateException("Looper stopped") },
                onExitCompleted = { error("Unconfirmed exit") },
                onExitFailed = { failures++ },
            )
        repeat(2) { controller.exitShorts() }
        assertEquals(2, actions)
        assertEquals(2, failures)
    }

    private class ExitScenario {
        var actionAccepted = true
        var closed = false
        var actionThrows = false
        var closureThrows = false
        var actions = 0
        var completed = 0
        var failed = 0
        private val scheduled = ArrayDeque<() -> Unit>()
        val controller =
            PhoneHomeExitController(
                exitAction = {
                    actions += 1
                    if (actionThrows) throw IllegalStateException("Window unavailable")
                    actionAccepted
                },
                isPlaybackClosed = {
                    if (closureThrows) throw IllegalStateException("Window unavailable")
                    closed
                },
                delayedActionScheduler = DelayedActionScheduler { _, action -> scheduled.addLast(action) },
                onExitCompleted = { completed += 1 },
                onExitFailed = { failed += 1 },
            )

        fun tick() {
            scheduled.removeFirst().invoke()
        }
    }
}
