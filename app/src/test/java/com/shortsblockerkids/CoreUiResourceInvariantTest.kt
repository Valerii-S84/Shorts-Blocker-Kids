package com.shortsblockerkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CoreUiResourceInvariantTest {
    @Test
    fun coreUiResourceCatalogContainsTheLocalizedPresentationBoundary() {
        val missingStrings = REQUIRED_STRING_RESOURCES - resourceNames("string")

        assertTrue("Missing string resources: " + missingStrings.sorted(), missingStrings.isEmpty())
        assertTrue(
            "Missing dashboard_days_remaining plural",
            resourceNames("plurals").contains("dashboard_days_remaining"),
        )
    }

    @Test
    fun defaultEnglishCopyAndFormattingContractsRemainStable() {
        assertEquals("Select Protected Apps", stringValue("protected_apps_title"))
        assertEquals("Short Video Protection", stringValue("dashboard_title"))
        assertEquals("YouTube Shorts", stringValue("platform_youtube_shorts"))
        assertEquals("Detector Playground", stringValue("debug_detector_playground_title"))
        assertEquals("Free test period ended. %1\$s", stringValue("error_free_test_ended_with_detail"))
        assertEquals("%1\$s: %2\$s", stringValue("platform_status_item_format"))
        assertEquals("%1\$s (%2\$s)", stringValue("platform_unsupported_item_format"))

        DEBUG_FORMAT_RESOURCES.forEach { resourceName ->
            assertTrue(
                resourceName + " must preserve its positional placeholder",
                stringValue(resourceName).contains("%1\$s"),
            )
        }
    }

    @Test
    fun dashboardRemainingDaysUsesPluralResources() {
        val items = pluralItems("dashboard_days_remaining")

        assertEquals(setOf("one", "other"), items.keys)
        assertTrue(items.getValue("one").contains("%1\$d"))
        assertTrue(items.getValue("other").contains("%1\$d"))
    }

    @Test
    fun composeSurfacesUseResourcesInsteadOfMovedEnglishLiterals() {
        val dashboard = repoFile(DASHBOARD_SOURCE).readText()
        val protectedApps = repoFile(PROTECTED_APPS_SOURCE).readText()
        val debug = repoFile(DEBUG_SOURCE).readText()

        assertTrue(dashboard.contains("pluralStringResource("))
        assertTrue(dashboard.contains("R.plurals.dashboard_days_remaining"))
        assertTrue(dashboard.contains("stringResource(entry.platformNameRes)"))
        assertTrue(protectedApps.contains("stringResource(entry.platformNameRes)"))
        assertTrue(debug.contains("stringResource(scenario.nameRes)"))

        DASHBOARD_MOVED_LITERALS.forEach { literal ->
            assertFalse("Dashboard still contains moved literal: " + literal, dashboard.contains(literal))
        }
        PROTECTED_APPS_MOVED_LITERALS.forEach { literal ->
            assertFalse(
                "Protected Apps still contains moved literal: " + literal,
                protectedApps.contains(literal),
            )
        }
        DEBUG_MOVED_LITERALS.forEach { literal ->
            assertFalse("Debug UI still contains moved literal: " + literal, debug.contains(literal))
        }
    }

    @Test
    fun platformModelKeepsTechnicalIdentitySeparateFromPresentation() {
        val platformSource = repoFile(PLATFORM_SOURCE).readText()
        val dashboardSource = repoFile(DASHBOARD_SOURCE).readText()

        assertFalse(platformSource.contains("val displayName: String"))
        assertFalse(platformSource.contains("val platformName: String"))
        assertFalse(platformSource.contains("\"YouTube Shorts\""))
        assertTrue(platformSource.contains("R.string.platform_youtube_shorts"))
        assertTrue(dashboardSource.contains("R.string.platform_status_supported"))
        assertTrue(platformSource.contains("const val TIKTOK_REGIONAL_PACKAGE"))
        assertTrue(platformSource.contains("const val FACEBOOK_LITE_PACKAGE"))
    }

    private fun resourceNames(tagName: String): Set<String> {
        val nodes = resources.getElementsByTagName(tagName)
        return buildSet {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as Element
                add(element.getAttribute("name"))
            }
        }
    }

    private fun stringValue(name: String): String = resourceElement("string", name).textContent.trim()

    private fun pluralItems(name: String): Map<String, String> {
        val plural = resourceElement("plurals", name)
        return buildMap {
            val children = plural.childNodes
            for (index in 0 until children.length) {
                val child = children.item(index)
                if (child is Element && child.tagName == "item") {
                    put(child.getAttribute("quantity"), child.textContent.trim())
                }
            }
        }
    }

    private fun resourceElement(
        tagName: String,
        name: String,
    ): Element {
        val nodes = resources.getElementsByTagName(tagName)
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            if (element.getAttribute("name") == name) {
                return element
            }
        }
        error("Missing " + tagName + " resource: " + name)
    }

    private val resources by lazy {
        DocumentBuilderFactory
            .newInstance()
            .apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }.newDocumentBuilder()
            .parse(repoFile(STRINGS_XML))
    }

    private fun repoFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var root: File? = File(userDir)
        while (root != null) {
            val candidate = File(root, relativePath)
            if (candidate.isFile) {
                return candidate
            }
            root = root.parentFile
        }
        error("Missing repository file: " + relativePath)
    }

    private companion object {
        const val STRINGS_XML = "app/src/main/res/values/strings.xml"
        const val DASHBOARD_SOURCE =
            "app/src/main/java/com/shortsblockerkids/feature/dashboard/DashboardScreen.kt"
        const val PROTECTED_APPS_SOURCE =
            "app/src/main/java/com/shortsblockerkids/feature/onboarding/ProtectedAppsScreen.kt"
        const val DEBUG_SOURCE =
            "app/src/debug/java/com/shortsblockerkids/feature/debug/DetectorPlaygroundScreen.kt"
        const val PLATFORM_SOURCE =
            "app/src/main/java/com/shortsblockerkids/accessibility/SupportedPlatform.kt"

        val DASHBOARD_MOVED_LITERALS =
            setOf(
                "\"Short Video Protection\"",
                "\"Setup checklist\"",
                "\"Protected apps selected\"",
                "\"Days remaining\"",
                "\"Protection inactive.",
                "\"Google Play subscription\"",
                "\"Manage subscription\"",
                "\"Restore purchases\"",
            )

        val PROTECTED_APPS_MOVED_LITERALS =
            setOf(
                "\"Select Protected Apps\"",
                "\"Choose which supported short-video surfaces",
                "Text(\"Continue\")",
            )

        val DEBUG_MOVED_LITERALS =
            setOf(
                "\"Detector Playground\"",
                "\"No scenario\"",
                "\"not requested\"",
                "\"Capture YouTube UI Snapshot\"",
                "\"Request Test Overlay\"",
                "Text(\"Back\")",
                "name = \"Simulate",
                "overlayMessage = \"snapshot requested\"",
                "overlayMessage = \"overlay requested\"",
            )

        val DEBUG_FORMAT_RESOURCES =
            setOf(
                "debug_scenario_format",
                "debug_confidence_format",
                "debug_reasons_format",
                "debug_signals_format",
                "debug_would_block_format",
                "debug_test_overlay_format",
                "debug_snapshot_format",
            )

        val REQUIRED_STRING_RESOURCES =
            setOf(
                "protected_apps_title",
                "protected_apps_description",
                "protected_apps_continue",
                "dashboard_title",
                "dashboard_setup_checklist",
                "dashboard_checklist_parent_pin",
                "dashboard_checklist_protected_apps",
                "dashboard_checklist_accessibility_disclosure",
                "dashboard_checklist_accessibility_service",
                "dashboard_checklist_tamper_protection",
                "dashboard_platform_support",
                "dashboard_enabled_apps",
                "dashboard_not_supported",
                "dashboard_pin",
                "dashboard_free_test",
                "dashboard_days_remaining_label",
                "dashboard_subscription",
                "dashboard_protection_permission",
                "dashboard_tamper_protection",
                "dashboard_protection_status",
                "dashboard_description",
                "dashboard_open_accessibility_settings",
                "dashboard_review_accessibility_disclosure",
                "dashboard_privacy_policy",
                "dashboard_review_tamper_protection",
                "dashboard_enable_tamper_protection",
                "dashboard_local_qa",
                "dashboard_google_play_subscription",
                "dashboard_manage_subscription",
                "dashboard_subscribe_google_play",
                "dashboard_restore_purchases",
                "status_active",
                "status_missing",
                "status_optional",
                "status_enabled",
                "status_disabled",
                "status_created",
                "status_not_created",
                "status_none",
                "status_not_started",
                "status_free_test_active",
                "status_free_test_expired",
                "status_subscription_active",
                "status_protection_permission_missing",
                "status_protection_active",
                "status_protection_locked",
                "status_inactive",
                "status_optional_inactive",
                "status_locked",
                "status_on",
                "status_off",
                "error_free_test_ended_with_detail",
                "error_protection_permission_missing",
                "error_no_protected_apps_selected",
                "error_free_test_expired",
                "error_protection_inactive_disabled",
                "error_protection_inactive_no_apps",
                "error_protection_inactive_accessibility",
                "error_protection_inactive_temporary_allow",
                "error_free_test_not_started",
                "error_protection_inactive_setup",
                "platform_youtube_shorts",
                "platform_tiktok_short_video_feed",
                "platform_tiktok_regional",
                "platform_instagram_reels",
                "platform_facebook_reels",
                "platform_facebook_lite",
                "platform_status_supported",
                "platform_status_supported_needs_qa",
                "platform_status_not_supported",
                "platform_status_item_format",
                "platform_unsupported_item_format",
                "debug_detector_playground_title",
                "debug_no_scenario",
                "debug_not_requested",
                "debug_detected_package",
                "debug_disabled",
                "debug_protection_mode",
                "debug_blocking_decision",
                "debug_entitlement",
                "debug_subscription_state",
                "debug_backend",
                "debug_enabled",
                "debug_billing_ui",
                "debug_protection_permission",
                "debug_missing",
                "debug_scenario_format",
                "debug_confidence_format",
                "debug_reasons_format",
                "debug_signals_format",
                "debug_would_block_format",
                "debug_test_overlay_format",
                "debug_snapshot_format",
                "debug_snapshot_requested",
                "debug_capture_youtube_snapshot",
                "debug_overlay_requested",
                "debug_request_test_overlay",
                "debug_back",
                "debug_scenario_youtube_home",
                "debug_scenario_youtube_video",
                "debug_scenario_search",
                "debug_scenario_shorts_high_confidence",
                "debug_scenario_non_youtube",
            )
    }
}
