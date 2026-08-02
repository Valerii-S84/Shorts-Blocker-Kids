package com.shortsblockerkids.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlatformPackageConvergenceInvariantTest {
    @Test
    fun accessibilityImplementationsUsePlatformPaths() {
        val repositoryRoot = repositoryRoot()

        PLATFORM_SOURCES.forEach { (relativePath, packageName) ->
            val sourceFile = File(repositoryRoot, relativePath)
            assertTrue("missing platform source: $relativePath", sourceFile.isFile)
            assertTrue(
                "wrong package for $relativePath",
                sourceFile.useLines { lines -> lines.firstOrNull() } == "package $packageName",
            )
        }
        OLD_IMPLEMENTATION_PATHS.forEach { relativePath ->
            assertFalse(
                "old implementation path still exists: $relativePath",
                File(repositoryRoot, relativePath).exists(),
            )
        }
    }

    @Test
    fun androidComponentShellsKeepStablePathsAndPackages() {
        val repositoryRoot = repositoryRoot()

        STABLE_COMPONENT_SHELLS.forEach { (relativePath, packageName) ->
            val sourceFile = File(repositoryRoot, relativePath)
            assertTrue("missing Android component shell: $relativePath", sourceFile.isFile)
            assertTrue(
                "wrong package for $relativePath",
                sourceFile.useLines { lines -> lines.firstOrNull() } == "package $packageName",
            )
        }
    }

    private fun repositoryRoot(): File {
        var candidate: File? = File(requireNotNull(System.getProperty("user.dir")))
        while (candidate != null) {
            if (
                File(candidate, "settings.gradle.kts").isFile &&
                File(candidate, "app/src/main").isDirectory
            ) {
                return candidate
            }
            candidate = candidate.parentFile
        }
        error("Repository root not found")
    }

    private companion object {
        const val PLATFORM_ACCESSIBILITY_SOURCE_ROOT =
            "app/src/main/java/com/shortsblockerkids/platform/accessibility"

        val PLATFORM_SOURCES =
            mapOf(
                "app/src/main/java/com/shortsblockerkids/platform/accessibility/AccessibilityServiceRuntime.kt" to
                    "com.shortsblockerkids.platform.accessibility",
                "app/src/main/java/com/shortsblockerkids/platform/accessibility/routing/AccessibilityEventRouter.kt" to
                    "com.shortsblockerkids.platform.accessibility.routing",
                "app/src/main/java/com/shortsblockerkids/platform/accessibility/routing/AccessibilityEventPolicy.kt" to
                    "com.shortsblockerkids.platform.accessibility.routing",
                "app/src/main/java/com/shortsblockerkids/platform/accessibility/scanning/AccessibilityTreeScanner.kt" to
                    "com.shortsblockerkids.platform.accessibility.scanning",
                "app/src/main/java/com/shortsblockerkids/platform/accessibility/overlay/BlockOverlayController.kt" to
                    "com.shortsblockerkids.platform.accessibility.overlay",
                "app/src/main/java/com/shortsblockerkids/platform/accessibility/overlay/PhoneHomeExitController.kt" to
                    "com.shortsblockerkids.platform.accessibility.overlay",
                "app/src/main/java/com/shortsblockerkids/platform/accessibility/overlay/TemporaryAllowNavigator.kt" to
                    "com.shortsblockerkids.platform.accessibility.overlay",
                "app/src/main/java/com/shortsblockerkids/platform/accessibility/status/AccessibilityServiceStatus.kt" to
                    "com.shortsblockerkids.platform.accessibility.status",
                "$PLATFORM_ACCESSIBILITY_SOURCE_ROOT/diagnostics/RuntimeProtectionState.kt" to
                    "com.shortsblockerkids.platform.accessibility.diagnostics",
                "$PLATFORM_ACCESSIBILITY_SOURCE_ROOT/diagnostics/DebugAccessibilityLogger.kt" to
                    "com.shortsblockerkids.platform.accessibility.diagnostics",
                "$PLATFORM_ACCESSIBILITY_SOURCE_ROOT/diagnostics/DetectorDebugSnapshotStore.kt" to
                    "com.shortsblockerkids.platform.accessibility.diagnostics",
                "app/src/main/java/com/shortsblockerkids/platform/tamper/TamperProtectionStatus.kt" to
                    "com.shortsblockerkids.platform.tamper",
            )

        val OLD_IMPLEMENTATION_PATHS =
            listOf(
                "app/src/main/java/com/shortsblockerkids/accessibility/AccessibilityEventRouter.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/AccessibilityEventPolicy.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/AccessibilityTreeScanner.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/BlockOverlayController.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/PhoneHomeExitController.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/TemporaryAllowNavigator.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/AccessibilityServiceStatus.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/RuntimeProtectionState.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/DebugAccessibilityLogger.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/DetectorDebugSnapshotStore.kt",
                "app/src/main/java/com/shortsblockerkids/core/tamper/TamperProtectionStatus.kt",
            )

        val STABLE_COMPONENT_SHELLS =
            mapOf(
                "app/src/main/java/com/shortsblockerkids/accessibility/ShortsBlockerAccessibilityService.kt" to
                    "com.shortsblockerkids.accessibility",
                "app/src/main/java/com/shortsblockerkids/core/tamper/TamperProtectionReceiver.kt" to
                    "com.shortsblockerkids.core.tamper",
            )
    }
}
