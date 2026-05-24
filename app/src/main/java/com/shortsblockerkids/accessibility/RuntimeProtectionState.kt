package com.shortsblockerkids.accessibility

import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.BuildConfig

object RuntimeProtectionState {
    @Volatile
    private var lastDetectorResult: LastDetectorResult? = null

    @Volatile
    private var debugOverlayRequested = false

    @Volatile
    private var debugSnapshotRequested = false

    @Volatile
    private var lastDebugSnapshot: DetectorDebugSnapshot? = null

    @Volatile
    private var lastBlockingDecision: BlockingDecision? = null

    fun recordDetectorResult(
        packageName: String,
        eventType: Int,
        result: DetectionResult,
        snapshot: AccessibilityTreeSnapshot,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            return
        }

        lastDetectorResult =
            LastDetectorResult(
                packageName = packageName,
                eventType = AccessibilityEvent.eventTypeToString(eventType),
                confidence = result.confidence,
                reasons = result.reasons,
                matchedSignals = result.matchedSignals,
                snapshotSummary = snapshot.toDebugSummary(),
                recordedAtMillis = nowMillis,
            )
    }

    fun clearDetectorResult() {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            lastDetectorResult = null
            lastBlockingDecision = null
        }
    }

    fun recordBlockingDecision(decision: BlockingDecision) {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            lastBlockingDecision = decision
        }
    }

    fun lastBlockingDecisionText(): String? {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            return null
        }

        return lastBlockingDecision?.name ?: "none"
    }

    fun lastDetectorResultText(): String? {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            return null
        }

        val result = lastDetectorResult ?: return "none"
        return "package=${result.packageName} event=${result.eventType} " +
            "confidence=${result.confidence} reasons=${result.reasons} " +
            "signals=${result.matchedSignals} " +
            "snapshot=${result.snapshotSummary} at=${result.recordedAtMillis}"
    }

    fun lastDebugSnapshotText(): String? {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            return null
        }

        val snapshot = lastDebugSnapshot ?: return "none"
        return "saved=${snapshot.path} summary=${snapshot.summary}"
    }

    fun requestDebugOverlay() {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            debugOverlayRequested = true
        }
    }

    fun consumeDebugOverlayRequest(): Boolean {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED || !debugOverlayRequested) {
            return false
        }

        debugOverlayRequested = false
        return true
    }

    fun requestDebugSnapshot() {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            debugSnapshotRequested = true
        }
    }

    fun consumeDebugSnapshotRequest(): Boolean {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED || !debugSnapshotRequested) {
            return false
        }

        debugSnapshotRequested = false
        return true
    }

    fun isDebugSnapshotPending(): Boolean = BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED && debugSnapshotRequested

    fun recordDebugSnapshot(snapshot: DetectorDebugSnapshot) {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            lastDebugSnapshot = snapshot
        }
    }
}

private data class LastDetectorResult(
    val packageName: String,
    val eventType: String,
    val confidence: Confidence,
    val reasons: List<String>,
    val matchedSignals: List<String>,
    val snapshotSummary: String,
    val recordedAtMillis: Long,
)
