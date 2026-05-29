package com.shortsblockerkids.accessibility

class YouTubeShortsDetector : ShortVideoDetector {
    override val platform: SupportedPlatform = SupportedPlatform.YOUTUBE_SHORTS
    override val supportedPackages: Set<String> =
        setOf(YOUTUBE_PACKAGE) + DebugFixturePackages.enabled(DebugFixturePackages.YOUTUBE)

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
                isHighConfidenceShorts(
                    hasShortsIdentifier = hasShortsIdentifier,
                    hasReelContainer = hasReelContainer,
                    hasActionRail = hasActionRail,
                    hasVerticalFullscreen = hasVerticalFullscreen,
                    hasRepeatedVerticalFeed = hasRepeatedVerticalFeed,
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
                node.contentDescriptionSignals.any { it in ShortVideoTextSignals.shortsIdentifiers } ||
                    node.viewIdResourceName.containsAnyIgnoringCase("shorts")
            }

        if (matched) {
            matchedSignals += DetectorSignal.SHORTS_IDENTIFIER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasReelContainer(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.viewIdResourceName.containsAnyIgnoringCase(
                    "reel",
                    "reel_watch_sequence",
                    "reel_player_page",
                    "shorts_player",
                    "shorts_watch",
                    "shorts_video",
                    "shorts_container",
                )
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
        hasShortsIdentifier: Boolean,
        hasReelContainer: Boolean,
        hasActionRail: Boolean,
        hasVerticalFullscreen: Boolean,
        hasRepeatedVerticalFeed: Boolean,
    ): Boolean {
        if (!hasVerticalFullscreen) {
            return false
        }

        return hasReelContainer &&
            hasActionRail ||
            hasShortsIdentifier &&
            hasReelContainer ||
            hasShortsIdentifier &&
            hasActionRail &&
            hasRepeatedVerticalFeed
    }

    private fun AccessibilityNodeSignal.isShortsSurfaceCandidate(): Boolean =
        viewIdResourceName.containsAnyIgnoringCase("reel", "shorts") ||
            className.containsAnyIgnoringCase("viewpager")

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
