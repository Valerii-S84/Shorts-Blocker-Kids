package com.shortsblockerkids.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseCopyInvariantTest {
    @Test
    fun readmeDocumentsTheCurrentPlatformSupportMatrix() {
        val readme = repoFile("README.md").readText()

        assertTrue(readme.contains("YouTube Shorts | supported"))
        assertTrue(readme.contains("TikTok main `com.zhiliaoapp.musically` | supported by code; needs real-device QA"))
        assertTrue(readme.contains("TikTok regional `com.ss.android.ugc.trill` | not supported"))
        assertTrue(readme.contains("Facebook Lite `com.facebook.lite` | not supported"))
    }

    @Test
    fun inAppPrivacyAndDisclosureCopyDoNotOverclaimSupport() {
        val disclosure =
            repoFile(
                "app/src/main/java/com/shortsblockerkids/feature/onboarding/AccessibilityDisclosureScreen.kt",
            ).readText()
        val privacy =
            repoFile(
                "app/src/main/java/com/shortsblockerkids/feature/privacy/PrivacyPolicyScreen.kt",
            ).readText()

        listOf(disclosure, privacy).forEach { text ->
            assertTrue(text.contains("real-device"))
            assertTrue(text.contains("QA"))
            assertTrue(text.contains("com.ss.android.ugc.trill"))
            assertTrue(text.contains("Facebook"))
            assertTrue(text.contains("Lite"))
            assertTrue(text.contains("not supported"))
            assertFalse(text.contains("supports all"))
            assertFalse(text.contains("blocks all"))
        }
    }

    @Test
    fun privacyPolicyDraftMatchesThePlatformSupportMatrix() {
        val privacyPolicy = repoFile("docs/SHORTS_BLOCKER_PRIVACY_POLICY_DRAFT.md").readText()

        assertTrue(privacyPolicy.contains("YouTube Shorts | supported"))
        assertTrue(privacyPolicy.contains("TikTok main `com.zhiliaoapp.musically` | supported by code; needs real-device QA"))
        assertTrue(privacyPolicy.contains("TikTok regional `com.ss.android.ugc.trill` | not supported"))
        assertTrue(privacyPolicy.contains("Facebook Lite `com.facebook.lite` | not supported"))
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
    fun privacyCopyDeniesSensitiveChildAndAccessibilityDataCollection() {
        val privacySources =
            listOf(
                normalizedText(
                    repoFile(
                        "app/src/main/java/com/shortsblockerkids/feature/privacy/PrivacyPolicyScreen.kt",
                    ).readText(),
                ),
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
        val inAppPrivacy =
            normalizedText(
                repoFile(
                    "app/src/main/java/com/shortsblockerkids/feature/privacy/PrivacyPolicyScreen.kt",
                ).readText(),
            )

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
        assertTrue(buildConfig.contains("SBK_BILLING_BACKEND_BASE_URL must be an https URL with a host"))
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
            .replace(Regex("[\"+\\r\\n]+"), " ")
            .replace(Regex("\\s+"), " ")
}
