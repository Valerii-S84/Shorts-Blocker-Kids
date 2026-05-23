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
}
