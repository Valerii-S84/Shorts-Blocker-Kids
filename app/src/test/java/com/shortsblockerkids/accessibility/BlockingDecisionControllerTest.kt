package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
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
            result = DetectionResult(true, Confidence.HIGH, listOf("test")),
            nowMillis = nowMillis,
        )
}
