package com.shortsblockerkids.application.port

import com.shortsblockerkids.domain.detection.AccessibilityTreeSnapshot
import com.shortsblockerkids.domain.detection.DetectionResult

interface AccessibilityDiagnosticsPort {
    fun logIgnoredEvent(
        packageName: String?,
        eventType: Int,
        reason: String,
    )

    fun consumeDebugOverlayRequest(): Boolean

    fun isDebugSnapshotPending(): Boolean

    fun clearDetectorResult()

    fun captureDebugSnapshotIfRequested(
        packageName: String,
        eventType: String,
        snapshot: AccessibilityTreeSnapshot,
        nowMillis: Long,
    )

    fun recordDetection(
        packageName: String,
        eventType: Int,
        result: DetectionResult,
        snapshot: AccessibilityTreeSnapshot,
        nowMillis: Long,
    )

    fun recordBlockingDecision(decision: String)

    fun lastBlockingDecisionText(): String?

    fun lastDetectorResultText(): String?

    fun lastDebugSnapshotText(): String?

    fun requestDebugOverlay()

    fun requestDebugSnapshot()
}
