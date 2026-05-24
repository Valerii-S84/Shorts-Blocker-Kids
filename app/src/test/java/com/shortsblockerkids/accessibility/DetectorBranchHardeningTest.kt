package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorBranchHardeningTest {
    @Test
    fun unsupportedAliasesAndNullPackagesAreIgnored() {
        detectorCases.forEach { detectorCase ->
            val snapshot = highConfidenceSnapshot(detectorCase)

            detectorCase.unsupportedPackages.forEach { packageName ->
                assertEquals(
                    detectorCase.name,
                    DetectionResult.None,
                    detectorCase.detector.detect(packageName, snapshot),
                )
            }
            assertEquals(
                detectorCase.name,
                DetectionResult.None,
                detectorCase.detector.detect(null, snapshot),
            )
        }
    }

    @Test
    fun misleadingShortVideoTextOnNonVideoScreensDoesNotBlock() {
        detectorCases.forEach { detectorCase ->
            misleadingScreenLabels.forEach { label ->
                val result =
                    detectorCase.detector.detect(
                        packageName = detectorCase.packageName,
                        snapshot = labeledScreenSnapshot(detectorCase.explicitSignal, label),
                    )

                assertFalse("${detectorCase.name} $label", result.shouldBlock)
            }
        }
    }

    @Test
    fun localeLikeActionTextWithoutVerticalShortVideoStructureDoesNotBlock() {
        detectorCases.forEach { detectorCase ->
            val result =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        horizontalActionSnapshot(
                            identifier = detectorCase.localizedExplicitSignal,
                            actions = detectorCase.localizedActionSignals,
                        ),
                )

            assertFalse(detectorCase.name, result.shouldBlock)
        }
    }

    @Test
    fun zeroSizedAccessibilityTreeDoesNotBlock() {
        detectorCases.forEach { detectorCase ->
            val result =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        AccessibilityTreeSnapshot(
                            nodes =
                                listOf(
                                    node(
                                        viewIdResourceName = detectorCase.containerViewId,
                                        contentDescriptionSignals = setOf(detectorCase.explicitSignal),
                                        right = 0,
                                        bottom = 0,
                                        width = 0,
                                        height = 0,
                                    ),
                                ),
                        ),
                )

            assertFalse(detectorCase.name, result.shouldBlock)
        }
    }

    @Test
    fun actionRailRequiresEnoughVisibleRightSideActionsAndVerticalSpread() {
        detectorCases.forEach { detectorCase ->
            val leftRailResult =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        highConfidenceSnapshot(
                            detectorCase = detectorCase,
                            includeIdentifier = false,
                            includeRepeatedFeed = false,
                            actionLeft = 40,
                        ),
                )
            val compactRailResult =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        highConfidenceSnapshot(
                            detectorCase = detectorCase,
                            includeIdentifier = false,
                            includeRepeatedFeed = false,
                            actionTopSpacing = 40,
                        ),
                )
            val hiddenRailResult =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        highConfidenceSnapshot(
                            detectorCase = detectorCase,
                            includeIdentifier = false,
                            includeRepeatedFeed = false,
                            actionsVisible = false,
                        ),
                )

            assertFalse(detectorCase.name, leftRailResult.shouldBlock)
            assertFalse(detectorCase.name, compactRailResult.shouldBlock)
            assertFalse(detectorCase.name, hiddenRailResult.shouldBlock)
        }
    }

    @Test
    fun verticalFullscreenRequiresVisibleNestedLargeSurface() {
        detectorCases.forEach { detectorCase ->
            val rootSurfaceResult =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        highConfidenceSnapshot(
                            detectorCase = detectorCase,
                            surfaceDepth = 0,
                        ),
                )
            val compactSurfaceResult =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        highConfidenceSnapshot(
                            detectorCase = detectorCase,
                            surfaceWidth = 600,
                            surfaceHeight = 1_100,
                        ),
                )
            val hiddenSurfaceResult =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        highConfidenceSnapshot(
                            detectorCase = detectorCase,
                            surfaceVisible = false,
                        ),
                )

            assertFalse(detectorCase.name, rootSurfaceResult.shouldBlock)
            assertFalse(detectorCase.name, compactSurfaceResult.shouldBlock)
            assertFalse(detectorCase.name, hiddenSurfaceResult.shouldBlock)
        }
    }

    @Test
    fun youtubeBlocksEveryHighConfidenceShortsCombination() {
        val detector = YouTubeShortsDetector()

        val identifierAndReel =
            detector.detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = youtubeCase,
                        includeActionRail = false,
                        includeRepeatedFeed = false,
                    ),
            )
        val identifierActionAndFeed =
            detector.detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = youtubeCase,
                        includeReelContainer = false,
                        surfaceClassName = VIEW_PAGER_CLASS,
                    ),
            )
        val reelAndAction =
            detector.detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = youtubeCase,
                        includeIdentifier = false,
                        includeRepeatedFeed = false,
                    ),
            )

        assertTrue(identifierAndReel.shouldBlock)
        assertTrue(identifierActionAndFeed.shouldBlock)
        assertTrue(reelAndAction.shouldBlock)
    }

    @Test
    fun youtubeStrongShortsEvidenceWithoutFullscreenStaysMedium() {
        val result =
            YouTubeShortsDetector().detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = youtubeCase,
                        includeActionRail = false,
                        includeRepeatedFeed = false,
                        surfaceDepth = 0,
                    ),
            )

        assertFalse(result.shouldBlock)
        assertEquals(Confidence.MEDIUM, result.confidence)
    }

    @Test
    fun reelsDetectorsCanBlockFromIdentifierOrRepeatedFeedContext() {
        listOf(instagramCase, facebookCase).forEach { detectorCase ->
            val identifierContext =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        highConfidenceSnapshot(
                            detectorCase = detectorCase,
                            includeRepeatedFeed = false,
                        ),
                )
            val repeatedFeedContext =
                detectorCase.detector.detect(
                    packageName = detectorCase.packageName,
                    snapshot =
                        highConfidenceSnapshot(
                            detectorCase = detectorCase,
                            includeIdentifier = false,
                        ),
                )

            assertTrue(detectorCase.name, identifierContext.shouldBlock)
            assertTrue(detectorCase.name, repeatedFeedContext.shouldBlock)
        }
    }

    @Test
    fun tiktokCanBlockFromFeedIdentifierOrStructuralNavigationContext() {
        val detector = TikTokShortVideoDetector()

        val feedIdentifierContext =
            detector.detect(
                packageName = TikTokShortVideoDetector.TIKTOK_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = tiktokCase,
                        includeRepeatedFeed = false,
                        navigationSignals = emptyList(),
                    ),
            )
        val structuralNavigationContext =
            detector.detect(
                packageName = TikTokShortVideoDetector.TIKTOK_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = tiktokCase,
                        includeIdentifier = false,
                    ),
            )

        assertTrue(feedIdentifierContext.shouldBlock)
        assertTrue(structuralNavigationContext.shouldBlock)
    }

    @Test
    fun detectorSpecificRequiredContextIsNotInterchangeable() {
        val youtubeWithoutReelOrRepeatedFeed =
            YouTubeShortsDetector().detect(
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = youtubeCase,
                        includeReelContainer = false,
                        includeRepeatedFeed = false,
                        surfaceClassName = VIEW_PAGER_CLASS,
                    ),
            )
        val instagramWithoutIdentifierOrRepeatedFeed =
            InstagramReelsDetector().detect(
                packageName = InstagramReelsDetector.INSTAGRAM_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = instagramCase,
                        includeIdentifier = false,
                        includeRepeatedFeed = false,
                        surfaceViewId = "com.instagram.android:id/viewer",
                    ),
            )
        val facebookWithoutReelContainer =
            FacebookReelsDetector().detect(
                packageName = FacebookReelsDetector.FACEBOOK_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = facebookCase,
                        includeReelContainer = false,
                    ),
            )
        val tiktokWithoutContainer =
            TikTokShortVideoDetector().detect(
                packageName = TikTokShortVideoDetector.TIKTOK_PACKAGE,
                snapshot =
                    highConfidenceSnapshot(
                        detectorCase = tiktokCase,
                        includeReelContainer = false,
                    ),
            )

        assertFalse(youtubeWithoutReelOrRepeatedFeed.shouldBlock)
        assertFalse(instagramWithoutIdentifierOrRepeatedFeed.shouldBlock)
        assertFalse(facebookWithoutReelContainer.shouldBlock)
        assertFalse(tiktokWithoutContainer.shouldBlock)
    }

    private fun highConfidenceSnapshot(
        detectorCase: DetectorCase,
        includeIdentifier: Boolean = true,
        includeReelContainer: Boolean = true,
        includeActionRail: Boolean = true,
        includeRepeatedFeed: Boolean = true,
        navigationSignals: List<String> = detectorCase.navigationSignals,
        actionLeft: Int = ACTION_RAIL_LEFT,
        actionTopSpacing: Int = ACTION_RAIL_SPACING,
        actionsVisible: Boolean = true,
        surfaceClassName: String = detectorCase.surfaceClassName,
        surfaceViewId: String? = null,
        surfaceVisible: Boolean = true,
        surfaceDepth: Int = 2,
        surfaceWidth: Int = SCREEN_WIDTH,
        surfaceHeight: Int = SHORT_VIDEO_HEIGHT,
    ): AccessibilityTreeSnapshot {
        val nodes = mutableListOf(rootNode())
        nodes +=
            node(
                className = surfaceClassName,
                viewIdResourceName =
                    surfaceViewId
                        ?: if (includeReelContainer) {
                            detectorCase.containerViewId
                        } else {
                            detectorCase.genericSurfaceViewId
                        },
                isScrollable = includeRepeatedFeed,
                isVisibleToUser = surfaceVisible,
                left = 0,
                top = 0,
                right = surfaceWidth,
                bottom = surfaceHeight,
                width = surfaceWidth,
                height = surfaceHeight,
                depth = surfaceDepth,
            )
        if (includeIdentifier) {
            nodes += node(contentDescriptionSignals = setOf(detectorCase.explicitSignal), left = 420, top = 80)
        }
        if (includeActionRail) {
            detectorCase.actionSignals.forEachIndexed { index, signal ->
                nodes +=
                    node(
                        contentDescriptionSignals = setOf(signal),
                        isVisibleToUser = actionsVisible,
                        left = actionLeft,
                        top = 760 + index * actionTopSpacing,
                        depth = 4,
                    )
            }
        }
        navigationSignals.forEachIndexed { index, signal ->
            nodes +=
                node(
                    contentDescriptionSignals = setOf(signal),
                    left = 40 + index * 240,
                    top = 2_070,
                    depth = 3,
                )
        }
        return AccessibilityTreeSnapshot(nodes)
    }

    private fun labeledScreenSnapshot(
        shortVideoLabel: String,
        screenLabel: String,
    ): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            listOf(
                rootNode(),
                node(contentDescriptionSignals = setOf(shortVideoLabel), left = 120, top = 240),
                node(contentDescriptionSignals = setOf(screenLabel), left = 420, top = 240),
            ),
        )

    private fun horizontalActionSnapshot(
        identifier: String,
        actions: List<String>,
    ): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            buildList {
                add(rootNode())
                add(node(contentDescriptionSignals = setOf(identifier), left = 120, top = 120))
                actions.forEachIndexed { index, action ->
                    add(
                        node(
                            contentDescriptionSignals = setOf(action),
                            left = 120 + index * 180,
                            top = 1_020,
                        ),
                    )
                }
            },
        )

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

    private data class DetectorCase(
        val name: String,
        val detector: ShortVideoDetector,
        val packageName: String,
        val unsupportedPackages: List<String>,
        val explicitSignal: String,
        val localizedExplicitSignal: String,
        val containerViewId: String,
        val genericSurfaceViewId: String,
        val surfaceClassName: String,
        val actionSignals: List<String>,
        val localizedActionSignals: List<String>,
        val navigationSignals: List<String> = emptyList(),
    )

    private companion object {
        const val SCREEN_WIDTH = 1_080
        const val SCREEN_HEIGHT = 2_200
        const val SHORT_VIDEO_HEIGHT = 2_140
        const val ACTION_RAIL_LEFT = 930
        const val ACTION_RAIL_SPACING = 330
        const val VIEW_PAGER_CLASS = "androidx.viewpager.widget.ViewPager"
        const val FRAME_LAYOUT_CLASS = "android.widget.FrameLayout"

        val misleadingScreenLabels = listOf("feed", "profile", "search", "settings", "groups", "story")

        val youtubeCase =
            DetectorCase(
                name = "YouTube",
                detector = YouTubeShortsDetector(),
                packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
                unsupportedPackages = listOf("com.google.android.youtube.tv", "com.google.android.apps.youtube.music"),
                explicitSignal = "shorts",
                localizedExplicitSignal = "shorts",
                containerViewId = "com.google.android.youtube:id/reel_watch_sequence",
                genericSurfaceViewId = "com.google.android.youtube:id/watch_player",
                surfaceClassName = "android.view.View",
                actionSignals = listOf("like", "comments", "share"),
                localizedActionSignals = listOf("me gusta", "comentar", "compartir"),
            )
        val tiktokCase =
            DetectorCase(
                name = "TikTok",
                detector = TikTokShortVideoDetector(),
                packageName = TikTokShortVideoDetector.TIKTOK_PACKAGE,
                unsupportedPackages = listOf("com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go"),
                explicitSignal = "for you",
                localizedExplicitSignal = "para ti",
                containerViewId = "com.zhiliaoapp.musically:id/aweme_feed_view_pager",
                genericSurfaceViewId = "com.zhiliaoapp.musically:id/regular_surface",
                surfaceClassName = FRAME_LAYOUT_CLASS,
                actionSignals = listOf("like", "comments", "share"),
                localizedActionSignals = listOf("me gusta", "comentar", "compartir"),
                navigationSignals = listOf("home", "friends", "inbox", "profile"),
            )
        val instagramCase =
            DetectorCase(
                name = "Instagram",
                detector = InstagramReelsDetector(),
                packageName = InstagramReelsDetector.INSTAGRAM_PACKAGE,
                unsupportedPackages = listOf("com.instagram.lite", "com.instagram.android.direct"),
                explicitSignal = "reels",
                localizedExplicitSignal = "reels",
                containerViewId = "com.instagram.android:id/clips_viewer_viewpager",
                genericSurfaceViewId = "com.instagram.android:id/main_surface",
                surfaceClassName = FRAME_LAYOUT_CLASS,
                actionSignals = listOf("like", "comments", "share"),
                localizedActionSignals = listOf("me gusta", "comentar", "compartir"),
            )
        val facebookCase =
            DetectorCase(
                name = "Facebook",
                detector = FacebookReelsDetector(),
                packageName = FacebookReelsDetector.FACEBOOK_PACKAGE,
                unsupportedPackages = listOf("com.facebook.lite", "com.facebook.orca"),
                explicitSignal = "reels",
                localizedExplicitSignal = "reels",
                containerViewId = "com.facebook.katana:id/reels_viewer",
                genericSurfaceViewId = "com.facebook.katana:id/main_surface",
                surfaceClassName = FRAME_LAYOUT_CLASS,
                actionSignals = listOf("like", "comments", "share"),
                localizedActionSignals = listOf("me gusta", "comentar", "compartir"),
            )
        val detectorCases = listOf(youtubeCase, tiktokCase, instagramCase, facebookCase)
    }
}
