package com.shortsblockerkids.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.BuildConfig

class DebugAccessibilityLogger {
    fun logIgnoredEvent(
        packageName: String?,
        eventType: Int,
        reason: String,
    ) {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            return
        }

        Log.d(
            TAG,
            "ignored package=$packageName event=${AccessibilityEvent.eventTypeToString(eventType)} reason=$reason",
        )
    }

    fun logDetection(
        packageName: String?,
        eventType: Int,
        result: DetectionResult,
        snapshot: AccessibilityTreeSnapshot,
    ) {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            return
        }

        Log.d(
            TAG,
            "package=$packageName event=${AccessibilityEvent.eventTypeToString(eventType)} " +
                "confidence=${result.confidence} reasons=${result.reasons} " +
                "signals=${result.matchedSignals} " +
                "snapshot=${snapshot.toDebugSummary()}",
        )
    }

    companion object {
        private const val TAG = "ShortsBlocker"
    }
}
