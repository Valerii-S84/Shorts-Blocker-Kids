package com.shortsblockerkids.accessibility

import java.util.Locale

class FacebookReelsDetector : ShortVideoDetector {
    override val platform: SupportedPlatform = SupportedPlatform.FACEBOOK_REELS
    override val supportedPackages: Set<String> = setOf(FACEBOOK_PACKAGE)

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
            listOf(
                hasReelsIdentifier to 2,
                hasReelContainer to 3,
                hasActionRail to 3,
                hasVerticalFullscreen to 1,
                hasRepeatedVerticalFeed to 1,
            ).sumOf { (matched, points) -> if (matched) points else 0 }

        val confidence =
            when {
                hasVerticalFullscreen && hasActionRail && (hasReelsIdentifier || hasReelContainer) -> {
                    Confidence.HIGH
                }
                score >= 5 && hasReelsIdentifier -> Confidence.MEDIUM
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

    private fun AccessibilityTreeSnapshot.hasReelsIdentifier(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.contentDescriptionSignals.any { it in reelsSignals } ||
                    node.viewIdResourceName.containsAny("reel", "reels")
            }

        if (matched) {
            matchedSignals += DetectorSignal.SHORT_VIDEO_IDENTIFIER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasReelContainer(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.viewIdResourceName.containsAny("reel", "reels", "reel_viewer", "video", "player") ||
                    node.className.containsAny("viewpager")
            }

        if (matched) {
            matchedSignals += DetectorSignal.REEL_CONTAINER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasActionRail(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val screenWidth = screenRight
        val screenHeight = screenBottom
        if (screenWidth <= 0 || screenHeight <= 0) {
            return false
        }

        val actionNodes =
            nodes.filter { node ->
                node.isVisibleToUser &&
                    node.contentDescriptionSignals.any { it in facebookActionSignals }
            }
        val actionSignals =
            actionNodes
                .flatMap { it.contentDescriptionSignals }
                .filter { it in facebookActionSignals }
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
                verticalSpread >= screenHeight * 0.22f
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
                    node.depth > 0 &&
                    node.isFacebookSurfaceCandidate() &&
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
                    node.height >= screenHeight * 0.65f
            }

        if (matched) {
            matchedSignals += DetectorSignal.REPEATED_VERTICAL_FEED
        }

        return matched
    }

    private fun String?.containsAny(vararg needles: String): Boolean {
        val normalized = this?.lowercase(Locale.US) ?: return false
        return needles.any { normalized.contains(it) }
    }

    private fun AccessibilityNodeSignal.isFacebookSurfaceCandidate(): Boolean =
        isScrollable ||
            viewIdResourceName.containsAny("reel", "reels", "player", "video") ||
            className.containsAny("viewpager", "framelayout")

    companion object {
        const val FACEBOOK_PACKAGE = "com.facebook.katana"

        private val reelsSignals = setOf("reel", "reels")
        private val facebookActionSignals = setOf("like", "comments", "comment", "share", "more")
    }
}
