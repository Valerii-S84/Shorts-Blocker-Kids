package com.shortsblockerkids.accessibility

import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.application.port.MonotonicTimeProvider
import com.shortsblockerkids.domain.detection.Confidence
import com.shortsblockerkids.domain.detection.DetectionResult
import com.shortsblockerkids.platform.accessibility.routing.AccessibilityEventPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeIntervalClockTest {
    @Test
    fun scanDebounceSurvivesBackwardAndForwardWallClockJumps() {
        for (wallJump in listOf(-86_400_000L, 86_400_000L)) {
            val clock = Clock()
            val delays = mutableListOf<Long>()
            val policy = AccessibilityEventPolicy(monotonicTimeProvider = clock, requestRescan = delays::add)
            assertTrue(policy.shouldScan(EVENT, 100_000L))
            clock.elapsed += 100L
            assertFalse(policy.shouldScan(EVENT, 100_000L + wallJump))
            assertEquals(listOf(400L), delays)
            clock.elapsed += 400L
            assertTrue(policy.shouldScan(EVENT, 100_500L + wallJump))
        }
    }

    @Test
    fun blockingCooldownExpiresOnlyAfterElapsedIntervalDespiteWallClockJumps() {
        for (wallJump in listOf(-86_400_000L, 86_400_000L)) {
            val clock = Clock()
            val controller = BlockingDecisionController(monotonicTimeProvider = clock, detectionDebounceMs = 0L)
            assertEquals(BlockingDecision.ShowOverlay, controller.high(100_000L))
            controller.onOverlayDismissed()
            clock.elapsed += 1_199L
            assertEquals(BlockingDecision.Ignore, controller.high(100_000L + wallJump))
            clock.elapsed += 1L
            assertEquals(BlockingDecision.ShowOverlay, controller.high(100_001L + wallJump))
        }
    }

    @Test
    fun detectionDebounceUsesElapsedTimeDespiteWallClockJumps() {
        for (wallJump in listOf(-86_400_000L, 86_400_000L)) {
            val clock = Clock()
            val controller = BlockingDecisionController(monotonicTimeProvider = clock, blockCooldownMs = 0L)
            assertEquals(BlockingDecision.ShowOverlay, controller.high(100_000L))
            controller.onOverlayDismissed()
            clock.elapsed += 100L
            assertEquals(BlockingDecision.Ignore, controller.high(100_000L + wallJump))
            clock.elapsed += 500L
            assertEquals(BlockingDecision.ShowOverlay, controller.high(100_001L + wallJump))
        }
    }

    @Test
    fun pinLaunchGraceExpiresOnlyAfterElapsedIntervalDespiteWallClockJumps() {
        for (wallJump in listOf(-86_400_000L, 86_400_000L)) {
            val clock = Clock()
            val controller = BlockingDecisionController(monotonicTimeProvider = clock)
            controller.onPinEntryRequested(100_000L)
            clock.elapsed += 4_999L
            assertTrue(controller.shouldIgnoreUnsupportedPackageEvent(100_000L + wallJump))
            assertEquals(BlockingDecision.Ignore, controller.high(100_000L + wallJump))
            clock.elapsed += 1L
            assertFalse(controller.shouldIgnoreUnsupportedPackageEvent(100_001L + wallJump))
            assertEquals(BlockingDecision.ShowOverlay, controller.high(100_001L + wallJump))
        }
    }

    private class Clock(var elapsed: Long = 10_000L) : MonotonicTimeProvider {
        override fun elapsedRealtimeMillis(): Long = elapsed
    }

    private fun BlockingDecisionController.high(wallMillis: Long): BlockingDecision =
        evaluate(true, true, DetectionResult(true, Confidence.HIGH, listOf("test")), wallMillis)

    companion object {
        private const val EVENT = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    }
}
