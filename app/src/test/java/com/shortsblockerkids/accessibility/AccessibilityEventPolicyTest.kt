package com.shortsblockerkids.accessibility

import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.platform.accessibility.routing.AccessibilityEventPolicy
import org.junit.Assert.assertEquals
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

    @Test
    fun lastContentChangeInsideDebounceRequestsScanWithoutAnotherEvent() {
        val requestedDelays = mutableListOf<Long>()
        val policy = AccessibilityEventPolicy(requestRescan = { requestedDelays += it })

        assertTrue(policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, nowMillis = 1_000L))
        assertFalse(policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, nowMillis = 1_120L))

        assertEquals(listOf(380L), requestedDelays)
        assertTrue(policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, nowMillis = 1_500L))
    }

    @Test
    fun continuousContentChangesDoNotPostponeDeferredScanDeadline() {
        val requestedDelays = mutableListOf<Long>()
        val policy = AccessibilityEventPolicy(requestRescan = { requestedDelays += it })
        policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, nowMillis = 1_000L)

        assertFalse(policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, nowMillis = 1_100L))
        assertFalse(policy.shouldScan(AccessibilityEvent.TYPE_VIEW_SCROLLED, nowMillis = 1_400L))

        assertEquals(listOf(400L, 100L), requestedDelays)
    }

    @Test
    fun unavailableRootRequestsFollowUpAfterWindowTransition() {
        val requestedDelays = mutableListOf<Long>()
        val policy = AccessibilityEventPolicy(requestRescan = { requestedDelays += it })
        policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, nowMillis = 1_000L)

        policy.onRootUnavailable()

        assertEquals(listOf(500L), requestedDelays)
    }

    @Test
    fun irrelevantEventDoesNotRequestDeferredScan() {
        val requestedDelays = mutableListOf<Long>()
        val policy = AccessibilityEventPolicy(requestRescan = { requestedDelays += it })

        assertFalse(policy.shouldScan(AccessibilityEvent.TYPE_VIEW_CLICKED, nowMillis = 1_000L))

        assertTrue(requestedDelays.isEmpty())
    }

    @Test
    fun matchingRootPackageAllowsScanWithoutRescan() {
        val requestedDelays = mutableListOf<Long>()
        val policy = AccessibilityEventPolicy(requestRescan = { requestedDelays += it })

        assertTrue(policy.isRootPackageConsistent("com.google.android.youtube", "com.google.android.youtube"))
        assertTrue(requestedDelays.isEmpty())
    }

    @Test
    fun differentSupportedPlatformRootIsRejectedBeforeUsingEventPlatformGate() {
        val requestedDelays = mutableListOf<Long>()
        val policy = AccessibilityEventPolicy(requestRescan = { requestedDelays += it })

        assertFalse(policy.isRootPackageConsistent("com.google.android.youtube", "com.instagram.android"))
        assertEquals(listOf(500L), requestedDelays)
    }

    @Test
    fun missingOrUnsupportedRootPackageCannotAuthorizeDetection() {
        val requestedDelays = mutableListOf<Long>()
        val policy = AccessibilityEventPolicy(requestRescan = { requestedDelays += it })

        for (rootPackage in listOf(null, "", "com.example.unprotected")) {
            assertFalse(policy.isRootPackageConsistent("com.google.android.youtube", rootPackage))
        }
        assertEquals(listOf(500L, 500L, 500L), requestedDelays)
    }

    @Test
    fun changedRootCanBeScannedByItsOwnFreshEventAfterMismatch() {
        val requestedDelays = mutableListOf<Long>()
        val policy = AccessibilityEventPolicy(requestRescan = { requestedDelays += it })

        assertFalse(policy.isRootPackageConsistent("com.google.android.youtube", "com.instagram.android"))
        assertTrue(policy.isRootPackageConsistent("com.instagram.android", "com.instagram.android"))
        assertTrue(policy.shouldScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, 1_000L))
        assertEquals(listOf(500L), requestedDelays)
    }
}
