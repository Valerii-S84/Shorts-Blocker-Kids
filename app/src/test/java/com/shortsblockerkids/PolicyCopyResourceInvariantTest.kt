package com.shortsblockerkids

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PolicyCopyResourceInvariantTest {
    @Test
    fun policyCopyResourceCatalogContainsRequiredKeys() {
        val missingStrings = REQUIRED_STRING_RESOURCES - resourceNames("string")

        assertTrue("Missing policy string resources: " + missingStrings.sorted(), missingStrings.isEmpty())
    }

    @Test
    fun defaultEnglishPolicyCopyKeepsCriticalClaims() {
        val accessibility = normalizedText(stringValue("accessibility_disclosure_body"))
        val privacy = normalizedText(PRIVACY_BODY_RESOURCES.joinToString(" ") { stringValue(it) })
        val tamper = normalizedText(stringValue("tamper_protection_disclosure_body"))

        assertTrue(accessibility.wordCount() >= 145)
        assertTrue(privacy.wordCount() >= 120)
        assertTrue(tamper.wordCount() >= 70)

        ACCESSIBILITY_CLAIMS.forEach { claim ->
            assertTrue("Missing accessibility claim: " + claim, accessibility.contains(claim))
        }
        PRIVACY_CLAIMS.forEach { claim ->
            assertTrue("Missing privacy claim: " + claim, privacy.contains(claim))
        }
        TAMPER_CLAIMS.forEach { claim ->
            assertTrue("Missing tamper claim: " + claim, tamper.contains(claim))
        }
    }

    @Test
    fun policyScreensUseStringResourcesInsteadOfMovedEnglishLiterals() {
        val accessibility = repoFile(ACCESSIBILITY_SOURCE).readText()
        val privacy = repoFile(PRIVACY_SOURCE).readText()
        val tamper = repoFile(TAMPER_SOURCE).readText()

        assertTrue(accessibility.contains("stringResource(R.string.accessibility_disclosure_title)"))
        assertTrue(accessibility.contains("stringResource(R.string.accessibility_disclosure_body)"))
        assertTrue(accessibility.contains("stringResource(R.string.accessibility_disclosure_accept)"))
        assertTrue(privacy.contains("stringResource(titleRes)"))
        assertTrue(privacy.contains("stringResource(bodyRes)"))
        assertTrue(tamper.contains("stringResource(R.string.tamper_protection_disclosure_body)"))
        assertTrue(tamper.contains("stringResource(R.string.tamper_protection_open_device_admin)"))

        val sources = listOf(accessibility, privacy, tamper)
        sources.forEach { source ->
            assertFalse("Policy screen contains a direct English string literal", ENGLISH_LITERAL.containsMatchIn(source))
        }
    }

    @Test
    fun manifestLabelsAccessibilityServiceFromResources() {
        val manifest = repoFile("app/src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android:label=\"@string/accessibility_service_label\""))
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

    private fun normalizedText(text: String): String =
        text
            .replace("\\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun String.wordCount(): Int = split(Regex("\\s+")).count { it.isNotBlank() }

    private companion object {
        const val STRINGS_XML = "app/src/main/res/values/strings.xml"
        const val ACCESSIBILITY_SOURCE =
            "app/src/main/java/com/shortsblockerkids/feature/onboarding/AccessibilityDisclosureScreen.kt"
        const val PRIVACY_SOURCE =
            "app/src/main/java/com/shortsblockerkids/feature/privacy/PrivacyPolicyScreen.kt"
        const val TAMPER_SOURCE =
            "app/src/main/java/com/shortsblockerkids/feature/tamper/TamperProtectionDisclosureScreen.kt"

        val ENGLISH_LITERAL = Regex("\"(?:[^\"\\\\]|\\\\.)*[A-Za-z](?:[^\"\\\\]|\\\\.)*\"")

        val PRIVACY_BODY_RESOURCES =
            setOf(
                "privacy_policy_local_app_body",
                "privacy_policy_accessibility_service_body",
                "privacy_policy_tamper_protection_body",
                "privacy_policy_local_data_body",
                "privacy_policy_data_collection_body",
                "privacy_policy_payments_body",
                "privacy_policy_limitations_body",
            )

        val REQUIRED_STRING_RESOURCES =
            setOf(
                "common_back",
                "common_done",
                "common_not_now",
                "accessibility_disclosure_title",
                "accessibility_disclosure_body",
                "accessibility_disclosure_accept",
                "privacy_policy_title",
                "privacy_policy_local_app_title",
                "privacy_policy_local_app_body",
                "privacy_policy_accessibility_service_title",
                "privacy_policy_accessibility_service_body",
                "privacy_policy_tamper_protection_title",
                "privacy_policy_tamper_protection_body",
                "privacy_policy_local_data_title",
                "privacy_policy_local_data_body",
                "privacy_policy_data_collection_title",
                "privacy_policy_data_collection_body",
                "privacy_policy_payments_title",
                "privacy_policy_payments_body",
                "privacy_policy_limitations_title",
                "privacy_policy_limitations_body",
                "tamper_protection_disclosure_title",
                "tamper_protection_disclosure_body",
                "tamper_protection_status_active",
                "tamper_protection_status_optional_inactive",
                "tamper_protection_open_device_admin",
            )

        val ACCESSIBILITY_CLAIMS =
            setOf(
                "Android Accessibility Service",
                "protected short-video surfaces",
                "YouTube Shorts",
                "TikTok short-video feed",
                "Instagram Reels",
                "Facebook Reels",
                "com.ss.android.ugc.trill",
                "Facebook Lite are not supported",
                "prior real-device smoke evidence",
                "detector and unit coverage",
                "real-device QA",
                "parent PIN can temporarily allow access",
                "does not collect or send child data",
                "raw Accessibility tree dumps",
                "There is no account system, analytics, or ads",
                "Google Play billing verification",
                "Rules and PIN protection stay locally on this phone",
                "Protection is ON",
                "permission is turned off",
                "app is removed",
                "blocking stops",
            )

        val PRIVACY_CLAIMS =
            setOf(
                "works locally on this phone",
                "does not require an account",
                "billing backend limited to Google Play entitlement status",
                "Android Accessibility Service",
                "YouTube Shorts",
                "TikTok short-video feed",
                "Instagram Reels",
                "Facebook Reels",
                "com.ss.android.ugc.trill",
                "Facebook Lite are not supported",
                "Optional Device Admin tamper protection",
                "does not replace Accessibility Service",
                "block Android Settings",
                "parent PIN hash metadata stay on this phone",
                "parent PIN is not stored as plain text",
                "does not collect or send child data",
                "raw Accessibility tree dumps",
                "billing technical data",
                "stores hashed purchase tokens",
                "Protection is ON",
                "blocking stops",
            )

        val TAMPER_CLAIMS =
            setOf(
                "Device Admin tamper protection is optional",
                "harder to uninstall",
                "child cannot remove protection",
                "does not replace Android Accessibility Service",
                "Blocking still works only when Accessibility protection is enabled",
                "will not block Android Settings",
                "hidden system settings",
                "wipe the device",
                "lock the screen",
                "reset passwords",
                "manage other apps",
                "Android will show a warning",
                "Device Admin status",
                "turned off",
            )
    }
}
