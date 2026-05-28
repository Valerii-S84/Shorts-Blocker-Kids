package com.shortsblockerkids.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.eventTypeToString
import android.view.accessibility.AccessibilityNodeInfo
import com.shortsblockerkids.core.storage.AppSettings

class AccessibilityEventRouter(
    private val settingsProvider: () -> AppSettings,
    private val eventPolicy: AccessibilityEventPolicy,
    private val treeScanner: AccessibilityTreeScanner,
    private val detectionEngine: ShortVideoDetectionEngine,
    private val blockingDecisionController: BlockingDecisionController,
    private val blockOverlayController: BlockOverlayController,
    private val debugLogger: DebugAccessibilityLogger,
    private val debugSnapshotStore: DetectorDebugSnapshotStore,
) {
    fun route(
        event: AccessibilityEvent,
        rootProvider: () -> AccessibilityNodeInfo?,
    ) {
        val packageName = event.packageName?.toString()
        val nowMillis = System.currentTimeMillis()
        val platform = detectionEngine.platformForPackage(packageName)
        if (platform == null) {
            if (
                blockOverlayController.isOverlayVisible ||
                blockingDecisionController.shouldIgnoreUnsupportedPackageEvent(nowMillis)
            ) {
                debugLogger.logIgnoredEvent(
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
        val settings = settingsProvider()
        if (!settings.isPlatformEnabled(platform.id)) {
            debugLogger.logIgnoredEvent(
                packageName = packageName,
                eventType = event.eventType,
                reason = "protected app disabled",
            )
            dismissBlockingState()
            return
        }

        if (blockOverlayController.isOverlayVisible) {
            debugLogger.logIgnoredEvent(
                packageName = packageName,
                eventType = event.eventType,
                reason = "blocking overlay visible",
            )
            return
        }

        if (RuntimeProtectionState.consumeDebugOverlayRequest()) {
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
                    nowMillis = System.currentTimeMillis(),
                )
            handleDecision(decision)
            return
        }

        val isProtectionActive = settings.canProtect(nowMillis)
        val isDebugSnapshotPending = RuntimeProtectionState.isDebugSnapshotPending()
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
            RuntimeProtectionState.clearDetectorResult()
            return
        }

        if (!eventPolicy.shouldScan(event.eventType, nowMillis)) {
            return
        }

        val root = rootProvider() ?: return
        val snapshot = treeScanner.scan(root)
        if (RuntimeProtectionState.consumeDebugSnapshotRequest()) {
            debugSnapshotStore
                .save(
                    packageName = supportedPackageName,
                    eventType = eventTypeToString(event.eventType),
                    snapshot = snapshot,
                    nowMillis = nowMillis,
                )?.let(RuntimeProtectionState::recordDebugSnapshot)
        }
        val result = detectionEngine.detect(supportedPackageName, snapshot)
        RuntimeProtectionState.recordDetectorResult(
            packageName = supportedPackageName,
            eventType = event.eventType,
            result = result,
            snapshot = snapshot,
            nowMillis = nowMillis,
        )
        debugLogger.logDetection(
            packageName = packageName,
            eventType = event.eventType,
            result = result,
            snapshot = snapshot,
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
        RuntimeProtectionState.clearDetectorResult()
    }

    private fun handleDecision(decision: BlockingDecision) {
        RuntimeProtectionState.recordBlockingDecision(decision)
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
