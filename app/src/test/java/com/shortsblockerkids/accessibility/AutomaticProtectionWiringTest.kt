package com.shortsblockerkids.accessibility

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards Android wiring that the JVM behavior tests cannot instantiate. */
class AutomaticProtectionWiringTest {
    @Test
    fun rootPackageGatePrecedesDebugRequestsAndTreeDetection() {
        val router = source("routing/AccessibilityEventRouter.kt")
        val gate = router.indexOf("if (!eventPolicy.isRootPackageConsistent(supportedPackageName, root.packageName?.toString()))")
        val debug = router.indexOf("diagnostics.consumeDebugOverlayRequest()")
        val scan = router.indexOf("treeScanner.scan(root)")
        assertTrue(gate >= 0 && gate < debug && debug < scan)
        assertTrue(router.substring(gate, debug).contains("return"))
        assertTrue(router.indexOf("protectionState.isPlatformEnabled(platform.id)") < gate)
    }

    @Test
    fun successfulOverlayWiresGuardedExitAndOnlyConfirmedClosureDismisses() {
        val router = source("routing/AccessibilityEventRouter.kt")
        assertTrue(router.contains("startPlaybackExit = blockOverlayController::exitBlockedPlayback"))
        val overlay = source("overlay/BlockOverlayController.kt")
        val automaticExit = overlay.substringAfter("fun exitBlockedPlayback(").substringBefore("fun exitAfterOverlayFailure(")
        assertTrue(automaticExit.contains("exitAfterOverlayFailure(packageName)"))
        val success = automaticExit.substringAfter("if (closed) {").substringBefore("} else {")
        val failure = automaticExit.substringAfter("} else {")
        assertTrue(success.contains("dismissOverlay()"))
        assertTrue(!failure.contains("dismissOverlay()"))
        assertTrue(failure.contains("pinButton?.isEnabled = true"))
        val boundedExit = overlay.substringAfter("fun exitAfterOverlayFailure(").substringBefore("fun dismissOverlay(")
        assertTrue(boundedExit.contains("PhoneHomeExitController("))
        assertTrue(boundedExit.contains("playbackExit.exitIfStillDetected(packageName)"))
        assertTrue(boundedExit.contains("playbackExit.isPlaybackClosed()"))
        assertTrue(overlay.substringAfter("fun dismissOverlay(").contains("fallbackExitController?.cancel()"))
    }

    private fun source(relativePath: String): String {
        var candidate: File? = File(requireNotNull(System.getProperty("user.dir")))
        while (candidate != null) {
            val file = File(candidate, "app/src/main/java/com/shortsblockerkids/platform/accessibility/$relativePath")
            if (file.isFile) return file.readText()
            candidate = candidate.parentFile
        }
        error("Repository source not found: $relativePath")
    }
}
