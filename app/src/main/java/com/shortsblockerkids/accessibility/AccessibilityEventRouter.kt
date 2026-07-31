package com.shortsblockerkids.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.eventTypeToString
import android.view.accessibility.AccessibilityNodeInfo
import com.shortsblockerkids.application.port.AccessibilityDiagnosticsPort
import com.shortsblockerkids.application.port.ProtectionSettingsPort
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.detection.Confidence
import com.shortsblockerkids.domain.detection.DetectionResult
import com.shortsblockerkids.domain.detection.ShortVideoDetectionEngine

class AccessibilityEventRouter(
    private val protectionSettingsPort: ProtectionSettingsPort,
    private val timeProvider: TimeProvider,
    private val eventPolicy: AccessibilityEventPolicy,
    private val treeScanner: AccessibilityTreeScanner,
    private val detectionEngine: ShortVideoDetectionEngine,
    private val blockingDecisionController: BlockingDecisionController,
    private val blockOverlayController: BlockOverlayController,
    private val diagnostics: AccessibilityDiagnosticsPort,
) {
    fun route(
        event: AccessibilityEvent,
        rootProvider: () -> AccessibilityNodeInfo?,
    ) {
        val packageName = event.packageName?.toString()
        val nowMillis = timeProvider.currentTimeMillis()
        val platform = detectionEngine.platformForPackage(packageName)
        if (platform == null) {
            if (
                blockOverlayController.isOverlayVisible ||
                blockingDecisionController.shouldIgnoreUnsupportedPackageEvent(nowMillis)
            ) {
                diagnostics.logIgnoredEvent(
                    packageName = packageName,
                    eventType = event.eventType,
                    reason = "blocking ui active",
                )
                return
            }
            dismissBlockingState()
            return
        }
        val supportedPackageName = packageName ?: return
        val protectionState = protectionSettingsPort.protectionState(nowMillis)
        if (!protectionState.isPlatformEnabled(platform.id)) {
            diagnostics.logIgnoredEvent(
                packageName = packageName,
                eventType = event.eventType,
                reason = "protected app disabled",
            )
            dismissBlockingState()
            return
        }

        if (blockOverlayController.isOverlayVisible) {
            diagnostics.logIgnoredEvent(
                packageName = packageName,
                eventType = event.eventType,
                reason = "blocking overlay visible",
            )
            return
        }

        if (diagnostics.consumeDebugOverlayRequest()) {
            val decision =
                blockingDecisionController.evaluate(
                    isInSupportedApp = true,
                    isProtectionActive = true,
                    result =
                        DetectionResult(
                            isShorts = true,
                            confidence = Confidence.HIGH,
                            reasons = listOf("debug overlay request"),
                            matchedSignals = listOf("debug_overlay_request"),
                        ),
                    nowMillis = nowMillis,
                )
            handleDecision(decision)
            return
        }

        val isProtectionActive = protectionState.isProtectionActive
        val isDebugSnapshotPending = diagnostics.isDebugSnapshotPending()
        if (!isProtectionActive && !isDebugSnapshotPending) {
            val decision =
                blockingDecisionController.evaluate(
                    isInSupportedApp = true,
                    isProtectionActive = false,
                    result = DetectionResult.None,
                    nowMillis = nowMillis,
                )
            handleDecision(decision)
            eventPolicy.reset()
            diagnostics.clearDetectorResult()
            return
        }

        if (!eventPolicy.shouldScan(event.eventType, nowMillis)) {
            return
        }

        val root = rootProvider() ?: return
        val snapshot = treeScanner.scan(root)
        diagnostics.captureDebugSnapshotIfRequested(
            packageName = supportedPackageName,
            eventType = eventTypeToString(event.eventType),
            snapshot = snapshot,
            nowMillis = nowMillis,
        )
        val result = detectionEngine.detect(supportedPackageName, snapshot)
        diagnostics.recordDetection(
            packageName = supportedPackageName,
            eventType = event.eventType,
            result = result,
            snapshot = snapshot,
            nowMillis = nowMillis,
        )

        val decision =
            blockingDecisionController.evaluate(
                isInSupportedApp = true,
                isProtectionActive = isProtectionActive,
                result = result,
                nowMillis = nowMillis,
            )
        handleDecision(decision)
    }

    fun shutdown() {
        dismissBlockingState()
    }

    fun dismissBlockingState() {
        blockOverlayController.dismissOverlay()
        blockingDecisionController.reset()
        eventPolicy.reset()
        diagnostics.clearDetectorResult()
    }

    private fun handleDecision(decision: BlockingDecision) {
        diagnostics.recordBlockingDecision(decision.name)
        when (decision) {
            BlockingDecision.ShowOverlay -> {
                if (!blockOverlayController.showBlockedOverlay()) {
                    if (!blockOverlayController.isOverlayVisible) {
                        blockingDecisionController.onOverlayDismissed()
                    }
                }
            }
            BlockingDecision.DismissOverlay -> blockOverlayController.dismissOverlay()
            BlockingDecision.Ignore -> Unit
        }
    }
}
