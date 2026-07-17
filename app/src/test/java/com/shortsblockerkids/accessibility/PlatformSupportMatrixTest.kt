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
                    PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
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
                    PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
                ),
                support(
                    SupportedPlatform.FACEBOOK_REELS.id,
                    "Facebook Reels",
                    FacebookReelsDetector.FACEBOOK_PACKAGE,
                    PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
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
        assertTrue(
            protectedSummary.contains(
                "TikTok short-video feed: supported by code; needs real-device QA",
            ),
        )
        assertTrue(
            protectedSummary.contains("Instagram Reels: supported by code; needs real-device QA"),
        )
        assertTrue(
            protectedSummary.contains("Facebook Reels: supported by code; needs real-device QA"),
        )
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
