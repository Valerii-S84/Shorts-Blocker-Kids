package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformSupportMatrixTest {
    @Test
    fun platformSupportMatrixMatchesCurrentReleaseScope() {
        assertEquals(
            listOf(
                support(
                    SupportedPlatform.YOUTUBE_SHORTS.id,
                    "YouTube Shorts",
                    YouTubeShortsDetector.YOUTUBE_PACKAGE,
                    PlatformSupportStatus.SUPPORTED,
                ),
                support(
                    SupportedPlatform.TIKTOK.id,
                    "TikTok short-video feed",
                    TikTokShortVideoDetector.TIKTOK_PACKAGE,
                    PlatformSupportStatus.SUPPORTED,
                ),
                support(
                    "tiktok_regional",
                    "TikTok regional",
                    PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE,
                    PlatformSupportStatus.NOT_SUPPORTED,
                ),
                support(
                    SupportedPlatform.INSTAGRAM_REELS.id,
                    "Instagram Reels",
                    InstagramReelsDetector.INSTAGRAM_PACKAGE,
                    PlatformSupportStatus.SUPPORTED,
                ),
                support(
                    SupportedPlatform.FACEBOOK_REELS.id,
                    "Facebook Reels",
                    FacebookReelsDetector.FACEBOOK_PACKAGE,
                    PlatformSupportStatus.SUPPORTED,
                ),
                support(
                    "facebook_lite",
                    "Facebook Lite",
                    PlatformSupportMatrix.FACEBOOK_LITE_PACKAGE,
                    PlatformSupportStatus.NOT_SUPPORTED,
                ),
            ),
            PlatformSupportMatrix.entries,
        )
    }

    @Test
    fun productionEngineDoesNotSupportUnsupportedRegionalOrLitePackages() {
        val engine = ShortVideoDetectionEngine.production()

        assertFalse(engine.supportsPackage(PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE))
        assertFalse(engine.supportsPackage(PlatformSupportMatrix.FACEBOOK_LITE_PACKAGE))
    }

    @Test
    fun protectedSummaryDoesNotClaimUnsupportedPackages() {
        val protectedSummary = PlatformSupportMatrix.protectedSummary()
        val unsupportedSummary = PlatformSupportMatrix.unsupportedSummary()

        assertTrue(protectedSummary.contains("YouTube Shorts: supported"))
        assertTrue(protectedSummary.contains("TikTok short-video feed: supported"))
        assertTrue(protectedSummary.contains("Instagram Reels: supported"))
        assertTrue(protectedSummary.contains("Facebook Reels: supported"))
        assertFalse(protectedSummary.contains(PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE))
        assertFalse(protectedSummary.contains(PlatformSupportMatrix.FACEBOOK_LITE_PACKAGE))
        assertTrue(unsupportedSummary.contains(PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE))
        assertTrue(unsupportedSummary.contains(PlatformSupportMatrix.FACEBOOK_LITE_PACKAGE))
    }

    private fun support(
        platformId: String,
        platformName: String,
        packageName: String,
        status: PlatformSupportStatus,
    ): PlatformSupportEntry =
        PlatformSupportEntry(
            platformId = platformId,
            platformName = platformName,
            packageName = packageName,
            status = status,
        )
}
