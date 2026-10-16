package com.shortsblockerkids.accessibility

import com.shortsblockerkids.platform.accessibility.routing.BlockingProtectionExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingProtectionExecutorTest {
    @Test fun canceledParentNavigationAllowsSamePackageProtectionAgain() {
        val scenario = StableScenario()
        scenario.executor.protect("video.app")
        assertEquals(listOf("video.app"), scenario.shownPackages)
        scenario.executor.reset()
        scenario.executor.protect("video.app")
        assertEquals(listOf("video.app", "video.app"), scenario.shownPackages)
        assertEquals(2, scenario.outcomes.count { it == "ShowOverlay" })
    }

    @Test fun duplicateFallbackCompletionCannotChangeSettledOutcomeOrScheduleRetry() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        val completion = scenario.complete
        completion(true)
        val settledOutcomes = scenario.outcomes.toList()
        completion(false)
        assertEquals(settledOutcomes, scenario.outcomes)
        assertEquals(1, scenario.dismissals)
        assertTrue(scenario.retries.isEmpty())
    }

    @Test fun repeatedFailuresAndNewEventsShareOneFiniteProtectionBudget() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        repeat(BlockingProtectionExecutor.MAX_PROTECTION_ATTEMPTS) { attempt ->
            scenario.complete(false)
            if (attempt + 1 < BlockingProtectionExecutor.MAX_PROTECTION_ATTEMPTS) {
                scenario.retries.removeFirst().invoke()
            }
        }
        repeat(5) { scenario.executor.protect("video.app") }
        assertEquals(BlockingProtectionExecutor.MAX_PROTECTION_ATTEMPTS, scenario.fallbacks)
        assertTrue(scenario.retries.isEmpty())
        assertEquals("ProtectionRetryExhausted", scenario.outcomes.last())
        scenario.executor.reset()
        scenario.executor.protect("video.app")
        assertEquals(BlockingProtectionExecutor.MAX_PROTECTION_ATTEMPTS + 1, scenario.fallbacks)
    }

    @Test fun permanentlyUnavailableRetryObservationExhaustsSharedBudget() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.guardFailures = 100
        scenario.complete(false)
        repeat(BlockingProtectionExecutor.MAX_PROTECTION_ATTEMPTS - 1) {
            scenario.retries.removeFirst().invoke()
        }
        assertTrue(scenario.retries.isEmpty())
        assertEquals(1, scenario.fallbacks)
        assertEquals("ProtectionRetryExhausted", scenario.outcomes.last())
    }

    @Test fun successfulOverlayIsRecordedAfterItIsShown() {
        val events = mutableListOf<String>()
        val executor =
            BlockingProtectionExecutor(
                {
                    events += "shown"
                    true
                },
                { _, _ -> error("Unexpected fallback") },
                { events += it },
                {},
            )
        executor.protect("video.app")
        assertEquals(
            listOf("shown", "ShowOverlay"),
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

    @Test fun failedFallbackCanRetrySamePackageAfterFailure() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.complete(false)
        scenario.executor.protect("video.app")
        assertEquals(2, scenario.fallbacks)
        assertTrue("ProtectionFailed" in scenario.outcomes)
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

    @Test fun failedFallbackAutomaticallyRetriesWithoutAnotherEvent() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.complete(false)
        assertEquals(1, scenario.retries.size)
        scenario.retries.removeFirst().invoke()
        assertEquals(2, scenario.fallbacks)
    }

    @Test fun changedPackageCancelsScheduledProtectionRetry() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.complete(false)
        scenario.stillBlocked = false
        scenario.retries.removeFirst().invoke()
        assertEquals(1, scenario.fallbacks)
    }

    @Test fun retryGuardFailureKeepsCheckingUntilObservationRecovers() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.complete(false)
        scenario.guardFailures = 2
        repeat(2) {
            scenario.retries.removeFirst().invoke()
            assertEquals(1, scenario.fallbacks)
            assertEquals(1, scenario.retries.size)
        }
        scenario.retries.removeFirst().invoke()
        assertEquals(2, scenario.fallbacks)
        assertEquals(2, scenario.outcomes.count { it == "ProtectionRetryCheckFailed" })
        scenario.complete(true)
        assertTrue(scenario.retries.isEmpty())
    }

    @Test fun recoveredNegativeGuardStopsRetryAfterObservationFailure() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.complete(false)
        scenario.guardFailures = 1
        scenario.retries.removeFirst().invoke()
        scenario.stillBlocked = false
        scenario.retries.removeFirst().invoke()
        assertEquals(1, scenario.fallbacks)
        assertTrue(scenario.retries.isEmpty())
    }

    @Test fun resetCancelsRetryRearmedAfterObservationFailure() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.complete(false)
        scenario.guardFailures = 1
        scenario.retries.removeFirst().invoke()
        scenario.executor.reset()
        scenario.retries.removeFirst().invoke()
        assertEquals(1, scenario.fallbacks)
        assertTrue(scenario.retries.isEmpty())
    }

    @Test fun resetCancelsScheduledProtectionRetry() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.complete(false)
        scenario.executor.reset()
        scenario.retries.removeFirst().invoke()
        assertEquals(1, scenario.fallbacks)
    }

    @Test fun thrownOverlayAndFallbackAreControlledAndRetryable() {
        val outcomes = mutableListOf<String>()
        val retries = ArrayDeque<() -> Unit>()
        val executor =
            BlockingProtectionExecutor(
                { throw IllegalStateException("Window unavailable") },
                { _, _ -> throw IllegalStateException("Exit unavailable") },
                { outcomes += it },
                {},
                scheduleRetry = { _, action -> retries.addLast(action) },
            )
        executor.protect("video.app")
        assertEquals("ProtectionFailed", outcomes.last())
        retries.removeFirst().invoke()
        assertEquals(2, outcomes.count { it == "ProtectionFailed" })
    }

    @Test fun successfulOverlaySchedulesNoAutomaticNavigation() {
        val scenario = StableScenario()
        scenario.executor.protect("video.app")
        assertEquals(listOf("video.app"), scenario.shownPackages)
        assertEquals(listOf("ShowOverlay"), scenario.outcomes)
        assertEquals(0, scenario.fallbacks)
        assertTrue(scenario.scheduled.isEmpty())
    }

    @Test fun stableOverlayDoesNotConsumeFallbackBudgetOnRepeatedEvents() {
        val scenario = StableScenario()
        repeat(BlockingProtectionExecutor.MAX_PROTECTION_ATTEMPTS + 3) { scenario.executor.protect("video.app") }
        assertEquals(listOf("video.app"), scenario.shownPackages)
        assertEquals(0, scenario.fallbacks)
        assertEquals(listOf("ShowOverlay"), scenario.outcomes)
    }

    @Test fun observedDepartureAllowsFreshOverlayForSamePackage() {
        val scenario = StableScenario()
        scenario.executor.protect("video.app")
        scenario.executor.reset()
        scenario.executor.protect("video.app")
        assertEquals(listOf("video.app", "video.app"), scenario.shownPackages)
        assertEquals(0, scenario.fallbacks)
    }

    @Test fun anotherPackageCannotCreateDuplicateOverlayBeforeCurrentDismissal() {
        val scenario = StableScenario()
        scenario.executor.protect("video.app")
        scenario.executor.protect("other.app")
        assertEquals(listOf("video.app"), scenario.shownPackages)
        scenario.executor.reset()
        scenario.executor.protect("other.app")
        assertEquals(listOf("video.app", "other.app"), scenario.shownPackages)
        assertEquals(0, scenario.fallbacks)
    }

    @Test fun successfulOverlayAfterFailureInvalidatesOldScheduledRetry() {
        val scenario = Scenario()
        scenario.executor.protect("video.app")
        scenario.complete(false)
        scenario.overlayAvailable = true
        scenario.executor.protect("video.app")
        scenario.retries.removeFirst().invoke()
        assertEquals(1, scenario.fallbacks)
        assertEquals("ShowOverlay", scenario.outcomes.last())
        assertTrue(scenario.retries.isEmpty())
    }

    private class StableScenario {
        val outcomes = mutableListOf<String>()
        val scheduled = ArrayDeque<() -> Unit>()
        val shownPackages = mutableListOf<String>()
        var fallbacks = 0
        val executor =
            BlockingProtectionExecutor(
                showOverlay = {
                    shownPackages += it
                    true
                },
                startFallback = { _, _ -> fallbacks++ },
                recordOutcome = { outcomes += it },
                onOverlayFailed = { error("Successful overlay must be retained") },
                scheduleRetry = { _, action -> scheduled.addLast(action) },
            )
    }

    private class Scenario {
        val outcomes = mutableListOf<String>()
        var fallbacks = 0
        var dismissals = 0
        var stillBlocked = true
        var guardFailures = 0
        var overlayAvailable = false
        val retries = ArrayDeque<() -> Unit>()
        var complete: (Boolean) -> Unit = {}
        val executor =
            BlockingProtectionExecutor(
                { overlayAvailable },
                { _, callback ->
                    fallbacks++
                    complete = callback
                },
                { outcomes += it },
                { dismissals++ },
                scheduleRetry = { _, action -> retries.addLast(action) },
                shouldRetryProtection = {
                    if (guardFailures > 0) {
                        guardFailures--
                        throw IllegalStateException("Active window unavailable")
                    }
                    stillBlocked
                },
            )
    }
}
