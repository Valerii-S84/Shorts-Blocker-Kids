package com.shortsblockerkids.domain.detection

class YouTubeShortsDetector(
    packageAliases: Set<String> = emptySet(),
) : ShortVideoDetector {
    override val platform: SupportedPlatform = SupportedPlatform.YOUTUBE_SHORTS
    override val supportedPackages: Set<String> =
        setOf(YOUTUBE_PACKAGE) + packageAliases

    override fun detect(
        packageName: String?,
        snapshot: AccessibilityTreeSnapshot,
    ): DetectionResult {
        if (packageName !in supportedPackages) {
            return DetectionResult.None
        }

        val matchedSignals = linkedSetOf<DetectorSignal>()
        val hasShortsIdentifier = snapshot.hasShortsIdentifier(matchedSignals)
        val hasReelContainer = snapshot.hasReelContainer(matchedSignals)
        val hasActionRail = snapshot.hasShortsActionRail(matchedSignals)
        val hasVerticalFullscreen = snapshot.hasVerticalFullscreen(matchedSignals)
        val hasRepeatedVerticalFeed = snapshot.hasRepeatedVerticalFeed(matchedSignals)
        val hasDedicatedPlayer =
            snapshot.hasVerticalFullscreenSignal(matchedSignals) {
                it.viewIdResourceName?.substringAfterLast('/') in DEDICATED_PLAYER_VIEW_IDS
            }

        val score =
            weightedScore(
                hasShortsIdentifier to 2,
                hasReelContainer to 3,
                hasActionRail to 2,
                hasVerticalFullscreen to 1,
                hasRepeatedVerticalFeed to 1,
            )

        val confidence =
            when {
                hasDedicatedPlayer -> Confidence.HIGH
                isHighConfidenceShorts(
                    hasReelContainer = hasReelContainer,
                    hasActionRail = hasActionRail,
                    hasVerticalFullscreen = hasVerticalFullscreen,
                ) -> Confidence.HIGH
                score >= 4 && hasShortsIdentifier -> Confidence.MEDIUM
                score > 0 -> Confidence.LOW
                else -> Confidence.NONE
            }

        return detectionResult(confidence, matchedSignals)
    }

    private fun AccessibilityTreeSnapshot.hasShortsIdentifier(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.isVisibleToUser &&
                    (
                        node.contentDescriptionSignals.any { it in ShortVideoTextSignals.shortsIdentifiers } ||
                            node.viewIdResourceName.containsAnyIgnoringCase("shorts")
                    )
            }

        if (matched) {
            matchedSignals += DetectorSignal.SHORTS_IDENTIFIER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasReelContainer(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.isVisibleToUser && node.isShortsContainerCandidate()
            }

        if (matched) {
            matchedSignals += DetectorSignal.REEL_CONTAINER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasShortsActionRail(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasActionRailSignal(
            actionSignals = ShortVideoTextSignals.youtubeActions,
            matchedSignals = matchedSignals,
            minVerticalSpreadRatio = YOUTUBE_ACTION_RAIL_MIN_VERTICAL_SPREAD_RATIO,
        )

    private fun AccessibilityTreeSnapshot.hasVerticalFullscreen(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasVerticalFullscreenSignal(matchedSignals) { it.isShortsSurfaceCandidate() }

    private fun AccessibilityTreeSnapshot.hasRepeatedVerticalFeed(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasRepeatedVerticalFeedSignal(matchedSignals) { it.isShortsSurfaceCandidate() }

    private fun isHighConfidenceShorts(
        hasReelContainer: Boolean,
        hasActionRail: Boolean,
        hasVerticalFullscreen: Boolean,
    ): Boolean {
        if (!hasVerticalFullscreen) {
            return false
        }

        return hasReelContainer && hasActionRail
    }

    private fun AccessibilityNodeSignal.isShortsSurfaceCandidate(): Boolean =
        isShortsContainerCandidate() ||
            className.containsAnyIgnoringCase("viewpager")

    private fun AccessibilityNodeSignal.isShortsContainerCandidate(): Boolean =
        viewIdResourceName?.substringAfterLast('/') in PLAYER_VIEW_IDS ||
            viewIdResourceName.containsAnyIgnoringCase("reel") ||
            isLargeShortsViewId()

    private fun AccessibilityNodeSignal.isLargeShortsViewId(): Boolean =
        viewIdResourceName.containsAnyIgnoringCase("shorts") &&
            width >= YOUTUBE_MIN_SURFACE_WIDTH &&
            height >= YOUTUBE_MIN_SURFACE_HEIGHT

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private val PLAYER_VIEW_IDS =
            setOf(
                "reel_watch_sequence",
                "reel_player_page",
                "reel_player_view",
                "shorts_player",
                "shorts_watch",
                "shorts_video",
                "shorts_container",
            )
        private val DEDICATED_PLAYER_VIEW_IDS = PLAYER_VIEW_IDS - "shorts_container"
        private const val YOUTUBE_MIN_SURFACE_WIDTH = 720
        private const val YOUTUBE_MIN_SURFACE_HEIGHT = 1200
    }
}
