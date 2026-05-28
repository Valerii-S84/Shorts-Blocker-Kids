package com.shortsblockerkids.accessibility

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityServiceLifecycleTest {
    @Test
    fun serviceShutdownClearsBlockingStateAndScanDebounce() {
        val eventPolicy = AccessibilityEventPolicy(scanDebounceMs = 500L)
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
            )

        assertTrue(eventPolicy.shouldScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, nowMillis = 1_000L))
        assertFalse(eventPolicy.shouldScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, nowMillis = 1_100L))
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))

        eventPolicy.reset()
        controller.reset()

        assertTrue(eventPolicy.shouldScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, nowMillis = 1_100L))
        assertFalse(controller.shouldIgnoreUnsupportedPackageEvent(nowMillis = 1_100L))
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_100L))
    }

    @Test
    fun serviceRestartDoesNotCarryPinEntryGraceFromPreviousInstance() {
        val oldController =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
                pinEntryLaunchGraceMs = 5_000L,
            )
        assertEquals(BlockingDecision.ShowOverlay, oldController.evaluateHigh(nowMillis = 1_000L))
        oldController.onPinEntryRequested(nowMillis = 1_100L)
        oldController.onOverlayDismissed()

        val restartedController =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
                pinEntryLaunchGraceMs = 5_000L,
            )

        assertTrue(oldController.shouldIgnoreUnsupportedPackageEvent(nowMillis = 1_200L))
        assertFalse(restartedController.shouldIgnoreUnsupportedPackageEvent(nowMillis = 1_200L))
        assertEquals(BlockingDecision.ShowOverlay, restartedController.evaluateHigh(nowMillis = 1_200L))
    }

    @Test
    fun inactiveServiceStateDismissesVisibleOverlayAndAllowsFutureBlocking() {
        val controller =
            BlockingDecisionController(
                blockCooldownMs = 0L,
                detectionDebounceMs = 0L,
            )

        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_000L))
        assertEquals(
            BlockingDecision.DismissOverlay,
            controller.evaluate(
                isInSupportedApp = true,
                isProtectionActive = false,
                result = highResult(),
                nowMillis = 1_100L,
            ),
        )

        assertFalse(controller.shouldIgnoreUnsupportedPackageEvent(nowMillis = 1_200L))
        assertEquals(BlockingDecision.ShowOverlay, controller.evaluateHigh(nowMillis = 1_200L))
    }

    private fun BlockingDecisionController.evaluateHigh(nowMillis: Long): BlockingDecision =
        evaluate(
            isInSupportedApp = true,
            isProtectionActive = true,
            result = highResult(),
            nowMillis = nowMillis,
        )

    private fun highResult(): DetectionResult = DetectionResult(true, Confidence.HIGH, listOf("test"))
}
