package com.shortsblockerkids.accessibility

import com.shortsblockerkids.R
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
                    R.string.platform_youtube_shorts,
                    YouTubeShortsDetector.YOUTUBE_PACKAGE,
                    PlatformSupportStatus.SUPPORTED,
                ),
                support(
                    SupportedPlatform.TIKTOK.id,
                    R.string.platform_tiktok_short_video_feed,
                    TikTokShortVideoDetector.TIKTOK_PACKAGE,
                    PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
                ),
                support(
                    "tiktok_regional",
                    R.string.platform_tiktok_regional,
                    PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE,
                    PlatformSupportStatus.NOT_SUPPORTED,
                ),
                support(
                    SupportedPlatform.INSTAGRAM_REELS.id,
                    R.string.platform_instagram_reels,
                    InstagramReelsDetector.INSTAGRAM_PACKAGE,
                    PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
                ),
                support(
                    SupportedPlatform.FACEBOOK_REELS.id,
                    R.string.platform_facebook_reels,
                    FacebookReelsDetector.FACEBOOK_PACKAGE,
                    PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
                ),
                support(
                    "facebook_lite",
                    R.string.platform_facebook_lite,
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
    fun platformSupportMatrixSeparatesProtectedAndUnsupportedPackages() {
        assertEquals(
            listOf(
                YouTubeShortsDetector.YOUTUBE_PACKAGE,
                TikTokShortVideoDetector.TIKTOK_PACKAGE,
                InstagramReelsDetector.INSTAGRAM_PACKAGE,
                FacebookReelsDetector.FACEBOOK_PACKAGE,
            ),
            PlatformSupportMatrix.protectedEntries.map { it.packageName },
        )
        assertEquals(
            listOf(
                PlatformSupportMatrix.TIKTOK_REGIONAL_PACKAGE,
                PlatformSupportMatrix.FACEBOOK_LITE_PACKAGE,
            ),
            PlatformSupportMatrix.unsupportedEntries.map { it.packageName },
        )
        assertTrue(
            PlatformSupportMatrix.protectedEntries.none {
                it.status == PlatformSupportStatus.NOT_SUPPORTED
            },
        )
        assertTrue(
            PlatformSupportMatrix.unsupportedEntries.all {
                it.status == PlatformSupportStatus.NOT_SUPPORTED
            },
        )
    }

    private fun support(
        platformId: String,
        platformNameRes: Int,
        packageName: String,
        status: PlatformSupportStatus,
    ): PlatformSupportEntry =
        PlatformSupportEntry(
            platformId = platformId,
            platformNameRes = platformNameRes,
            packageName = packageName,
            status = status,
        )
}
