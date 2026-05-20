package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingDecisionControllerTest {
    @Test
    fun blocksOnlyHighConfidenceShorts() {
        val controller = BlockingDecisionController()

        val lowDecision =
            controller.evaluate(
                isInYouTube = true,
                isProtectionActive = true,
                result = DetectionResult(false, Confidence.LOW, emptyList()),
                nowMillis = 1_000L,
            )
        val highDecision =
            controller.evaluate(
                isInYouTube = true,
                isProtectionActive = true,
                result = DetectionResult(true, Confidence.HIGH, listOf("test")),
                nowMillis = 2_000L,
            )

        assertEquals(BlockingDecision.Ignore, lowDecision)
        assertEquals(BlockingDecision.ShowOverlay, highDecision)
    }

    @Test
    fun normalYouTubeVideoDoesNotShowBlocker() {
        val controller = BlockingDecisionController()

        val decision =
            controller.evaluate(
                isInYouTube = true,
                isProtectionActive = true,
                result = DetectionResult.None,
                nowMillis = 1_000L,
            )

        assertEquals(BlockingDecision.Ignore, decision)
    }

    @Test
    fun shortsDetectedLaunchesBlockerOnceWhileVisible() {
        val controller = BlockingDecisionController(detectionDebounceMs = 0L)

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        assertEquals(BlockingDecision.Ignore, controller.evaluateHigh(nowMillis = 2_500L))
        assertTrue(controller.shouldIgnoreNonYouTubeEvent(nowMillis = 2_600L))
    }

    @Test
    fun blockerNotVisibleAndShortsDetectedLaunchesBlocker() {
        val controller = BlockingDecisionController()

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
    }

    @Test
    fun manyIdenticalShortsEventsLaunchBlockerOnlyOnce() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
            )
        val decisions = mutableListOf<BlockingDecision>()

        repeat(25) { index ->
            decisions += controller.evaluateHigh(nowMillis = 1_000L + index * 100L)
        }

        assertEquals(1, decisions.count { it == BlockingDecision.ShowOverlay })
        assertEquals(24, decisions.count { it == BlockingDecision.Ignore })
    }

    @Test
    fun eventStormDoesNotRestartBlockingActivity() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
            )
        var launchCount = 0

        repeat(100) { index ->
            if (controller.evaluateHigh(nowMillis = 1_000L + index * 10L) == BlockingDecision.ShowOverlay) {
                launchCount += 1
            }
        }

        assertEquals(1, launchCount)
    }

    @Test
    fun cooldownPreventsRepeatedBlocksAfterOverlayDismissal() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 1_200L,
                detectionDebounceMs = 0L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.onOverlayDismissed()
        assertEquals(BlockingDecision.Ignore, controller.evaluateHigh(nowMillis = 1_800L))
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 2_300L))
    }

    @Test
    fun debounceIgnoresRapidHighConfidenceEvents() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 500L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.onOverlayDismissed()
        assertEquals(BlockingDecision.Ignore, controller.evaluateHigh(nowMillis = 1_300L))
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_900L))
    }

    @Test
    fun blockerAlreadyVisiblePreventsNonYoutubeEventsFromResettingState() {
        val controller = BlockingDecisionController()

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))

        assertTrue(controller.shouldIgnoreNonYouTubeEvent(nowMillis = 1_100L))
        assertEquals(BlockingDecision.Ignore, controller.evaluateHigh(nowMillis = 3_000L))
    }

    @Test
    fun pinEntryLaunchGraceKeepsPinScreenStableDuringRepeatedDetections() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
                pinEntryLaunchGraceMs = 5_000L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.onPinEntryRequested(nowMillis = 1_100L)
        controller.onOverlayDismissed()

        assertTrue(controller.shouldIgnoreNonYouTubeEvent(nowMillis = 1_200L))
        assertEquals(BlockingDecision.Ignore, controller.evaluateHigh(nowMillis = 2_000L))
        assertEquals(BlockingDecision.Ignore, controller.evaluateHigh(nowMillis = 6_000L))
    }

    @Test
    fun pinInputDoesNotLoseFocusFromRepeatedEventsDuringPinEntryGrace() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
                pinEntryLaunchGraceMs = 5_000L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.onPinEntryRequested(nowMillis = 1_100L)
        controller.onOverlayDismissed()

        val repeatedDecisions =
            (1..20).map { index ->
                controller.evaluateHigh(nowMillis = 1_200L + index * 100L)
            }

        assertTrue(repeatedDecisions.all { it == BlockingDecision.Ignore })
    }

    @Test
    fun pinEntryLaunchGraceDoesNotPermanentlyDisableBlocking() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
                pinEntryLaunchGraceMs = 5_000L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.onPinEntryRequested(nowMillis = 1_100L)
        controller.onOverlayDismissed()

        assertFalse(controller.shouldIgnoreNonYouTubeEvent(nowMillis = 6_200L))
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 6_200L))
    }

    @Test
    fun cooldownPreventsSecondLaunchWithinWindow() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 1_200L,
                detectionDebounceMs = 0L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.onOverlayDismissed()
        assertEquals(BlockingDecision.Ignore, controller.evaluateHigh(nowMillis = 2_000L))
    }

    @Test
    fun shortsTriggerAfterCooldownCanLaunchAgain() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 1_200L,
                detectionDebounceMs = 0L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.onOverlayDismissed()
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 2_300L))
    }

    @Test
    fun cooldownDoesNotAllowBypassAfterWindowExpires() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 1_200L,
                detectionDebounceMs = 0L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.onOverlayDismissed()
        assertEquals(BlockingDecision.Ignore, controller.evaluateHigh(nowMillis = 1_500L))
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 2_300L))
    }

    @Test
    fun blockerClosedActivityDestroyedClearsState() {
        val controller = BlockingDecisionController()

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.reset()

        assertFalse(controller.shouldIgnoreNonYouTubeEvent(nowMillis = 1_100L))
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 3_000L))
    }

    @Test
    fun accessibilityServiceRestartStartsWithCleanBlockerState() {
        val oldController = BlockingDecisionController()
        assertEquals(BlockingDecision.ShowOverlay, oldController.evaluateHigh(nowMillis = 1_000L))

        val restartedController = BlockingDecisionController()

        assertEquals(BlockingDecision.ShowOverlay, restartedController.evaluateHigh(nowMillis = 1_100L))
    }

    @Test
    fun orientationBackgroundForegroundEventsDoNotCreateRepeatedLaunches() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        repeat(10) { index ->
            assertTrue(controller.shouldIgnoreNonYouTubeEvent(nowMillis = 1_100L + index * 50L))
            assertEquals(BlockingDecision.Ignore, controller.evaluateHigh(nowMillis = 1_150L + index * 50L))
        }
    }

    @Test
    fun mainActivityLaunchDoesNotTriggerFakeShortsBlocking() {
        val controller = BlockingDecisionController()

        assertEquals(
            BlockingDecision.DismissOverlay,
            controller.evaluate(
                isInYouTube = false,
                isProtectionActive = true,
                result = highResult(),
                nowMillis = 1_000L,
            ),
        )
    }

    @Test
    fun reopeningYouTubeAfterUnlockFollowsExistingProtectionState() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
                pinEntryLaunchGraceMs = 5_000L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        controller.onPinEntryRequested(nowMillis = 1_100L)
        controller.onOverlayDismissed()
        assertEquals(
            BlockingDecision.DismissOverlay,
            controller.evaluate(
                isInYouTube = true,
                isProtectionActive = false,
                result = DetectionResult.None,
                nowMillis = 2_000L,
            ),
        )
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 8_000L))
    }

    @Test
    fun protectionInactiveDismissesOverlayState() {
        val controller = BlockingDecisionController()

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        assertEquals(
            BlockingDecision.DismissOverlay,
            controller.evaluate(
                isInYouTube = true,
                isProtectionActive = false,
                result = DetectionResult.None,
                nowMillis = 1_100L,
            ),
        )
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 3_000L))
    }

    @Test
    fun youtubeExitDismissesOverlayState() {
        val controller = BlockingDecisionController()

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        assertEquals(
            BlockingDecision.DismissOverlay,
            controller.evaluate(
                isInYouTube = false,
                isProtectionActive = true,
                result = DetectionResult.None,
                nowMillis = 1_100L,
            ),
        )
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 3_000L))
    }

    @Test
    fun temporaryAllowDismissesOverlayState() {
        val controller = BlockingDecisionController()

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        assertEquals(
            BlockingDecision.DismissOverlay,
            controller.evaluate(
                isInYouTube = true,
                isProtectionActive = false,
                result = DetectionResult.None,
                nowMillis = 1_100L,
            ),
        )
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 3_000L))
    }

    private fun BlockingDecisionController.evaluateHigh(nowMillis: Long): BlockingDecision =
        evaluate(
            isInYouTube = true,
            isProtectionActive = true,
            result = highResult(),
            nowMillis = nowMillis,
        )

    private fun highResult(): DetectionResult = DetectionResult(true, Confidence.HIGH, listOf("test"))
}
