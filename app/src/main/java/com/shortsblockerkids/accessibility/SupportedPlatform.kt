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
    }
}
