package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortVideoDetectorContractTest {
    @Test
    fun eachPlatformBlocksHighConfidenceShortVideo() {
        platformCases.forEach { platformCase ->
            val result =
                platformCase.detector.detect(
                    packageName = platformCase.packageName,
                    snapshot = shortVideoSnapshot(platformCase),
                )

            assertEquals(platformCase.name, Confidence.HIGH, result.confidence)
            assertTrue(platformCase.name, result.shouldBlock)
            assertTrue(platformCase.name, result.matchedSignals.contains(DetectorSignal.ACTION_RAIL.id))
            assertTrue(platformCase.name, result.matchedSignals.contains(DetectorSignal.VERTICAL_FULLSCREEN.id))
        }
    }

    @Test
    fun eachPlatformBlocksShortVideoWithoutExplicitShortsOrReelsText() {
        platformCases.forEach { platformCase ->
            val result =
                platformCase.detector.detect(
                    packageName = platformCase.packageName,
                    snapshot = shortVideoSnapshot(platformCase, explicitSignal = null),
                )

            assertEquals(platformCase.name, Confidence.HIGH, result.confidence)
            assertTrue(platformCase.name, result.shouldBlock)
            assertFalse(platformCase.name, result.matchedSignals.contains(DetectorSignal.SHORTS_IDENTIFIER.id))
        }
    }

    @Test
    fun eachPlatformDoesNotBlockNormalHomeFeed() {
        platformCases.forEach { platformCase ->
            val result =
                platformCase.detector.detect(
                    packageName = platformCase.packageName,
                    snapshot = normalHomeFeedSnapshot(platformCase),
                )

            assertFalse(platformCase.name, result.shouldBlock)
        }
    }

    @Test
    fun eachPlatformDoesNotBlockProfileSearchOrSettingsScreens() {
        val screenSignals = listOf("profile", "search", "settings")

        platformCases.forEach { platformCase ->
            screenSignals.forEach { screenSignal ->
                val result =
                    platformCase.detector.detect(
                        packageName = platformCase.packageName,
                        snapshot = labeledScreenSnapshot(screenSignal),
                    )

                assertFalse("${platformCase.name} $screenSignal", result.shouldBlock)
            }
        }
    }

    @Test
    fun eachPlatformDoesNotBlockFalsePositiveWorkflowScreens() {
        platformCases.forEach { platformCase ->
            platformCase.falsePositiveSurfaces.forEach { surface ->
                val result =
                    platformCase.detector.detect(
                        packageName = platformCase.packageName,
                        snapshot = falsePositiveWorkflowSnapshot(platformCase, surface),
                    )

                assertFalse("${platformCase.name} ${surface.signal}", result.shouldBlock)
            }
        }
    }

    @Test
    fun textOnlyShortVideoNamesDoNotBlock() {
        platformCases.forEach { platformCase ->
            val result =
                platformCase.detector.detect(
                    packageName = platformCase.packageName,
                    snapshot = labeledScreenSnapshot(platformCase.explicitSignal),
                )

            assertFalse(platformCase.name, result.shouldBlock)
        }
    }

    @Test
    fun genericFullscreenVideoWithActionRailDoesNotBlockWithoutFeedOrReelEvidence() {
        platformCases.forEach { platformCase ->
            val result =
                platformCase.detector.detect(
                    packageName = platformCase.packageName,
                    snapshot = genericFullscreenVideoSnapshot(platformCase),
                )

            assertFalse(platformCase.name, result.shouldBlock)
        }
    }

    @Test
    fun localizedInterfaceSignalsBlockOnlyWithShortVideoStructure() {
        platformCases.forEach { platformCase ->
            val result =
                platformCase.detector.detect(
                    packageName = platformCase.packageName,
                    snapshot =
                        shortVideoSnapshot(
                            platformCase = platformCase,
                            explicitSignal = platformCase.localizedExplicitSignal,
                            actionSignals = localizedActionSignals,
                            navigationSignals = platformCase.localizedNavigationSignals,
                        ),
                )

            assertTrue(platformCase.name, result.shouldBlock)
        }
    }

    @Test
    fun alternateActionButtonNamesStillBlockWhenStructureIsPresent() {
        platformCases.forEach { platformCase ->
            val result =
                platformCase.detector.detect(
                    packageName = platformCase.packageName,
                    snapshot =
                        shortVideoSnapshot(
                            platformCase = platformCase,
                            actionSignals = platformCase.alternateActionSignals,
                        ),
                )

            assertTrue(platformCase.name, result.shouldBlock)
        }
    }

    @Test
    fun emptyTreeDoesNotBlockOrThrow() {
        platformCases.forEach { platformCase ->
            val result =
                platformCase.detector.detect(
                    packageName = platformCase.packageName,
                    snapshot = AccessibilityTreeSnapshot.Empty,
                )

            assertFalse(platformCase.name, result.shouldBlock)
            assertEquals(platformCase.name, Confidence.NONE, result.confidence)
        }
    }

    @Test
    fun unknownPackageDoesNotBlockEvenWithShortVideoStructure() {
        platformCases.forEach { platformCase ->
            val result =
                platformCase.detector.detect(
                    packageName = UNKNOWN_PACKAGE,
                    snapshot = shortVideoSnapshot(platformCase),
                )

            assertEquals(platformCase.name, DetectionResult.None, result)
        }
    }

    private fun shortVideoSnapshot(
        platformCase: PlatformCase,
        explicitSignal: String? = platformCase.explicitSignal,
        actionSignals: List<String> = platformCase.primaryActionSignals,
        navigationSignals: List<String> = platformCase.navigationSignals,
    ): AccessibilityTreeSnapshot {
        val nodes =
            mutableListOf(
                rootNode(),
                node(
                    className = "androidx.viewpager.widget.ViewPager",
                    viewIdResourceName = platformCase.shortVideoContainerViewId,
                    isScrollable = true,
                    left = 0,
                    top = 0,
                    right = SCREEN_WIDTH,
                    bottom = SHORT_VIDEO_BOTTOM,
                    width = SCREEN_WIDTH,
                    height = SHORT_VIDEO_BOTTOM,
                    depth = 2,
                ),
            )
        explicitSignal?.let { signal ->
            nodes += node(contentDescriptionSignals = setOf(signal), left = 430, top = 40)
        }
        actionSignals.forEachIndexed { index, signal ->
            nodes +=
                node(
                    contentDescriptionSignals = setOf(signal),
                    left = ACTION_RAIL_LEFT,
                    top = 730 + index * 350,
                    right = ACTION_RAIL_RIGHT,
                    bottom = 850 + index * 350,
                    depth = 5,
                )
        }
        navigationSignals.forEachIndexed { index, signal ->
            nodes +=
                node(
                    contentDescriptionSignals = setOf(signal),
                    left = 40 + index * 260,
                    top = 2_070,
                    right = 160 + index * 260,
                    bottom = 2_160,
                    depth = 3,
                )
        }

        return AccessibilityTreeSnapshot(nodes = nodes)
    }

    private fun normalHomeFeedSnapshot(platformCase: PlatformCase): AccessibilityTreeSnapshot {
        val feedNodes =
            listOf(
                rootNode(),
                node(
                    className = "androidx.recyclerview.widget.RecyclerView",
                    viewIdResourceName = platformCase.normalFeedViewId,
                    isScrollable = true,
                    left = 0,
                    top = 120,
                    right = SCREEN_WIDTH,
                    bottom = 1_920,
                    width = SCREEN_WIDTH,
                    height = 1_800,
                    depth = 2,
                ),
                node(contentDescriptionSignals = setOf("like"), left = 40, top = 980),
                node(contentDescriptionSignals = setOf("comment"), left = 200, top = 980),
                node(contentDescriptionSignals = setOf("share"), left = 360, top = 980),
            )
        val navigationNodes =
            platformCase.navigationSignals.mapIndexed { index, signal ->
                node(
                    contentDescriptionSignals = setOf(signal),
                    left = 40 + index * 260,
                    top = 2_070,
                    right = 160 + index * 260,
                    bottom = 2_160,
                )
            }

        return AccessibilityTreeSnapshot(nodes = feedNodes + navigationNodes)
    }

    private fun labeledScreenSnapshot(signal: String): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            nodes =
                listOf(
                    rootNode(),
                    node(contentDescriptionSignals = setOf(signal), left = 420, top = 120),
                ),
        )

    private fun genericFullscreenVideoSnapshot(platformCase: PlatformCase): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            nodes =
                listOf(
                    rootNode(),
                    node(
                        viewIdResourceName = platformCase.genericVideoViewId,
                        left = 0,
                        top = 0,
                        right = SCREEN_WIDTH,
                        bottom = SHORT_VIDEO_BOTTOM,
                        width = SCREEN_WIDTH,
                        height = SHORT_VIDEO_BOTTOM,
                        depth = 2,
                    ),
                    node(contentDescriptionSignals = setOf("like"), left = ACTION_RAIL_LEFT, top = 730),
                    node(contentDescriptionSignals = setOf("comment"), left = ACTION_RAIL_LEFT, top = 1_080),
                    node(contentDescriptionSignals = setOf("share"), left = ACTION_RAIL_LEFT, top = 1_430),
                ),
        )

    private fun falsePositiveWorkflowSnapshot(
        platformCase: PlatformCase,
        surface: FalsePositiveSurface,
    ): AccessibilityTreeSnapshot {
        val nodes =
            mutableListOf(
                rootNode(),
                node(
                    className = "androidx.recyclerview.widget.RecyclerView",
                    viewIdResourceName = surface.viewIdResourceName,
                    contentDescriptionSignals = setOf(surface.signal),
                    isScrollable = true,
                    left = 0,
                    top = 120,
                    right = SCREEN_WIDTH,
                    bottom = SHORT_VIDEO_BOTTOM,
                    width = SCREEN_WIDTH,
                    height = SHORT_VIDEO_BOTTOM - 120,
                    depth = 2,
                ),
            )
        platformCase.primaryActionSignals.forEachIndexed { index, signal ->
            nodes +=
                node(
                    contentDescriptionSignals = setOf(signal),
                    left = ACTION_RAIL_LEFT,
                    top = 730 + index * 350,
                    right = ACTION_RAIL_RIGHT,
                    bottom = 850 + index * 350,
                    depth = 4,
                )
        }
        platformCase.navigationSignals.forEachIndexed { index, signal ->
            nodes +=
                node(
                    contentDescriptionSignals = setOf(signal),
                    left = 40 + index * 260,
                    top = 2_070,
                    right = 160 + index * 260,
                    bottom = 2_160,
                    depth = 3,
                )
        }
        return AccessibilityTreeSnapshot(nodes = nodes)
    }

    private fun rootNode(): AccessibilityNodeSignal =
        node(
            isClickable = false,
            left = 0,
            top = 0,
            right = SCREEN_WIDTH,
            bottom = SCREEN_HEIGHT,
            width = SCREEN_WIDTH,
            height = SCREEN_HEIGHT,
            depth = 0,
        )

    private fun node(
        className: String = "android.view.View",
        viewIdResourceName: String? = null,
        contentDescriptionSignals: Set<String> = emptySet(),
        isClickable: Boolean = true,
        isScrollable: Boolean = false,
        isVisibleToUser: Boolean = true,
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
            isClickable = isClickable,
            isScrollable = isScrollable,
            isVisibleToUser = isVisibleToUser,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            width = width,
            height = height,
            depth = depth,
        )

    private data class PlatformCase(
        val name: String,
        val detector: ShortVideoDetector,
        val packageName: String,
        val shortVideoContainerViewId: String,
        val normalFeedViewId: String,
        val genericVideoViewId: String,
        val explicitSignal: String,
        val localizedExplicitSignal: String = explicitSignal,
        val primaryActionSignals: List<String> = listOf("like", "comment", "share"),
        val alternateActionSignals: List<String> = primaryActionSignals,
        val navigationSignals: List<String> = emptyList(),
        val localizedNavigationSignals: List<String> = navigationSignals,
        val falsePositiveSurfaces: List<FalsePositiveSurface>,
    )

    private data class FalsePositiveSurface(
        val signal: String,
        val viewIdResourceName: String,
    )

    private companion object {
        const val SCREEN_WIDTH = 1_080
        const val SCREEN_HEIGHT = 2_200
        const val SHORT_VIDEO_BOTTOM = 2_140
        const val ACTION_RAIL_LEFT = 930
        const val ACTION_RAIL_RIGHT = 1_050
        const val UNKNOWN_PACKAGE = "com.example.unknown"

        val localizedActionSignals = listOf("me gusta", "comentar", "compartir")

        val platformCases =
            listOf(
                PlatformCase(
                    name = "YouTube Shorts",
                    detector = YouTubeShortsDetector(),
                    packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                    shortVideoContainerViewId = "com.google.android.youtube:id/reel_watch_sequence",
                    normalFeedViewId = "com.google.android.youtube:id/home_recycler",
                    genericVideoViewId = "com.google.android.youtube:id/watch_player",
                    explicitSignal = "shorts",
                    primaryActionSignals = listOf("like", "comments", "share"),
                    alternateActionSignals = listOf("like", "comment", "share"),
                    falsePositiveSurfaces =
                        listOf(
                            FalsePositiveSurface("comments", "com.google.android.youtube:id/comments_panel"),
                            FalsePositiveSurface("search", "com.google.android.youtube:id/search_results"),
                            FalsePositiveSurface("settings", "com.google.android.youtube:id/settings_list"),
                        ),
                ),
                PlatformCase(
                    name = "TikTok main",
                    detector = TikTokShortVideoDetector(),
                    packageName = TikTokShortVideoDetector.TIKTOK_PACKAGE,
                    shortVideoContainerViewId = "com.zhiliaoapp.musically:id/aweme_feed_view_pager",
                    normalFeedViewId = "com.zhiliaoapp.musically:id/home_feed",
                    genericVideoViewId = "com.zhiliaoapp.musically:id/video_player",
                    explicitSignal = "for you",
                    localizedExplicitSignal = "para ti",
                    alternateActionSignals = listOf("save", "more", "follow"),
                    navigationSignals = listOf("home", "friends", "inbox", "profile"),
                    localizedNavigationSignals = listOf("inicio", "amigos", "bandeja", "perfil"),
                    falsePositiveSurfaces =
                        listOf(
                            FalsePositiveSurface("comments", "com.zhiliaoapp.musically:id/comment_panel"),
                            FalsePositiveSurface("search", "com.zhiliaoapp.musically:id/search_results"),
                            FalsePositiveSurface("settings", "com.zhiliaoapp.musically:id/settings_list"),
                            FalsePositiveSurface("inbox", "com.zhiliaoapp.musically:id/inbox_list"),
                            FalsePositiveSurface("profile", "com.zhiliaoapp.musically:id/profile_grid"),
                        ),
                ),
                PlatformCase(
                    name = "Instagram Reels",
                    detector = InstagramReelsDetector(),
                    packageName = InstagramReelsDetector.INSTAGRAM_PACKAGE,
                    shortVideoContainerViewId = "com.instagram.android:id/clips_viewer_viewpager",
                    normalFeedViewId = "com.instagram.android:id/feed_recycler",
                    genericVideoViewId = "com.instagram.android:id/video_player",
                    explicitSignal = "reels",
                    alternateActionSignals = listOf("send", "save", "more"),
                    falsePositiveSurfaces =
                        listOf(
                            FalsePositiveSurface("comments", "com.instagram.android:id/comments_view"),
                            FalsePositiveSurface("search", "com.instagram.android:id/search_results"),
                            FalsePositiveSurface("settings", "com.instagram.android:id/settings_list"),
                            FalsePositiveSurface("direct", "com.instagram.android:id/direct_inbox_recycler"),
                            FalsePositiveSurface("profile", "com.instagram.android:id/profile_grid"),
                        ),
                ),
                PlatformCase(
                    name = "Facebook Reels",
                    detector = FacebookReelsDetector(),
                    packageName = FacebookReelsDetector.FACEBOOK_PACKAGE,
                    shortVideoContainerViewId = "com.facebook.katana:id/reels_viewer",
                    normalFeedViewId = "com.facebook.katana:id/news_feed",
                    genericVideoViewId = "com.facebook.katana:id/video_player",
                    explicitSignal = "reels",
                    alternateActionSignals = listOf("like", "comments", "more"),
                    falsePositiveSurfaces =
                        listOf(
                            FalsePositiveSurface("comments", "com.facebook.katana:id/comments_list"),
                            FalsePositiveSurface("search", "com.facebook.katana:id/search_results"),
                            FalsePositiveSurface("settings", "com.facebook.katana:id/settings_list"),
                            FalsePositiveSurface("messages", "com.facebook.katana:id/messages_list"),
                            FalsePositiveSurface("profile", "com.facebook.katana:id/profile_feed"),
                        ),
                ),
            )
    }
}
