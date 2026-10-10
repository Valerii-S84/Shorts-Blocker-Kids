package com.shortsblockerkids.platform.accessibility.overlay

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.shortsblockerkids.domain.detection.DetectionResult
import com.shortsblockerkids.platform.accessibility.detection.ProductionDetectorRegistry
import com.shortsblockerkids.platform.accessibility.scanning.AccessibilityTreeScanner

class BlockedPlaybackExit(
    private val service: AccessibilityService,
) {
    private val scanner = AccessibilityTreeScanner()
    private val detector = ProductionDetectorRegistry.create()
    private var blockedPackage: String? = null

    @Suppress("DEPRECATION")
    fun captureBlockedPackage(expectedPackage: String): Boolean {
        val root = service.rootInActiveWindow
        val matches = root?.packageName?.toString() == expectedPackage
        root?.recycle()
        if (matches) blockedPackage = expectedPackage
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
        blockedPackage = expectedPackage
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
        val shouldExit =
            try {
                canExitAfterOverlayFailure(
                    packageName,
                    root.packageName?.toString(),
                    detector.detect(packageName, scanner.scan(root)),
                )
            } finally {
                root.recycle()
            }
        return if (shouldExit) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        } else {
            isPlaybackClosed()
        }
    }

    fun isPlaybackClosed(): Boolean {
        // Accessibility reports UI/action acceptance, not third-party audio/video state.
        // No playback acknowledgement API is available here; vanished markers, windows,
        // accepted BACK or PiP dismiss must therefore never release the barrier.
        return false
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
