package com.shortsblockerkids.accessibility

data class SupportedPlatform(
    val id: String,
    val displayName: String,
) {
    companion object {
        val YOUTUBE_SHORTS =
            SupportedPlatform(
                id = "youtube_shorts",
                displayName = "YouTube Shorts",
            )
        val TIKTOK =
            SupportedPlatform(
                id = "tiktok",
                displayName = "TikTok main",
            )
        val INSTAGRAM_REELS =
            SupportedPlatform(
                id = "instagram_reels",
                displayName = "Instagram Reels",
            )
        val FACEBOOK_REELS =
            SupportedPlatform(
                id = "facebook_reels",
                displayName = "Facebook Reels",
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

enum class PlatformSupportStatus(
    val label: String,
) {
    SUPPORTED("supported"),
    CODE_SUPPORTED_NEEDS_REAL_DEVICE_QA("supported by code; needs real-device QA"),
    NOT_SUPPORTED("not supported"),
}

data class PlatformSupportEntry(
    val platformName: String,
    val packageName: String,
    val status: PlatformSupportStatus,
)

object PlatformSupportMatrix {
    const val TIKTOK_REGIONAL_PACKAGE = "com.ss.android.ugc.trill"
    const val FACEBOOK_LITE_PACKAGE = "com.facebook.lite"

    val entries =
        listOf(
            PlatformSupportEntry(
                platformName = "YouTube Shorts",
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                status = PlatformSupportStatus.SUPPORTED,
            ),
            PlatformSupportEntry(
                platformName = "TikTok main",
                packageName = TikTokShortVideoDetector.TIKTOK_PACKAGE,
                status = PlatformSupportStatus.CODE_SUPPORTED_NEEDS_REAL_DEVICE_QA,
            ),
            PlatformSupportEntry(
                platformName = "TikTok regional",
                packageName = TIKTOK_REGIONAL_PACKAGE,
                status = PlatformSupportStatus.NOT_SUPPORTED,
            ),
            PlatformSupportEntry(
                platformName = "Instagram Reels",
                packageName = InstagramReelsDetector.INSTAGRAM_PACKAGE,
                status = PlatformSupportStatus.CODE_SUPPORTED_NEEDS_REAL_DEVICE_QA,
            ),
            PlatformSupportEntry(
                platformName = "Facebook Reels",
                packageName = FacebookReelsDetector.FACEBOOK_PACKAGE,
                status = PlatformSupportStatus.CODE_SUPPORTED_NEEDS_REAL_DEVICE_QA,
            ),
            PlatformSupportEntry(
                platformName = "Facebook Lite",
                packageName = FACEBOOK_LITE_PACKAGE,
                status = PlatformSupportStatus.NOT_SUPPORTED,
            ),
        )

    val protectedEntries: List<PlatformSupportEntry>
        get() = entries.filterNot { it.status == PlatformSupportStatus.NOT_SUPPORTED }

    val unsupportedEntries: List<PlatformSupportEntry>
        get() = entries.filter { it.status == PlatformSupportStatus.NOT_SUPPORTED }

    fun protectedSummary(): String =
        protectedEntries.joinToString { entry ->
            "${entry.platformName}: ${entry.status.label}"
        }

    fun unsupportedSummary(): String =
        unsupportedEntries.joinToString { entry ->
            "${entry.platformName} (${entry.packageName})"
        }
}
