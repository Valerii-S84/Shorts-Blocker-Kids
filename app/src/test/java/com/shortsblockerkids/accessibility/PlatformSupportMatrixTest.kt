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
                support("YouTube Shorts", YouTubeShortsDetector.YOUTUBE_PACKAGE, PlatformSupportStatus.SUPPORTED),
                support(
                    "TikTok main",
                    TikTokShortVideoDetector.TIKTOK_PACKAGE,
                    PlatformSupportStatus.CODE_SUPPORTED_NEEDS_REAL_DEVICE_QA,
                ),
                support(
                    "TikTok regional",
                    PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE,
                    PlatformSupportStatus.NOT_SUPPORTED,
                ),
                support(
                    "Instagram Reels",
                    InstagramReelsDetector.INSTAGRAM_PACKAGE,
                    PlatformSupportStatus.CODE_SUPPORTED_NEEDS_REAL_DEVICE_QA,
                ),
                support(
                    "Facebook Reels",
                    FacebookReelsDetector.FACEBOOK_PACKAGE,
                    PlatformSupportStatus.CODE_SUPPORTED_NEEDS_REAL_DEVICE_QA,
                ),
                support(
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
        assertTrue(protectedSummary.contains("TikTok main: supported by code; needs real-device QA"))
        assertFalse(protectedSummary.contains(PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE))
        assertFalse(protectedSummary.contains(PlatformSupportMatrix.FACEBOOK_LITE_PACKAGE))
        assertTrue(unsupportedSummary.contains(PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE))
        assertTrue(unsupportedSummary.contains(PlatformSupportMatrix.FACEBOOK_LITE_PACKAGE))
    }

    private fun support(
        platformName: String,
        packageName: String,
        status: PlatformSupportStatus,
    ): PlatformSupportEntry =
        PlatformSupportEntry(
            platformName = platformName,
            packageName = packageName,
            status = status,
        )
}
