package com.shortsblockerkids.accessibility

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityEventPolicyTest {
    @Test
    fun acceptsOnlyRelevantYouTubeEventTypes() {
        val policy = AccessibilityEventPolicy()

        assertTrue(policy.isRelevantEventType(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
        assertTrue(policy.isRelevantEventType(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED))
        assertTrue(policy.isRelevantEventType(AccessibilityEvent.TYPE_VIEW_SCROLLED))
        assertTrue(policy.isRelevantEventType(AccessibilityEvent.TYPE_WINDOWS_CHANGED))
        assertFalse(policy.isRelevantEventType(AccessibilityEvent.TYPE_VIEW_CLICKED))
    }

    @Test
    fun debouncesTreeScansForRapidRelevantEvents() {
        val policy = AccessibilityEventPolicy(scanDebounceMs = 500L)

        assertTrue(policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, nowMillis = 1_000L))
        assertFalse(policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, nowMillis = 1_200L))
        assertTrue(policy.shouldScan(AccessibilityEvent.TYPE_VIEW_SCROLLED, nowMillis = 1_500L))
    }

    @Test
    fun resetClearsDebounceState() {
        val policy = AccessibilityEventPolicy(scanDebounceMs = 500L)

        assertTrue(policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, nowMillis = 1_000L))
        policy.reset()
        assertTrue(policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, nowMillis = 1_100L))
    }
}
