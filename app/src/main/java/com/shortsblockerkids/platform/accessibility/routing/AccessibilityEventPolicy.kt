package com.shortsblockerkids.platform.accessibility.routing

import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.application.port.MonotonicTimeProvider

/** Uses the injected session clock; explicit nowMillis must be monotonic when no clock is supplied. */
class AccessibilityEventPolicy(
    private val monotonicTimeProvider: MonotonicTimeProvider? = null,
    private val scanDebounceMs: Long = DEFAULT_SCAN_DEBOUNCE_MS,
    private val requestRescan: (Long) -> Unit = {},
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

        val intervalNowMillis = monotonicTimeProvider?.elapsedRealtimeMillis() ?: nowMillis
        val lastScanAt = lastScanAtMillis
        if (lastScanAt != null && intervalNowMillis - lastScanAt < scanDebounceMs) {
            requestRescan(scanDebounceMs - (intervalNowMillis - lastScanAt))
            return false
        }

        lastScanAtMillis = intervalNowMillis
        return true
    }

    fun reset() {
        lastScanAtMillis = null
    }

    fun onRootUnavailable() {
        requestRescan(scanDebounceMs)
    }

    fun isRootPackageConsistent(
        eventPackage: String,
        rootPackage: String?,
    ): Boolean {
        if (rootPackage == eventPackage) return true
        requestRescan(scanDebounceMs)
        return false
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
