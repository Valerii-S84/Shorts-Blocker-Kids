package com.shortsblockerkids.accessibility

import com.shortsblockerkids.domain.detection.Confidence
import com.shortsblockerkids.domain.detection.DetectionResult
import com.shortsblockerkids.platform.accessibility.overlay.canExitAfterOverlayFailure
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayFallbackContextTest {
    private val high = DetectionResult(isShorts = true, confidence = Confidence.HIGH, reasons = emptyList())

    @Test fun currentHighDetectionAllowsExit() {
        assertTrue(canExitAfterOverlayFailure("video.app", "video.app", high))
    }

    @Test fun changedOrMissingPackageNeverNavigates() {
        assertFalse(canExitAfterOverlayFailure("video.app", "other.app", high))
        assertFalse(canExitAfterOverlayFailure("video.app", null, high))
    }

    @Test fun leavingShortsInSamePackageStopsNavigation() {
        assertFalse(canExitAfterOverlayFailure("video.app", "video.app", DetectionResult.None))
        assertFalse(canExitAfterOverlayFailure("video.app", "video.app", high.copy(confidence = Confidence.MEDIUM)))
    }
}
