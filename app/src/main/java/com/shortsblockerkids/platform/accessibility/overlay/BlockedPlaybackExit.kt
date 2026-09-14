package com.shortsblockerkids.platform.accessibility.overlay

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.shortsblockerkids.platform.accessibility.detection.ProductionDetectorRegistry
import com.shortsblockerkids.platform.accessibility.scanning.AccessibilityTreeScanner

class BlockedPlaybackExit(
    private val service: AccessibilityService,
) {
    private val scanner = AccessibilityTreeScanner()
    private val detector = ProductionDetectorRegistry.create()
    private var blockedPackage: String? = null

    @Suppress("DEPRECATION")
    fun captureBlockedPackage() {
        val root = service.rootInActiveWindow
        blockedPackage = root?.packageName?.toString()
        root?.recycle()
    }

    @Suppress("DEPRECATION")
    fun exitPlayer(): Boolean {
        val packageName = blockedPackage ?: return false
        val windows = currentWindows()
        for (window in windows) {
            if (!window.isInPictureInPictureMode) continue
            val root = window.root ?: return false
            try {
                if (root.packageName?.toString() == packageName) {
                    return root.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
                }
            } finally {
                root.recycle()
            }
        }
        val root = service.rootInActiveWindow ?: return false
        val isBlockedApp = root.packageName?.toString() == packageName
        root.recycle()
        return if (isBlockedApp) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        } else {
            isPlaybackClosed()
        }
    }

    @Suppress("DEPRECATION")
    fun isPlaybackClosed(): Boolean {
        val packageName = blockedPackage ?: return false
        val windows = currentWindows().filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        if (windows.isEmpty()) return false
        for (window in windows) {
            val root = window.root ?: return false
            try {
                if (root.packageName?.toString() != packageName) continue
                if (window.isInPictureInPictureMode) return false
                val snapshot = scanner.scan(root)
                if (snapshot.visibleNodeCount == 0) return false
                if (detector.detect(packageName, snapshot).shouldBlock) return false
                if (snapshot.nodes.any { node ->
                        val id = node.viewIdResourceName?.substringAfterLast('/').orEmpty()
                        node.isVisibleToUser &&
                            (
                                id.contains("mini_player") ||
                                    id.contains("miniplayer") ||
                                    id in PLAYBACK_IDS
                            )
                    }
                ) {
                    return false
                }
            } finally {
                root.recycle()
            }
        }
        return true
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
        val PLAYBACK_IDS =
            setOf(
                "reel_watch_sequence",
                "reel_player_page",
                "reel_player_view",
                "shorts_player",
                "shorts_watch",
                "shorts_video",
            )
    }
}
