package com.shortsblockerkids.presentation.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedAppsCallbacksTest {
    @Test
    fun callbacksDelegatePlatformChangesAndContinueEvents() {
        val events = mutableListOf<String>()
        val callbacks =
            ProtectedAppsCallbacks(
                onPlatformEnabledChanged = { platformId, enabled ->
                    events += "$platformId:$enabled"
                },
                onContinue = {
                    events += "continue"
                },
            )

        callbacks.onPlatformEnabledChanged("youtube_shorts", true)
        callbacks.onPlatformEnabledChanged("tiktok_short_video_feed", false)
        callbacks.onContinue()

        assertEquals(
            listOf(
                "youtube_shorts:true",
                "tiktok_short_video_feed:false",
                "continue",
            ),
            events,
        )
    }

    @Test
    fun dataClassEqualityUsesCallbackReferences() {
        val platformCallback: (String, Boolean) -> Unit = { _, _ -> }
        val continueCallback: () -> Unit = {}
        val callbacks =
            ProtectedAppsCallbacks(
                onPlatformEnabledChanged = platformCallback,
                onContinue = continueCallback,
            )
        val sameReferences =
            ProtectedAppsCallbacks(
                onPlatformEnabledChanged = platformCallback,
                onContinue = continueCallback,
            )
        val copied = callbacks.copy()
        val different =
            callbacks.copy(
                onContinue = {},
            )

        assertEquals(callbacks, sameReferences)
        assertEquals(callbacks.hashCode(), copied.hashCode())
        assertNotEquals(callbacks, different)
        assertTrue(callbacks.toString().contains("ProtectedAppsCallbacks"))
    }
}
