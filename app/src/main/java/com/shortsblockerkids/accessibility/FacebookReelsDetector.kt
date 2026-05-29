package com.shortsblockerkids.accessibility

class FacebookReelsDetector : ShortVideoDetector {
    override val platform: SupportedPlatform = SupportedPlatform.FACEBOOK_REELS
    override val supportedPackages: Set<String> =
        setOf(FACEBOOK_PACKAGE) + DebugFixturePackages.enabled(DebugFixturePackages.FACEBOOK)

    override fun detect(
        packageName: String?,
        snapshot: AccessibilityTreeSnapshot,
    ): DetectionResult {
        if (packageName !in supportedPackages) {
            return DetectionResult.None
        }

        val matchedSignals = linkedSetOf<DetectorSignal>()
        val hasReelsIdentifier = snapshot.hasReelsIdentifier(matchedSignals)
        val hasReelContainer = snapshot.hasReelContainer(matchedSignals)
        val hasActionRail = snapshot.hasActionRail(matchedSignals)
        val hasVerticalFullscreen = snapshot.hasVerticalFullscreen(matchedSignals)
        val hasRepeatedVerticalFeed = snapshot.hasRepeatedVerticalFeed(matchedSignals)

        val score =
            weightedScore(
                hasReelsIdentifier to 2,
                hasReelContainer to 3,
                hasActionRail to 3,
                hasVerticalFullscreen to 1,
                hasRepeatedVerticalFeed to 1,
            )

        val confidence =
            when {
                isHighConfidenceReels(
                    hasReelsIdentifier = hasReelsIdentifier,
                    hasActionRail = hasActionRail,
                    hasVerticalFullscreen = hasVerticalFullscreen,
                    hasRepeatedVerticalFeed = hasRepeatedVerticalFeed,
                ) -> {
                    Confidence.HIGH
                }
                score >= 5 && hasReelsIdentifier -> Confidence.MEDIUM
                score > 0 -> Confidence.LOW
                else -> Confidence.NONE
            }

        return detectionResult(confidence, matchedSignals)
    }

    private fun AccessibilityTreeSnapshot.hasReelsIdentifier(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.contentDescriptionSignals.any { it in ShortVideoTextSignals.reelsIdentifiers } ||
                    node.viewIdResourceName.containsAnyIgnoringCase("reel", "reels")
            }

        if (matched) {
            matchedSignals += DetectorSignal.SHORT_VIDEO_IDENTIFIER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasReelContainer(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.viewIdResourceName.containsAnyIgnoringCase(
                    "reel",
                    "reels",
                    "reel_viewer",
                    "video",
                    "player",
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
            actionSignals = ShortVideoTextSignals.facebookActions,
            matchedSignals = matchedSignals,
        )

    private fun AccessibilityTreeSnapshot.hasVerticalFullscreen(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasVerticalFullscreenSignal(matchedSignals) { it.isFacebookSurfaceCandidate() }

    private fun AccessibilityTreeSnapshot.hasRepeatedVerticalFeed(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasRepeatedVerticalFeedSignal(matchedSignals) { !it.viewIdResourceName.isNonReelsWorkflowId() }

    private fun isHighConfidenceReels(
        hasReelsIdentifier: Boolean,
        hasActionRail: Boolean,
        hasVerticalFullscreen: Boolean,
        hasRepeatedVerticalFeed: Boolean,
    ): Boolean {
        if (!hasActionRail || !hasVerticalFullscreen) {
            return false
        }

        return hasReelsIdentifier || hasRepeatedVerticalFeed
    }

    private fun String?.isNonReelsWorkflowId(): Boolean =
        containsAnyIgnoringCase(
            "news_feed",
            "home_feed",
            "comments",
            "comment",
            "search",
            "settings",
            "messages",
            "message",
            "profile",
            "groups",
            "group",
            "marketplace",
        )

    private fun AccessibilityNodeSignal.isFacebookSurfaceCandidate(): Boolean =
        isScrollable ||
            viewIdResourceName.containsAnyIgnoringCase("reel", "reels", "player", "video") ||
            className.containsAnyIgnoringCase("viewpager", "framelayout")

    companion object {
        const val FACEBOOK_PACKAGE = "com.facebook.katana"
    }
}
