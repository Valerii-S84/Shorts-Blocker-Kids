package com.shortsblockerkids.presentation.dashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class DashboardPresentationBoundaryTest {
    @Test
    fun slice13PresentationSurfacesAvoidRawAndOuterLayerDependencies() {
        boundarySources().forEach { source ->
            val content = source.readText()
            FORBIDDEN_TOKENS.forEach { token ->
                assertFalse(
                    "${source.relativeTo(repositoryRoot)} contains $token",
                    content.contains(token),
                )
            }
        }
    }

    @Test
    fun screensAndDebugVariantsConsumePresentationModels() {
        val dashboard = source(DASHBOARD_SCREEN)
        val protectedApps = source(PROTECTED_APPS_SCREEN)
        val debug = source(DEBUG_SCREEN)
        val release = source(RELEASE_DEBUG_SCREEN)

        assertTrue(dashboard.contains("uiState: DashboardUiState"))
        assertTrue(protectedApps.contains("items: List<ProtectedPlatformItemUiModel>"))
        assertTrue(debug.contains("uiState: DashboardUiState"))
        assertTrue(release.contains("uiState: DashboardUiState"))
    }

    @Test
    fun dashboardPresentationReferencesExistingStringResources() {
        val referencedResources =
            RESOURCE_SOURCES
                .flatMap { path ->
                    RESOURCE_REFERENCE
                        .findAll(source(path))
                        .map { match -> match.groupValues[1] }
                        .toList()
                }.toSet()
        val missingResources = referencedResources - resourceNames()

        assertTrue("Dashboard presentation must reference resources", referencedResources.isNotEmpty())
        assertTrue("Missing dashboard presentation resources: $missingResources", missingResources.isEmpty())
    }

    private fun boundarySources(): List<File> =
        BOUNDARY_PATHS.flatMap { relativePath ->
            val path = File(repositoryRoot, relativePath)
            if (path.isDirectory) {
                path.walkTopDown().filter { file -> file.isFile && file.extension == "kt" }.toList()
            } else {
                listOf(path)
            }
        }

    private fun resourceNames(): Set<String> {
        val resources =
            DocumentBuilderFactory
                .newInstance()
                .apply {
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                }.newDocumentBuilder()
                .parse(File(repositoryRoot, STRINGS_XML))
        val nodes = resources.getElementsByTagName("string")
        return buildSet {
            for (index in 0 until nodes.length) {
                add((nodes.item(index) as Element).getAttribute("name"))
            }
        }
    }

    private fun source(relativePath: String): String = File(repositoryRoot, relativePath).readText()

    private val repositoryRoot: File by lazy {
        generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { directory ->
            directory.parentFile
        }.first { directory -> File(directory, "settings.gradle.kts").isFile }
    }

    private companion object {
        const val SOURCE_ROOT = "app/src/main/java/com/shortsblockerkids"
        const val DASHBOARD_SCREEN = "$SOURCE_ROOT/feature/dashboard/DashboardScreen.kt"
        const val PROTECTED_APPS_SCREEN =
            "$SOURCE_ROOT/feature/onboarding/ProtectedAppsScreen.kt"
        const val DEBUG_SCREEN =
            "app/src/debug/java/com/shortsblockerkids/feature/debug/DetectorPlaygroundScreen.kt"
        const val RELEASE_DEBUG_SCREEN =
            "app/src/release/java/com/shortsblockerkids/feature/debug/DetectorPlaygroundScreen.kt"
        const val STRINGS_XML = "app/src/main/res/values/strings.xml"

        val BOUNDARY_PATHS =
            listOf(
                "$SOURCE_ROOT/presentation/dashboard",
                "$SOURCE_ROOT/presentation/app/ShortsBlockerKidsApp.kt",
                DASHBOARD_SCREEN,
                PROTECTED_APPS_SCREEN,
                DEBUG_SCREEN,
                RELEASE_DEBUG_SCREEN,
            )
        val RESOURCE_SOURCES =
            listOf(
                "$SOURCE_ROOT/presentation/dashboard/DashboardStateFactory.kt",
                DASHBOARD_SCREEN,
                PROTECTED_APPS_SCREEN,
            )
        val FORBIDDEN_TOKENS =
            listOf(
                "AppSettingsSnapshot",
                "PlatformSupportMatrix",
                "DataStoreSettingsStore",
                "SettingsRepository",
                "System.currentTimeMillis(",
                "pinHash",
                "pinSalt",
                "billingInstallationId",
                "AccessibilityEvent",
                "AccessibilityNodeInfo",
            )
        val RESOURCE_REFERENCE = Regex("R\\.string\\.([a-z0-9_]+)")
    }
}
