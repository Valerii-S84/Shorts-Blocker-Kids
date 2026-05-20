package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeShortsDetectorTest {
    private val detector = YouTubeShortsDetector()

    @Test
    fun doesNotBlockNonYouTubePackage() {
        val result =
            detector.detect(
                packageName = "com.example.other",
                snapshot = shortsSnapshot(),
            )

        assertFalse(result.isShorts)
        assertEquals(Confidence.NONE, result.confidence)
    }

    @Test
    fun doesNotBlockNormalYouTubePageWithShortsTabAndHorizontalActions() {
        val result =
            detector.detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot = normalYouTubeWatchSnapshot(),
            )

        assertFalse(result.isShorts)
    }

    @Test
    fun blocksHighConfidenceShortsStructure() {
        val result =
            detector.detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot = shortsSnapshot(),
            )

        assertTrue(result.isShorts)
        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.matchedSignals.contains(DetectorSignal.REEL_CONTAINER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.ACTION_RAIL.id))
    }

    @Test
    fun blocksShortsScreenIdentifierWithFullscreenActionRail() {
        val result =
            detector.detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot = shortsUrlOrScreenSnapshot(),
            )

        assertTrue(result.shouldBlock)
        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.matchedSignals.contains(DetectorSignal.SHORTS_IDENTIFIER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.REEL_CONTAINER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.ACTION_RAIL.id))
    }

    @Test
    fun youtubeChannelPageDoesNotBlock() {
        val result =
            detector.detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot = channelPageSnapshot(),
            )

        assertFalse(result.shouldBlock)
    }

    @Test
    fun blocksReelContainerWithActionRailWithoutShortsText() {
        val result =
            detector.detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot = shortsSnapshot(contentDescriptionSignals = emptySet()),
            )

        assertTrue(result.isShorts)
        assertEquals(Confidence.HIGH, result.confidence)
        assertFalse(result.matchedSignals.contains(DetectorSignal.SHORTS_IDENTIFIER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.REEL_CONTAINER.id))
    }

    @Test
    fun keepsShortsTabOnlySignalBelowBlockingConfidence() {
        val result =
            detector.detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot = shortsTabOnlySnapshot(),
            )

        assertFalse(result.isShorts)
        assertEquals(Confidence.LOW, result.confidence)
        assertEquals(listOf(DetectorSignal.SHORTS_IDENTIFIER.id), result.matchedSignals)
    }

    private fun normalYouTubeWatchSnapshot(): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            nodes =
                listOf(
                    node(width = 1080, height = 2200, right = 1080, bottom = 2200, depth = 0),
                    node(
                        viewIdResourceName = "com.google.android.youtube:id/bottom_bar_shorts",
                        contentDescriptionSignals = setOf("shorts"),
                        left = 280,
                        top = 2070,
                        right = 420,
                        bottom = 2160,
                    ),
                    node(contentDescriptionSignals = setOf("like"), left = 40, top = 900),
                    node(contentDescriptionSignals = setOf("dislike"), left = 180, top = 900),
                    node(contentDescriptionSignals = setOf("share"), left = 320, top = 900),
                    node(contentDescriptionSignals = setOf("comments"), left = 460, top = 900),
                ),
        )

    private fun shortsSnapshot(contentDescriptionSignals: Set<String> = setOf("shorts")): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            nodes =
                listOf(
                    node(width = 1080, height = 2200, right = 1080, bottom = 2200, depth = 0),
                    node(
                        viewIdResourceName = "com.google.android.youtube:id/reel_watch_sequence",
                        width = 1080,
                        height = 2100,
                        right = 1080,
                        bottom = 2100,
                        depth = 2,
                        isScrollable = true,
                    ),
                    node(contentDescriptionSignals = contentDescriptionSignals, left = 40, top = 40),
                    node(contentDescriptionSignals = setOf("like"), left = 930, top = 780),
                    node(contentDescriptionSignals = setOf("comments"), left = 930, top = 1110),
                    node(contentDescriptionSignals = setOf("share"), left = 930, top = 1440),
                ),
        )

    private fun shortsUrlOrScreenSnapshot(): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            nodes =
                listOf(
                    node(width = 1080, height = 2200, right = 1080, bottom = 2200, depth = 0),
                    node(
                        viewIdResourceName = "com.google.android.youtube:id/shorts_watch",
                        width = 1080,
                        height = 2100,
                        right = 1080,
                        bottom = 2100,
                        depth = 2,
                        isScrollable = true,
                    ),
                    node(
                        viewIdResourceName = "com.google.android.youtube:id/reel_player_page",
                        width = 1080,
                        height = 2100,
                        right = 1080,
                        bottom = 2100,
                        depth = 3,
                    ),
                    node(contentDescriptionSignals = setOf("like"), left = 930, top = 780),
                    node(contentDescriptionSignals = setOf("comments"), left = 930, top = 1110),
                    node(contentDescriptionSignals = setOf("share"), left = 930, top = 1440),
                ),
        )

    private fun channelPageSnapshot(): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            nodes =
                listOf(
                    node(width = 1080, height = 2200, right = 1080, bottom = 2200, depth = 0),
                    node(contentDescriptionSignals = setOf("subscribe"), left = 40, top = 540),
                    node(contentDescriptionSignals = setOf("videos"), left = 120, top = 760),
                    node(contentDescriptionSignals = setOf("playlists"), left = 320, top = 760),
                    node(contentDescriptionSignals = setOf("shorts"), left = 520, top = 760),
                    node(contentDescriptionSignals = setOf("community"), left = 720, top = 760),
                ),
        )

    private fun shortsTabOnlySnapshot(): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            nodes =
                listOf(
                    node(width = 1080, height = 2200, right = 1080, bottom = 2200, depth = 0),
                    node(
                        viewIdResourceName = "com.google.android.youtube:id/bottom_bar_shorts",
                        contentDescriptionSignals = setOf("shorts"),
                        left = 280,
                        top = 2070,
                        right = 420,
                        bottom = 2160,
                    ),
                ),
        )

    private fun node(
        className: String = "android.view.View",
        viewIdResourceName: String? = null,
        contentDescriptionSignals: Set<String> = emptySet(),
        isScrollable: Boolean = false,
        left: Int = 0,
        top: Int = 0,
        right: Int = left + 120,
        bottom: Int = top + 120,
        width: Int = right - left,
        height: Int = bottom - top,
        depth: Int = 1,
    ): AccessibilityNodeSignal =
        AccessibilityNodeSignal(
            className = className,
            viewIdResourceName = viewIdResourceName,
            contentDescriptionSignals = contentDescriptionSignals,
            isClickable = true,
            isScrollable = isScrollable,
            isVisibleToUser = true,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            width = width,
            height = height,
            depth = depth,
        )
}
