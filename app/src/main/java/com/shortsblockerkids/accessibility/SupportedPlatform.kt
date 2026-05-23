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
                displayName = "TikTok",
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
