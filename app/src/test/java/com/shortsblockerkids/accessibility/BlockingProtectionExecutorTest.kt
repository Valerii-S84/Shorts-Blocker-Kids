package com.shortsblockerkids.accessibility

import com.shortsblockerkids.platform.accessibility.routing.BlockingProtectionExecutor
import com.shortsblockerkids.platform.accessibility.overlay.DelayedActionScheduler
import com.shortsblockerkids.platform.accessibility.overlay.PhoneHomeExitController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingProtectionExecutorTest {
    @Test fun successfulOverlayIsRecordedAfterItIsShown() {
        val events = mutableListOf<String>()
        val executor = BlockingProtectionExecutor(
            { events += "shown"; true }, { _, _ -> error("Unexpected fallback") },
            { events += it }, {},
            { packageName, completed -> events += "exit:$packageName"; completed(true) },
        )
        executor.protect("video.app")
        assertEquals(
            listOf("shown", "ShowOverlay", "AutomaticPlaybackExitPending", "exit:video.app", "PlaybackExitConfirmed"),
            events,
        )
    }

    @Test fun overlayFailureRunsFallbackAndWaitsForConfirmation() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        assertEquals(1, scenario.fallbacks)
        assertEquals(listOf("OverlayFailedFallbackPending"), scenario.outcomes)
        scenario.complete(true)
        assertEquals("FallbackExecuted", scenario.outcomes.last())
        assertEquals(1, scenario.dismissals)
    }

    @Test fun failedFallbackIsDegradedAndCannotLoopOnRepeatedDetections() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.complete(false)
        repeat(5) { scenario.executor.protect("video.app") }
        assertEquals(1, scenario.fallbacks)
        assertEquals("ProtectionFailed", scenario.outcomes.last())
        assertTrue("ShowOverlay" !in scenario.outcomes)
    }

    @Test fun resetCancelsStaleOutcomeAndAllowsNewContext() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        val oldCompletion = scenario.complete
        scenario.executor.reset()
        oldCompletion(true)
        assertTrue("FallbackExecuted" !in scenario.outcomes)
        scenario.executor.protect("video.app")
        assertEquals(2, scenario.fallbacks)
    }

    @Test fun pendingFallbackDoesNotStartAnotherAttempt() {
        val scenario = Scenario()
        repeat(3) { scenario.executor.protect("video.app") }
        assertEquals(1, scenario.fallbacks)
    }

    @Test fun automaticExitWaitsForTwoClosedObservationsWithoutButtonClick() {
        val scenario = AutomaticScenario()
        scenario.closed = true
        scenario.executor.protect("video.app")
        assertEquals(1, scenario.actions)
        assertTrue(scenario.overlayVisible)
        scenario.tick()
        assertTrue(scenario.overlayVisible)
        scenario.tick()
        assertEquals(false, scenario.overlayVisible)
        assertEquals("PlaybackExitConfirmed", scenario.outcomes.last())
    }

    @Test fun automaticExitFailureIsBoundedAndRetainsOverlay() {
        val scenario = AutomaticScenario()
        scenario.executor.protect("video.app")
        repeat(PhoneHomeExitController.MAX_EXIT_ATTEMPTS) { scenario.tick() }
        assertEquals(PhoneHomeExitController.MAX_EXIT_ATTEMPTS, scenario.actions)
        assertTrue(scenario.overlayVisible)
        assertEquals("OverlayRetainedExitUnconfirmed", scenario.outcomes.last())
        assertTrue("PlaybackExitConfirmed" !in scenario.outcomes)
    }

    @Test fun pendingAutomaticExitCannotStartDuplicateAttempts() {
        val scenario = AutomaticScenario()
        repeat(3) { scenario.executor.protect("video.app") }
        assertEquals(1, scenario.actions)
        assertEquals(1, scenario.outcomes.count { it == "ShowOverlay" })
    }

    @Test fun resetDiscardsStaleAutomaticExitOutcome() {
        val scenario = AutomaticScenario()
        scenario.closed = true
        scenario.executor.protect("video.app")
        scenario.executor.reset()
        scenario.tick()
        scenario.tick()
        assertTrue("PlaybackExitConfirmed" !in scenario.outcomes)
        scenario.executor.protect("other.app")
        assertEquals(2, scenario.actions)
    }

    private class AutomaticScenario {
        val outcomes = mutableListOf<String>()
        val scheduled = ArrayDeque<() -> Unit>()
        var overlayVisible = false
        var actions = 0
        var closed = false
        val executor = BlockingProtectionExecutor(
            showOverlay = { overlayVisible = true; true },
            startFallback = { _, _ -> error("Unexpected overlay failure") },
            recordOutcome = { outcomes += it },
            onOverlayFailed = { error("Successful overlay must be retained") },
            startPlaybackExit = { _, completed ->
                PhoneHomeExitController(
                    exitAction = { actions++; true },
                    isPlaybackClosed = { closed },
                    delayedActionScheduler = DelayedActionScheduler { _, action -> scheduled.addLast(action) },
                    onExitCompleted = { completed(true); overlayVisible = false },
                    onExitFailed = { completed(false) },
                ).exitShorts()
            },
        )

        fun tick() = scheduled.removeFirst().invoke()
    }

    private class Scenario {
        val outcomes = mutableListOf<String>()
        var fallbacks = 0
        var dismissals = 0
        var complete: (Boolean) -> Unit = {}
        val executor = BlockingProtectionExecutor(
            { false }, { _, callback -> fallbacks++; complete = callback },
            { outcomes += it }, { dismissals++ },
            { _, _ -> error("Overlay failure must use fallback") },
        )
    }
}
