package com.shortsblockerkids.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ReleaseCopyInvariantTest {
    @Test
    fun readmeDocumentsTheCurrentPlatformSupportMatrix() {
        val readme = repoFile("README.md").readText()

        assertTrue(readme.contains("YouTube Shorts | supported"))
        assertTrue(
            readme.contains(
                "TikTok short-video feed `com.zhiliaoapp.musically` | " +
                    "supported by code; needs real-device QA",
            ),
        )
        assertTrue(readme.contains("Instagram Reels | supported by code; needs real-device QA"))
        assertTrue(readme.contains("Facebook Reels | supported by code; needs real-device QA"))
        assertTrue(
            readme.contains("TikTok regional `com.ss.android.ugc.trill` | not supported"),
        )
        assertTrue(readme.contains("Facebook Lite `com.facebook.lite` | not supported"))
        assertTrue(readme.contains("Optional Device Admin tamper protection"))
        assertFalse(readme.contains("YouTube-only"))
    }

    @Test
    fun inAppPrivacyAndDisclosureCopyMatchFullPlatformScope() {
        val policyResourceText = normalizedText(defaultPolicyResourceText())

        assertTrue(policyResourceText.contains("YouTube Shorts"))
        assertTrue(policyResourceText.contains("TikTok short-video feed"))
        assertTrue(policyResourceText.contains("Instagram Reels"))
        assertTrue(policyResourceText.contains("Facebook Reels"))
        assertTrue(policyResourceText.contains("com.ss.android.ugc.trill"))
        assertTrue(policyResourceText.contains("Facebook Lite"))
        assertTrue(policyResourceText.contains("not supported"))
        assertTrue(policyResourceText.contains("real-device QA"))
        assertFalse(policyResourceText.contains("code-level detectors"))
        assertFalse(policyResourceText.contains("YouTube-only"))
        assertFalse(policyResourceText.contains("supports all"))
        assertFalse(policyResourceText.contains("blocks all"))
    }

    @Test
    fun privacyPolicyDraftMatchesThePlatformSupportMatrix() {
        val privacyPolicy = repoFile("docs/SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md").readText()

        assertTrue(privacyPolicy.contains("Effective date: July 17, 2026"))
        assertFalse(privacyPolicy.contains("Effective date: May 31, 2026"))
        assertTrue(privacyPolicy.contains("YouTube Shorts | supported"))
        assertTrue(
            privacyPolicy.contains(
                "TikTok short-video feed `com.zhiliaoapp.musically` | " +
                    "supported by code; needs real-device QA",
            ),
        )
        assertTrue(
            privacyPolicy.contains("Instagram Reels | supported by code; needs real-device QA"),
        )
        assertTrue(
            privacyPolicy.contains("Facebook Reels | supported by code; needs real-device QA"),
        )
        assertTrue(
            privacyPolicy.contains("TikTok regional `com.ss.android.ugc.trill` | not supported"),
        )
        assertTrue(privacyPolicy.contains("Facebook Lite `com.facebook.lite` | not supported"))
        assertTrue(privacyPolicy.contains("Optional Device Admin tamper protection"))
        assertTrue(privacyPolicy.contains("needs real-device QA"))
    }

    @Test
    fun websitePrivacyPolicyDateMatchesDeviceAdminDisclosureRelease() {
        val config = repoFile("website/src/config.mjs").readText()
        val privacyPage = repoFile("website/src/pages/privacy.mjs").readText()
        val smokeTest = repoFile("website/scripts/smoke-test.mjs").readText()

        assertTrue(config.contains("lastUpdated: \"July 17, 2026\""))
        assertFalse(config.contains("lastUpdated: \"May 31, 2026\""))
        assertTrue(privacyPage.contains("Device Admin Tamper Protection"))
        assertTrue(smokeTest.contains("Last updated: July 17, 2026"))
        assertTrue(smokeTest.contains("Device Admin Tamper Protection"))
    }

    @Test
    fun manifestDeclaresTransparentDeviceAdminTamperProtection() {
        val manifest = repoFile("app/src/main/AndroidManifest.xml").readText()
        val strings = repoFile("app/src/main/res/values/strings.xml").readText()
        val deviceAdmin = repoFile("app/src/main/res/xml/tamper_protection_device_admin.xml").readText()

        assertTrue(manifest.contains(".core.tamper.TamperProtectionReceiver"))
        assertTrue(manifest.contains("android.permission.BIND_DEVICE_ADMIN"))
        assertTrue(manifest.contains("@xml/tamper_protection_device_admin"))
        assertTrue(manifest.contains("android:label=\"@string/accessibility_service_label\""))
        assertTrue(deviceAdmin.contains("<uses-policies />"))
        assertTrue(strings.contains("does not replace Accessibility protection"))
        assertTrue(strings.contains("does not block Settings"))
        assertTrue(
            strings.contains("Disabling it can allow Shorts Blocker Kids to be uninstalled"),
        )
    }

    @Test
    fun protectedAppsSetupScreenIsScrollableBeforeAccessibilitySetup() {
        val protectedAppsScreen =
            repoFile(
                "app/src/main/java/com/shortsblockerkids/feature/onboarding/ProtectedAppsScreen.kt",
            ).readText()

        assertTrue(protectedAppsScreen.contains("import androidx.compose.foundation.rememberScrollState"))
        assertTrue(protectedAppsScreen.contains("import androidx.compose.foundation.verticalScroll"))
        assertTrue(protectedAppsScreen.contains(".verticalScroll(rememberScrollState())"))
    }

    @Test
    fun privacyAndPlayPolicyDocsUseFinalPersonalDeveloperContact() {
        val privacyPolicy = repoFile("docs/SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md").readText()
        val playPolicyPackage = repoFile("docs/SHORTS_BLOCKER_PLAY_POLICY_PACKAGE.md").readText()

        listOf(privacyPolicy, playPolicyPackage).forEach { text ->
            assertTrue(text.contains("svalerii535@gmail.com"))
            assertTrue(text.contains("Valerii Serputko"))
            assertFalse(text.contains("TODO before submission"))
            assertFalse(text.contains("add the public privacy contact"))
            assertFalse(text.contains("add the Play listing entity"))
        }
    }

    @Test
    fun productionFacingDocsUseShortsBlockerKidsCanonicalUrls() {
        val productionDocs =
            listOf(
                repoFile("README.md").readText(),
                repoFile(".env.example").readText(),
                repoFile("docs/SHORTS_BLOCKER_AAB_RELEASE_READINESS.md").readText(),
                repoFile("docs/SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md").readText(),
                repoFile("docs/SHORTS_BLOCKER_PLAY_CONSOLE_PREPARATION_PACKAGE.md").readText(),
                repoFile("docs/SHORTS_BLOCKER_PLAY_CONSOLE_BILLING_CONFIG.md").readText(),
                repoFile("docs/SHORTS_BLOCKER_PLAY_POLICY_PACKAGE.md").readText(),
                repoFile("docs/SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md").readText(),
            )

        productionDocs.forEach { text ->
            assertTrue(text.contains("https://billing.shortsblockerkids.de"))
            assertFalse(text.contains("movashield.de"))
            assertFalse(text.contains("billing.example.com"))
        }

        val combinedDocs = productionDocs.joinToString(separator = "\n")
        assertTrue(combinedDocs.contains("https://shortsblockerkids.de"))
        assertTrue(combinedDocs.contains("https://shortsblockerkids.de/privacy"))
        assertTrue(combinedDocs.contains("https://shortsblockerkids.de/support"))
        assertTrue(combinedDocs.contains("https://billing.shortsblockerkids.de/billing/play/rtdn"))
        assertTrue(combinedDocs.contains("Valerii Serputko"))
        assertTrue(combinedDocs.contains("svalerii535@gmail.com"))
    }

    @Test
    fun playConsoleCopyKeepsExpandedPlatformQaCaveat() {
        val preparationPackage =
            repoFile("docs/SHORTS_BLOCKER_PLAY_CONSOLE_PREPARATION_PACKAGE.md").readText()
        val currentBehavior =
            preparationPackage
                .substringAfter("Current behavior to keep consistent across every Play Console answer:")
                .substringBefore("## Official Sources Checked")

        assertTrue(currentBehavior.contains("prior real-device smoke evidence"))
        assertTrue(currentBehavior.contains("detector/unit coverage"))
        assertTrue(currentBehavior.contains("require real-device QA before they are"))
        assertTrue(currentBehavior.contains("production-verified"))
        assertTrue(preparationPackage.contains("Current app-side support:"))
        assertTrue(preparationPackage.contains("must keep the real-device QA"))
        assertTrue(preparationPackage.contains("full device matrix records evidence"))
        assertTrue(preparationPackage.contains("Internal testers should verify those surfaces"))
        assertFalse(preparationPackage.contains("- TikTok short-video feed is supported."))
        assertFalse(preparationPackage.contains("- Instagram Reels is supported."))
        assertFalse(preparationPackage.contains("- Facebook Reels is supported."))
    }

    @Test
    fun publicProductionDeploymentEvidenceOmitsHostIdentifiers() {
        val productionEvidence =
            repoFile("docs/SHORTS_BLOCKER_PRODUCTION_DEPLOYMENT_2026-05-31.md").readText()
        val rawIpAddress = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")

        assertTrue(productionEvidence.contains("Public Hostnames Verified"))
        assertTrue(productionEvidence.contains("intentionally omitted"))
        assertFalse(rawIpAddress.containsMatchIn(productionEvidence))
        listOf(
            "valerchik.de",
            "api.valerchik.de",
            "/opt/",
            "/var/www",
            "/etc/",
            "/root/",
            "127.0.0.1",
            "Dockerized Caddy",
            "docker compose",
        ).forEach { forbidden ->
            assertFalse("public deployment evidence contains $forbidden", productionEvidence.contains(forbidden))
        }
    }

    @Test
    fun privacyCopyDeniesSensitiveChildAndAccessibilityDataCollection() {
        val privacySources =
            listOf(
                normalizedText(defaultPolicyResourceText()),
                normalizedText(repoFile("docs/SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md").readText()),
            )

        privacySources.forEach { text ->
            assertTrue(text.contains("does not collect") || text.contains("does not store"))
            assertTrue(text.contains("watch history"))
            assertTrue(text.contains("video titles"))
            assertTrue(text.contains("URLs"))
            assertTrue(text.contains("account names"))
            assertTrue(text.contains("screen recordings"))
            assertTrue(text.contains("audio"))
            assertTrue(text.contains("raw Accessibility tree dumps"))
            assertTrue(text.contains("parent PIN"))
            assertTrue(text.contains("not stored as plain text"))
        }
    }

    @Test
    fun billingPrivacyCopyLimitsBackendDataToEntitlementVerification() {
        val privacyPolicy = normalizedText(repoFile("docs/SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md").readText())
        val inAppPrivacy = normalizedText(defaultPolicyResourceText())

        assertTrue(privacyPolicy.contains("billing technical data"))
        assertTrue(privacyPolicy.contains("hashed purchase token"))
        assertTrue(inAppPrivacy.contains("billing backend limited to Google Play entitlement status"))
        assertTrue(inAppPrivacy.contains("stores hashed purchase tokens"))
    }

    @Test
    fun releaseBuildRequiresConfiguredHttpsBillingBackendUrl() {
        val buildConfig = repoFile("app/build.gradle.kts").readText()

        assertTrue(buildConfig.contains("validateProductionReleaseConfig"))
        assertTrue(buildConfig.contains("SBK_BILLING_BACKEND_BASE_URL is required for release builds."))
        assertTrue(buildConfig.contains("SBK_BILLING_BACKEND_BASE_URL must be https://billing.shortsblockerkids.de"))
        assertTrue(buildConfig.contains("\"assembleRelease\""))
        assertTrue(buildConfig.contains("\"bundleRelease\""))
    }

    @Test
    fun billingRepositoryRecordsFailClosedEntitlementOnBackendFailures() {
        val repositorySource =
            repoFile(
                "app/src/main/java/com/shortsblockerkids/core/billing/PlayBillingRepository.kt",
            ).readText()
        val failureRecordings =
            Regex("\\.onFailure \\{\\s+recordFailClosedEntitlement\\(")
                .findAll(repositorySource)
                .count()

        assertTrue(repositorySource.contains("verificationPolicy.failClosedSnapshot"))
        assertTrue("expected verify and refresh failure handlers", failureRecordings >= 2)
    }

    @Test
    fun releaseReadinessDocsRecordBillingBackendAndPermissionBoundaries() {
        val releaseReadiness = repoFile("docs/SHORTS_BLOCKER_AAB_RELEASE_READINESS.md").readText()
        val backendRunbook = repoFile("docs/SHORTS_BLOCKER_PLAY_BILLING_BACKEND_RUNBOOK.md").readText()

        assertTrue(releaseReadiness.contains("SBK_BILLING_BACKEND_BASE_URL"))
        assertTrue(releaseReadiness.contains("Release fail-closed behavior"))
        assertTrue(backendRunbook.contains("Production Gates"))
        assertTrue(backendRunbook.contains("Play license tester"))
        assertTrue(backendRunbook.contains("Do not remove `android.permission.INTERNET`"))
    }

    @Test
    fun accessibilityServiceReceivesOnlyProtectedPlatformPackages() {
        val serviceConfig =
            repoFile(
                "app/src/main/res/xml/shorts_blocker_accessibility_service.xml",
            ).readText()

        assertTrue(serviceConfig.contains(YouTubeShortsDetector.YOUTUBE_PACKAGE))
        assertTrue(serviceConfig.contains(TikTokShortVideoDetector.TIKTOK_PACKAGE))
        assertTrue(serviceConfig.contains(InstagramReelsDetector.INSTAGRAM_PACKAGE))
        assertTrue(serviceConfig.contains(FacebookReelsDetector.FACEBOOK_PACKAGE))
        assertFalse(serviceConfig.contains(PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE))
        assertFalse(serviceConfig.contains(PlatformSupportMatrix.FACEBOOK_LITE_PACKAGE))
    }

    @Test
    fun debugAccessibilityServiceAllowsOnlyProtectedAndFixturePackages() {
        val serviceConfig =
            repoFile(
                "app/src/debug/res/xml/shorts_blocker_accessibility_service.xml",
            ).readText()

        assertTrue(serviceConfig.contains(YouTubeShortsDetector.YOUTUBE_PACKAGE))
        assertTrue(serviceConfig.contains(TikTokShortVideoDetector.TIKTOK_PACKAGE))
        assertTrue(serviceConfig.contains(InstagramReelsDetector.INSTAGRAM_PACKAGE))
        assertTrue(serviceConfig.contains(FacebookReelsDetector.FACEBOOK_PACKAGE))
        assertTrue(serviceConfig.contains(DebugFixturePackages.YOUTUBE))
        assertTrue(serviceConfig.contains(DebugFixturePackages.TIKTOK))
        assertTrue(serviceConfig.contains(DebugFixturePackages.INSTAGRAM))
        assertTrue(serviceConfig.contains(DebugFixturePackages.FACEBOOK))
        assertFalse(serviceConfig.contains(PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE))
        assertFalse(serviceConfig.contains(PlatformSupportMatrix.FACEBOOK_LITE_PACKAGE))
        assertFalse(serviceConfig.contains("com.example.shortvideo.fixture"))
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
        error("Missing repository file: $relativePath")
    }

    private fun normalizedText(text: String): String =
        text
            .replace("\\n", " ")
            .replace(Regex("[\"+\\r\\n]+"), " ")
            .replace(Regex("\\s+"), " ")

    private fun defaultPolicyResourceText(): String =
        POLICY_RESOURCE_NAMES.joinToString(separator = "\n") { resourceName ->
            stringValue(resourceName)
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
            .parse(repoFile("app/src/main/res/values/strings.xml"))
    }

    private companion object {
        val POLICY_RESOURCE_NAMES =
            setOf(
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
    }
}
