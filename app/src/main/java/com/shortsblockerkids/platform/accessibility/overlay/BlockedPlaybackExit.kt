package com.shortsblockerkids.platform.accessibility.overlay

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.shortsblockerkids.BuildConfig
import com.shortsblockerkids.domain.detection.DetectionResult
import com.shortsblockerkids.platform.accessibility.detection.ProductionDetectorRegistry
import com.shortsblockerkids.platform.accessibility.diagnostics.DebugAccessibilityLogger
import com.shortsblockerkids.platform.accessibility.scanning.AccessibilityTreeScanner

class BlockedPlaybackExit(
    private val service: AccessibilityService,
) {
    private val scanner = AccessibilityTreeScanner()
    private val detector = ProductionDetectorRegistry.create()
    private val debugLogger = DebugAccessibilityLogger()
    private var blockedPackage: String? = null
    private val departureObserver = BlockedSurfaceDepartureObserver()

    val blockedPackageName: String?
        get() = blockedPackage

    @Suppress("DEPRECATION")
    fun captureBlockedPackage(expectedPackage: String): Boolean {
        val root = service.rootInActiveWindow
        val matches = root?.packageName?.toString() == expectedPackage
        root?.recycle()
        if (matches) {
            blockedPackage = expectedPackage
            departureObserver.capture(expectedPackage)
        }
        return matches
    }

    @Suppress("DEPRECATION")
    fun exitIfStillDetected(expectedPackage: String): Boolean {
        if (dismissBlockedPip(expectedPackage)) return true
        val root = service.rootInActiveWindow ?: return false
        val shouldExit =
            try {
                val currentPackage = root.packageName?.toString()
                currentPackage == expectedPackage &&
                    canExitAfterOverlayFailure(
                        expectedPackage,
                        currentPackage,
                        detector.detect(expectedPackage, scanner.scan(root)),
                    )
            } finally {
                root.recycle()
            }
        if (!shouldExit) return false
        if (blockedPackage != expectedPackage) {
            blockedPackage = expectedPackage
            departureObserver.capture(expectedPackage)
        }
        return exitPlayer()
    }

    @Suppress("DEPRECATION")
    fun isBlockedPackageStillDetected(expectedPackage: String): Boolean {
        for (window in currentWindows()) {
            if (!window.isInPictureInPictureMode) continue
            val root = window.root ?: continue
            try {
                if (canDismissBlockedPictureInPicture(expectedPackage, blockedPackage, root.packageName?.toString(), true)) return true
            } finally {
                root.recycle()
            }
        }
        val root = service.rootInActiveWindow ?: return false
        return try {
            canExitAfterOverlayFailure(
                expectedPackage,
                root.packageName?.toString(),
                detector.detect(expectedPackage, scanner.scan(root)),
            )
        } finally {
            root.recycle()
        }
    }

    @Suppress("DEPRECATION")
    fun exitPlayer(): Boolean {
        val packageName = blockedPackage ?: return false
        if (dismissBlockedPip(packageName)) return true
        val root = service.rootInActiveWindow ?: return false
        val isBlockedPackageForeground =
            try {
                root.packageName?.toString() == packageName
            } finally {
                root.recycle()
            }
        return if (isBlockedPackageForeground) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        } else {
            isPlaybackClosed()
        }
    }

    fun isPlaybackClosed(): Boolean {
        // This confirms departure from protected UI, never third-party media playback state.
        val windows =
            currentWindows()
                .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                .map(::observeApplicationWindow)
                .sortedBy { it.windowId }
        val closed = departureObserver.observe(windows)
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            val windowSummary =
                windows.joinToString(separator = ";") { window ->
                    "id=${window.windowId},package=${window.packageName},active=${window.isActive}," +
                        "pip=${window.isPictureInPicture},shouldBlock=${window.shouldBlock}"
                }
            debugLogger.logIgnoredEvent(
                packageName = blockedPackage,
                eventType = AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                reason = "exit-observation closed=$closed windows=[$windowSummary]",
            )
        }
        return closed
    }

    fun shouldDismissForProtectionState(isTargetEligible: Boolean): Boolean =
        departureObserver.observe(emptyList(), isTargetEligible = isTargetEligible)

    @Suppress("DEPRECATION")
    private fun observeApplicationWindow(window: AccessibilityWindowInfo): ObservedPlaybackWindow {
        val root = window.root
        val active = window.isActive || window.isFocused
        if (root == null) {
            return ObservedPlaybackWindow(window.id, null, active, window.isInPictureInPictureMode, null)
        }
        return try {
            val packageName = root.packageName?.toString()
            val snapshot = scanner.scan(root)
            val hasVisibleContent = snapshot.nodes.any { it.isVisibleToUser && it.width > 0 && it.height > 0 }
            val shouldBlock =
                when {
                    !root.refresh() || !hasVisibleContent -> null
                    detector.platformForPackage(packageName) == null -> false
                    completeNodeCount(root) != snapshot.nodes.size -> null
                    else -> detector.detect(requireNotNull(packageName), snapshot).shouldBlock
                }
            ObservedPlaybackWindow(
                windowId = window.id,
                packageName = packageName,
                isActive = active,
                isPictureInPicture = window.isInPictureInPictureMode,
                shouldBlock = shouldBlock,
            )
        } finally {
            root.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun completeNodeCount(
        node: AccessibilityNodeInfo,
        depth: Int = 0,
        count: IntArray = intArrayOf(0),
    ): Int? {
        // Match the production scanner bounds; reaching a bound cannot prove absence.
        if (depth > MAX_OBSERVATION_DEPTH || count[0] >= MAX_OBSERVATION_NODES) return null
        count[0]++
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: return null
            val complete =
                try {
                    completeNodeCount(child, depth + 1, count) != null
                } finally {
                    child.recycle()
                }
            if (!complete) return null
        }
        return count[0].takeIf { it < MAX_OBSERVATION_NODES }
    }

    @Suppress("DEPRECATION")
    private fun dismissBlockedPip(expectedPackage: String): Boolean {
        if (blockedPackage != expectedPackage) return false
        for (window in currentWindows()) {
            if (!window.isInPictureInPictureMode) continue
            val root = window.root ?: continue
            try {
                if (canDismissBlockedPictureInPicture(expectedPackage, blockedPackage, root.packageName?.toString(), true)) {
                    return root.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
                }
            } finally {
                root.recycle()
            }
        }
        return false
    }

    private fun currentWindows(): List<AccessibilityWindowInfo> {
        // A window transition can leave the closed player in the accessibility cache.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            service.clearCache()
        } else {
            // Reapplying the existing service info also clears the cache on older Android.
            service.serviceInfo = service.serviceInfo
        }
        return service.windows
    }

    private companion object {
        const val MAX_OBSERVATION_DEPTH = 12
        const val MAX_OBSERVATION_NODES = 180
    }
}

internal data class ObservedPlaybackWindow(
    val windowId: Int,
    val packageName: String?,
    val isActive: Boolean,
    val isPictureInPicture: Boolean = false,
    val shouldBlock: Boolean? = false,
    val treeSignature: Int = 0,
)

internal class BlockedSurfaceDepartureObserver {
    private var blockedPackage: String? = null
    private var previousWindows: List<ObservedPlaybackWindow>? = null

    fun capture(packageName: String) {
        blockedPackage = packageName
        previousWindows = null
    }

    fun observe(
        windows: List<ObservedPlaybackWindow>,
        isTargetEligible: Boolean = true,
    ): Boolean {
        if (blockedPackage == null) return false
        if (!isTargetEligible) {
            previousWindows = null
            return true
        }
        // Only protection and window identity must stabilize; live content may change.
        val materialWindows = windows.map { it.copy(treeSignature = 0) }.sortedBy { it.windowId }
        val departed = hasLeftBlockedSurface(blockedPackage, materialWindows)
        val confirmed = departed && materialWindows == previousWindows
        previousWindows = materialWindows.takeIf { departed }
        return confirmed
    }
}

internal fun hasLeftBlockedSurface(
    blockedPackage: String?,
    windows: List<ObservedPlaybackWindow>,
): Boolean {
    if (blockedPackage.isNullOrBlank() || windows.isEmpty()) return false
    if (windows.any { it.packageName.isNullOrBlank() || it.shouldBlock == null }) return false
    if (windows.any { it.shouldBlock == true || (it.packageName == blockedPackage && it.isPictureInPicture) }) return false
    return windows.any { it.isActive }
}

internal fun canExitAfterOverlayFailure(
    expectedPackage: String,
    currentPackage: String?,
    currentDetection: DetectionResult,
): Boolean = currentPackage == expectedPackage && currentDetection.shouldBlock

internal fun canDismissBlockedPictureInPicture(
    expectedPackage: String,
    capturedBlockedPackage: String?,
    currentPackage: String?,
    isPictureInPicture: Boolean,
): Boolean = isPictureInPicture && capturedBlockedPackage == expectedPackage && currentPackage == expectedPackage
