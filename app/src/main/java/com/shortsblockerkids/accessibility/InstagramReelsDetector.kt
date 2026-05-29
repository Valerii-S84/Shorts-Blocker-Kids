package com.shortsblockerkids.accessibility

class InstagramReelsDetector : ShortVideoDetector {
    override val platform: SupportedPlatform = SupportedPlatform.INSTAGRAM_REELS
    override val supportedPackages: Set<String> =
        setOf(INSTAGRAM_PACKAGE) + DebugFixturePackages.enabled(DebugFixturePackages.INSTAGRAM)

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
                    hasReelContainer = hasReelContainer,
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
                    node.viewIdResourceName.containsAnyIgnoringCase("reel", "reels", "clips")
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
                    "clips",
                    "clips_viewer",
                    "reel",
                    "reels",
                    "reel_viewer",
                    "viewer",
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
            actionSignals = ShortVideoTextSignals.instagramActions,
            matchedSignals = matchedSignals,
        )

    private fun AccessibilityTreeSnapshot.hasVerticalFullscreen(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasVerticalFullscreenSignal(matchedSignals) { it.isInstagramSurfaceCandidate() }

    private fun AccessibilityTreeSnapshot.hasRepeatedVerticalFeed(matchedSignals: MutableSet<DetectorSignal>): Boolean =
        hasRepeatedVerticalFeedSignal(matchedSignals)

    private fun isHighConfidenceReels(
        hasReelsIdentifier: Boolean,
        hasReelContainer: Boolean,
        hasActionRail: Boolean,
        hasVerticalFullscreen: Boolean,
        hasRepeatedVerticalFeed: Boolean,
    ): Boolean {
        if (!hasActionRail || !hasVerticalFullscreen) {
            return false
        }

        return hasReelsIdentifier || hasReelContainer && hasRepeatedVerticalFeed
    }

    private fun AccessibilityNodeSignal.isInstagramSurfaceCandidate(): Boolean =
        isScrollable ||
            viewIdResourceName.containsAnyIgnoringCase("clips", "reel", "reels", "player", "video") ||
            className.containsAnyIgnoringCase("viewpager", "framelayout")

    companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
    }
}
