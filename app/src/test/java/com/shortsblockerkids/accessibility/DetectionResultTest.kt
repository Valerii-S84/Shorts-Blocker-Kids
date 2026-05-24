package com.shortsblockerkids.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionResultTest {
    @Test
    fun shouldBlockRequiresShortsAndHighConfidence() {
        assertTrue(DetectionResult(isShorts = true, confidence = Confidence.HIGH, reasons = emptyList()).shouldBlock)
        assertFalse(DetectionResult(isShorts = true, confidence = Confidence.MEDIUM, reasons = emptyList()).shouldBlock)
        assertFalse(DetectionResult(isShorts = false, confidence = Confidence.HIGH, reasons = emptyList()).shouldBlock)
    }
}
