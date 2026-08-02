package com.shortsblockerkids.platform.accessibility.diagnostics

import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.BuildConfig
import com.shortsblockerkids.application.port.AccessibilityDiagnosticsPort
import com.shortsblockerkids.domain.detection.AccessibilityTreeSnapshot
import com.shortsblockerkids.domain.detection.Confidence
import com.shortsblockerkids.domain.detection.DetectionResult

val accessibilityDiagnostics: AccessibilityDiagnosticsPort = RuntimeProtectionState

internal fun configureAccessibilityDiagnostics(
    debugLogger: DebugAccessibilityLogger,
    debugSnapshotStore: DetectorDebugSnapshotStore,
): AccessibilityDiagnosticsPort =
    RuntimeProtectionState.apply {
        configure(
            debugLogger = debugLogger,
            debugSnapshotStore = debugSnapshotStore,
        )
    }

private object RuntimeProtectionState : AccessibilityDiagnosticsPort {
    private lateinit var debugLogger: DebugAccessibilityLogger
    private lateinit var debugSnapshotStore: DetectorDebugSnapshotStore

    @Volatile
    private var lastDetectorResult: LastDetectorResult? = null

    @Volatile
    private var debugOverlayRequested = false

    @Volatile
    private var debugSnapshotRequested = false

    @Volatile
    private var lastDebugSnapshot: DetectorDebugSnapshot? = null

    @Volatile
    private var lastBlockingDecision: String? = null

    fun configure(
        debugLogger: DebugAccessibilityLogger,
        debugSnapshotStore: DetectorDebugSnapshotStore,
    ) {
        this.debugLogger = debugLogger
        this.debugSnapshotStore = debugSnapshotStore
    }

    override fun logIgnoredEvent(
        packageName: String?,
        eventType: Int,
        reason: String,
    ) {
        debugLogger.logIgnoredEvent(
            packageName = packageName,
            eventType = eventType,
            reason = reason,
        )
    }

    override fun recordDetection(
        packageName: String,
        eventType: Int,
        result: DetectionResult,
        snapshot: AccessibilityTreeSnapshot,
        nowMillis: Long,
    ) {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
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

        debugLogger.logDetection(
            packageName = packageName,
            eventType = eventType,
            result = result,
            snapshot = snapshot,
        )
    }

    override fun clearDetectorResult() {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            lastDetectorResult = null
            lastBlockingDecision = null
        }
    }

    override fun recordBlockingDecision(decision: String) {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            lastBlockingDecision = decision
        }
    }

    override fun lastBlockingDecisionText(): String? {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            return null
        }

        return lastBlockingDecision ?: "none"
    }

    override fun lastDetectorResultText(): String? {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            return null
        }

        val result = lastDetectorResult ?: return "none"
        return "package=${result.packageName} event=${result.eventType} " +
            "confidence=${result.confidence} reasons=${result.reasons} " +
            "signals=${result.matchedSignals} " +
            "snapshot=${result.snapshotSummary} at=${result.recordedAtMillis}"
    }

    override fun lastDebugSnapshotText(): String? {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            return null
        }

        val snapshot = lastDebugSnapshot ?: return "none"
        return "saved=${snapshot.path} summary=${snapshot.summary}"
    }

    override fun requestDebugOverlay() {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            debugOverlayRequested = true
        }
    }

    override fun consumeDebugOverlayRequest(): Boolean {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED || !debugOverlayRequested) {
            return false
        }

        debugOverlayRequested = false
        return true
    }

    override fun requestDebugSnapshot() {
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            debugSnapshotRequested = true
        }
    }

    override fun isDebugSnapshotPending(): Boolean = BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED && debugSnapshotRequested

    override fun captureDebugSnapshotIfRequested(
        packageName: String,
        eventType: String,
        snapshot: AccessibilityTreeSnapshot,
        nowMillis: Long,
    ) {
        if (!consumeDebugSnapshotRequest()) {
            return
        }

        debugSnapshotStore
            .save(
                packageName = packageName,
                eventType = eventType,
                snapshot = snapshot,
                nowMillis = nowMillis,
            )?.let(::recordDebugSnapshot)
    }

    private fun consumeDebugSnapshotRequest(): Boolean {
        if (!BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED || !debugSnapshotRequested) {
            return false
        }

        debugSnapshotRequested = false
        return true
    }

    private fun recordDebugSnapshot(snapshot: DetectorDebugSnapshot) {
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
