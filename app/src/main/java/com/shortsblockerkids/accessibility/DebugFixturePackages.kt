package com.shortsblockerkids.accessibility

import com.shortsblockerkids.BuildConfig

object DebugFixturePackages {
    const val YOUTUBE = "com.shortsblockerkids.fixture.youtube"
    const val TIKTOK = "com.shortsblockerkids.fixture.tiktok"
    const val INSTAGRAM = "com.shortsblockerkids.fixture.instagram"
    const val FACEBOOK = "com.shortsblockerkids.fixture.facebook"

    fun enabled(packageName: String): Set<String> =
        if (BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED) {
            setOf(packageName)
        } else {
            emptySet()
        }
}
