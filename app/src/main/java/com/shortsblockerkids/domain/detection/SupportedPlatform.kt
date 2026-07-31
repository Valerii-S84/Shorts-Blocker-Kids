package com.shortsblockerkids.domain.detection

data class SupportedPlatform(
    val id: String,
) {
    companion object {
        val YOUTUBE_SHORTS = SupportedPlatform(id = "youtube_shorts")
        val TIKTOK = SupportedPlatform(id = "tiktok")
        val INSTAGRAM_REELS = SupportedPlatform(id = "instagram_reels")
        val FACEBOOK_REELS = SupportedPlatform(id = "facebook_reels")

        val PROTECTED_PLATFORMS =
            listOf(
                YOUTUBE_SHORTS,
                TIKTOK,
                INSTAGRAM_REELS,
                FACEBOOK_REELS,
            )
    }
}
