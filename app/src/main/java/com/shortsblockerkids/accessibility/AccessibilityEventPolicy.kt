package com.shortsblockerkids.accessibility

import android.view.accessibility.AccessibilityEvent

class AccessibilityEventPolicy(
    private val scanDebounceMs: Long = DEFAULT_SCAN_DEBOUNCE_MS,
) {
    private var lastScanAtMillis: Long? = null

    fun isRelevantEventType(eventType: Int): Boolean = eventType in relevantEventTypes

    fun shouldScan(
        eventType: Int,
        nowMillis: Long,
    ): Boolean {
        if (!isRelevantEventType(eventType)) {
            return false
        }

        val lastScanAt = lastScanAtMillis
        if (lastScanAt != null && nowMillis - lastScanAt < scanDebounceMs) {
            return false
        }

        lastScanAtMillis = nowMillis
        return true
    }

    fun reset() {
        lastScanAtMillis = null
    }

    companion object {
        const val DEFAULT_SCAN_DEBOUNCE_MS = 500L

        private val relevantEventTypes =
            setOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            )
    }
}
