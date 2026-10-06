package com.shortsblockerkids.application.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityProtectionStateTest {
    @Test
    fun tracksGlobalProtectionAndEnabledPlatforms() {
        val state =
            AccessibilityProtectionState(
                isProtectionActive = true,
                enabledPlatformIds = setOf("youtube_shorts", "instagram_reels"),
            )

        assertTrue(state.isProtectionActive)
        assertTrue(state.isPlatformEnabled("youtube_shorts"))
        assertTrue(state.isPlatformEnabled("instagram_reels"))
        assertFalse(state.isPlatformEnabled("tiktok_short_video_feed"))
    }

    @Test
    fun dataClassEqualityUsesProtectionAndPlatformState() {
        val active =
            AccessibilityProtectionState(
                isProtectionActive = true,
                enabledPlatformIds = setOf("youtube_shorts"),
            )
        val disabled = active.copy(isProtectionActive = false)
        val sameAsActive =
            AccessibilityProtectionState(
                isProtectionActive = true,
                enabledPlatformIds = setOf("youtube_shorts"),
            )

        assertEquals(active, sameAsActive)
        assertEquals(active.hashCode(), sameAsActive.hashCode())
        assertNotEquals(active, disabled)
        assertTrue(active.toString().contains("isProtectionActive=true"))
    }
}
