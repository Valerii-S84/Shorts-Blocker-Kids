package com.shortsblockerkids.accessibility

import androidx.annotation.StringRes
import com.shortsblockerkids.R
import com.shortsblockerkids.core.storage.AppSettings

data class SupportedPlatform(
    val id: String,
) {
    companion object {
        val YOUTUBE_SHORTS =
            SupportedPlatform(
                id = AppSettings.YOUTUBE_SHORTS_PLATFORM_ID,
            )
        val TIKTOK =
            SupportedPlatform(
                id = AppSettings.TIKTOK_PLATFORM_ID,
            )
        val INSTAGRAM_REELS =
            SupportedPlatform(
                id = AppSettings.INSTAGRAM_REELS_PLATFORM_ID,
            )
        val FACEBOOK_REELS =
            SupportedPlatform(
                id = AppSettings.FACEBOOK_REELS_PLATFORM_ID,
            )

        val PROTECTED_PLATFORMS =
            listOf(
                YOUTUBE_SHORTS,
                TIKTOK,
                INSTAGRAM_REELS,
                FACEBOOK_REELS,
            )
    }
}

enum class PlatformSupportStatus {
    SUPPORTED,
    SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
    NOT_SUPPORTED,
}

data class PlatformSupportEntry(
    val platformId: String,
    @param:StringRes val platformNameRes: Int,
    val packageName: String,
    val status: PlatformSupportStatus,
)

object PlatformSupportMatrix {
    const val TIKTOK_REGIONAL_PACKAGE = "com.ss.android.ugc.trill"
    const val FACEBOOK_LITE_PACKAGE = "com.facebook.lite"

    val entries =
        listOf(
            PlatformSupportEntry(
                platformId = SupportedPlatform.YOUTUBE_SHORTS.id,
                platformNameRes = R.string.platform_youtube_shorts,
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                status = PlatformSupportStatus.SUPPORTED,
            ),
            PlatformSupportEntry(
                platformId = SupportedPlatform.TIKTOK.id,
                platformNameRes = R.string.platform_tiktok_short_video_feed,
                packageName = TikTokShortVideoDetector.TIKTOK_PACKAGE,
                status = PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
            ),
            PlatformSupportEntry(
                platformId = "tiktok_regional",
                platformNameRes = R.string.platform_tiktok_regional,
                packageName = TIKTOK_REGIONAL_PACKAGE,
                status = PlatformSupportStatus.NOT_SUPPORTED,
            ),
            PlatformSupportEntry(
                platformId = SupportedPlatform.INSTAGRAM_REELS.id,
                platformNameRes = R.string.platform_instagram_reels,
                packageName = InstagramReelsDetector.INSTAGRAM_PACKAGE,
                status = PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
            ),
            PlatformSupportEntry(
                platformId = SupportedPlatform.FACEBOOK_REELS.id,
                platformNameRes = R.string.platform_facebook_reels,
                packageName = FacebookReelsDetector.FACEBOOK_PACKAGE,
                status = PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA,
            ),
            PlatformSupportEntry(
                platformId = "facebook_lite",
                platformNameRes = R.string.platform_facebook_lite,
                packageName = FACEBOOK_LITE_PACKAGE,
                status = PlatformSupportStatus.NOT_SUPPORTED,
            ),
        )

    val protectedEntries: List<PlatformSupportEntry>
        get() = entries.filterNot { it.status == PlatformSupportStatus.NOT_SUPPORTED }

    val unsupportedEntries: List<PlatformSupportEntry>
        get() = entries.filter { it.status == PlatformSupportStatus.NOT_SUPPORTED }
}
