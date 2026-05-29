package com.shortsblockerkids.accessibility

class TikTokShortVideoDetector : ShortVideoDetector {
    override val platform: SupportedPlatform = SupportedPlatform.TIKTOK
    override val supportedPackages: Set<String> =
        setOf(TIKTOK_PACKAGE) + DebugFixturePackages.enabled(DebugFixturePackages.TIKTOK)

    override fun detect(
        packageName: String?,
        snapshot: AccessibilityTreeSnapshot,
    ): DetectionResult {
        if (packageName !in supportedPackages) {
            return DetectionResult.None
        }

        val matchedSignals = linkedSetOf<DetectorSignal>()
        val hasFeedIdentifier = snapshot.hasFeedIdentifier(matchedSignals)
        val hasShortVideoContainer = snapshot.hasShortVideoContainer(matchedSignals)
        val hasActionRail = snapshot.hasActionRail(matchedSignals)
        val hasVerticalFullscreen = snapshot.hasVerticalFullscreen(matchedSignals)
        val hasRepeatedVerticalFeed = snapshot.hasRepeatedVerticalFeed(matchedSignals)
        val hasNavigationContext = snapshot.hasNavigationContext(matchedSignals)

        val score =
            weightedScore(
                hasFeedIdentifier to 2,
                hasShortVideoContainer to 2,
                hasActionRail to 3,
                hasVerticalFullscreen to 1,
                hasRepeatedVerticalFeed to 1,
                hasNavigationContext to 1,
            )

        val confidence =
            when {
                isHighConfidenceFeed(
                    hasFeedIdentifier = hasFeedIdentifier,
                    hasShortVideoContainer = hasShortVideoContainer,
                    hasActionRail = hasActionRail,
                    hasVerticalFullscreen = hasVerticalFullscreen,
                    hasRepeatedVerticalFeed = hasRepeatedVerticalFeed,
                    hasNavigationContext = hasNavigationContext,
                ) -> Confidence.HIGH
                score >= 5 && hasActionRail && hasVerticalFullscreen -> Confidence.MEDIUM
                score > 0 -> Confidence.LOW
                else -> Confidence.NONE
            }

        return detectionResult(confidence, matchedSignals)
    }

    private fun AccessibilityTreeSnapshot.hasFeedIdentifier(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.contentDescriptionSignals.any { it in ShortVideoTextSignals.tiktokFeed } ||
                    node.viewIdResourceName.containsAnyIgnoringCase("for_you", "foryou", "aweme", "feed")
            }

        if (matched) {
            matchedSignals += DetectorSignal.SHORT_VIDEO_IDENTIFIER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasShortVideoContainer(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.viewIdResourceName.containsAnyIgnoringCase(
                    "aweme",
                    "feed",
                    "player",
                    "video",
                    "viewpager",
                ) ||
                    node.className.containsAnyIgnoringCase("viewpager")
            }

        if (matched) {
            matchedSignals += DetectorSignal.REEL_CONTAINER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasActionRail(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasActionRailSignal(
            actionSignals = ShortVideoTextSignals.tiktokActions,
            matchedSignals = matchedSignals,
        )

    private fun AccessibilityTreeSnapshot.hasVerticalFullscreen(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasVerticalFullscreenSignal(matchedSignals) { it.isTikTokSurfaceCandidate() }

    private fun AccessibilityTreeSnapshot.hasRepeatedVerticalFeed(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasRepeatedVerticalFeedSignal(matchedSignals)

    private fun AccessibilityTreeSnapshot.hasNavigationContext(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val signalCount =
            nodes
                .flatMap { it.contentDescriptionSignals }
                .filter { it in ShortVideoTextSignals.tiktokNavigation }
                .distinct()
                .size
        val matched = signalCount >= 3

        if (matched) {
            matchedSignals += DetectorSignal.PLATFORM_NAVIGATION
        }

        return matched
    }

    private fun isHighConfidenceFeed(
        hasFeedIdentifier: Boolean,
        hasShortVideoContainer: Boolean,
        hasActionRail: Boolean,
        hasVerticalFullscreen: Boolean,
        hasRepeatedVerticalFeed: Boolean,
        hasNavigationContext: Boolean,
    ): Boolean {
        if (!hasActionRail || !hasVerticalFullscreen) {
            return false
        }

        if (!hasShortVideoContainer) {
            return false
        }

        val hasStructuralFeedContext = hasNavigationContext && hasRepeatedVerticalFeed
        return hasFeedIdentifier || hasStructuralFeedContext
    }

    private fun AccessibilityNodeSignal.isTikTokSurfaceCandidate(): Boolean =
        isScrollable ||
            viewIdResourceName.containsAnyIgnoringCase(
                "aweme",
                "feed",
                "player",
                "video",
                "viewpager",
            ) ||
            className.containsAnyIgnoringCase("viewpager", "framelayout")

    companion object {
        const val TIKTOK_PACKAGE = "com.zhiliaoapp.musically"
    }
}
