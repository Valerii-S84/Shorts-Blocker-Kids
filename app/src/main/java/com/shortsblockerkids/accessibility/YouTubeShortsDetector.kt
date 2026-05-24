package com.shortsblockerkids.accessibility

import java.util.Locale

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
            listOf(
                hasShortsIdentifier to 2,
                hasReelContainer to 3,
                hasActionRail to 2,
                hasVerticalFullscreen to 1,
                hasRepeatedVerticalFeed to 1,
            ).sumOf { (matched, points) -> if (matched) points else 0 }

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

        return DetectionResult(
            isShorts = confidence == Confidence.HIGH,
            confidence = confidence,
            reasons = matchedSignals.map { it.reason },
            matchedSignals = matchedSignals.map { it.id },
        )
    }

    private fun AccessibilityTreeSnapshot.hasShortsIdentifier(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.contentDescriptionSignals.contains("shorts") ||
                    node.viewIdResourceName.containsAny("shorts")
            }

        if (matched) {
            matchedSignals += DetectorSignal.SHORTS_IDENTIFIER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasReelContainer(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.viewIdResourceName.containsAny(
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

    private fun AccessibilityTreeSnapshot.hasShortsActionRail(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val screenWidth = screenRight
        val screenHeight = screenBottom
        if (screenWidth <= 0 || screenHeight <= 0) {
            return false
        }

        val actionNodes =
            nodes.filter { node ->
                node.isVisibleToUser &&
                    node.contentDescriptionSignals.any { it in shortsActionSignals }
            }

        val actionSignals =
            actionNodes
                .flatMap { it.contentDescriptionSignals }
                .filter { it in shortsActionSignals }
                .distinct()

        val rightSideActionCount =
            actionNodes.count { node ->
                node.centerX >= screenWidth * 0.62f
            }

        val verticalSpread =
            actionNodes
                .map { it.centerY }
                .let { centers ->
                    val top = centers.minOrNull() ?: 0
                    val bottom = centers.maxOrNull() ?: 0
                    bottom - top
                }

        val matched =
            actionSignals.size >= 3 &&
                rightSideActionCount >= 3 &&
                verticalSpread >= screenHeight * 0.25f
        if (matched) {
            matchedSignals += DetectorSignal.ACTION_RAIL
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasVerticalFullscreen(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        if (screenWidth <= 0 || screenHeight <= 0) {
            return false
        }

        val matched =
            nodes.any { node ->
                node.isVisibleToUser &&
                    node.isShortsSurfaceCandidate() &&
                    node.depth > 0 &&
                    node.width >= screenWidth * 0.72f &&
                    node.height >= screenHeight * 0.70f
            }

        if (matched) {
            matchedSignals += DetectorSignal.VERTICAL_FULLSCREEN
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasRepeatedVerticalFeed(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        if (screenWidth <= 0 || screenHeight <= 0) {
            return false
        }

        val matched =
            nodes.any { node ->
                node.isVisibleToUser &&
                    node.isScrollable &&
                    node.width >= screenWidth * 0.72f &&
                    node.height >= screenHeight * 0.65f &&
                    node.isShortsSurfaceCandidate()
            }

        if (matched) {
            matchedSignals += DetectorSignal.REPEATED_VERTICAL_FEED
        }

        return matched
    }

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

    private fun String?.containsAny(vararg needles: String): Boolean {
        val normalized = this?.lowercase(Locale.US) ?: return false
        return needles.any { normalized.contains(it) }
    }

    private fun AccessibilityNodeSignal.isShortsSurfaceCandidate(): Boolean =
        viewIdResourceName.containsAny("reel", "shorts") ||
            className.containsAny("viewpager")

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"

        private val shortsActionSignals =
            setOf(
                "like",
                "me gusta",
                "dislike",
                "comment",
                "comments",
                "comentar",
                "share",
                "compartir",
                "remix",
                "subscribe",
            )
    }
}
