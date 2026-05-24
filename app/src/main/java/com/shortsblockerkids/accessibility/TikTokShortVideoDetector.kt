package com.shortsblockerkids.accessibility

import java.util.Locale

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
            listOf(
                hasFeedIdentifier to 2,
                hasShortVideoContainer to 2,
                hasActionRail to 3,
                hasVerticalFullscreen to 1,
                hasRepeatedVerticalFeed to 1,
                hasNavigationContext to 1,
            ).sumOf { (matched, points) -> if (matched) points else 0 }

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

        return DetectionResult(
            isShorts = confidence == Confidence.HIGH,
            confidence = confidence,
            reasons = matchedSignals.map { it.reason },
            matchedSignals = matchedSignals.map { it.id },
        )
    }

    private fun AccessibilityTreeSnapshot.hasFeedIdentifier(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.contentDescriptionSignals.any { it in tiktokFeedSignals } ||
                    node.viewIdResourceName.containsAny("for_you", "foryou", "aweme", "feed")
            }

        if (matched) {
            matchedSignals += DetectorSignal.SHORT_VIDEO_IDENTIFIER
        }

        return matched
    }

    private fun AccessibilityTreeSnapshot.hasShortVideoContainer(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val matched =
            nodes.any { node ->
                node.viewIdResourceName.containsAny("aweme", "feed", "player", "video", "viewpager") ||
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
                    node.contentDescriptionSignals.any { it in tiktokActionSignals }
            }
        val actionSignals =
            actionNodes
                .flatMap { it.contentDescriptionSignals }
                .filter { it in tiktokActionSignals }
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
                    node.isTikTokSurfaceCandidate() &&
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

    private fun AccessibilityTreeSnapshot.hasNavigationContext(matchedSignals: MutableSet<DetectorSignal>): Boolean {
        val signalCount =
            nodes
                .flatMap { it.contentDescriptionSignals }
                .filter { it in tiktokNavigationSignals }
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

    private fun String?.containsAny(vararg needles: String): Boolean {
        val normalized = this?.lowercase(Locale.US) ?: return false
        return needles.any { normalized.contains(it) }
    }

    private fun AccessibilityNodeSignal.isTikTokSurfaceCandidate(): Boolean =
        isScrollable ||
            viewIdResourceName.containsAny("aweme", "feed", "player", "video", "viewpager") ||
            className.containsAny("viewpager", "framelayout")

    companion object {
        const val TIKTOK_PACKAGE = "com.zhiliaoapp.musically"

        private val tiktokFeedSignals = setOf("for you", "para ti", "following", "siguiendo")
        private val tiktokNavigationSignals =
            setOf("home", "inicio", "friends", "amigos", "inbox", "bandeja", "profile", "perfil")
        private val tiktokActionSignals =
            setOf(
                "like",
                "me gusta",
                "comments",
                "comment",
                "comentar",
                "share",
                "compartir",
                "save",
                "guardar",
                "more",
                "follow",
                "seguir",
            )
    }
}
