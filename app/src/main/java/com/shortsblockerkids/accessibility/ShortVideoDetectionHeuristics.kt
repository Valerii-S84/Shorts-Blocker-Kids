package com.shortsblockerkids.accessibility

import java.util.Locale

private const val ACTION_RAIL_MIN_SIGNAL_COUNT = 3
private const val ACTION_RAIL_MIN_RIGHT_SIDE_COUNT = 3
private const val ACTION_RAIL_RIGHT_SIDE_MIN_X_RATIO = 0.62f
private const val DEFAULT_ACTION_RAIL_MIN_VERTICAL_SPREAD_RATIO = 0.22f
private const val FULLSCREEN_MIN_WIDTH_RATIO = 0.72f
private const val FULLSCREEN_MIN_HEIGHT_RATIO = 0.70f
private const val REPEATED_FEED_MIN_WIDTH_RATIO = 0.72f
private const val REPEATED_FEED_MIN_HEIGHT_RATIO = 0.65f

internal const val YOUTUBE_ACTION_RAIL_MIN_VERTICAL_SPREAD_RATIO = 0.25f

internal fun weightedScore(vararg weightedSignals: Pair<Boolean, Int>): Int =
    weightedSignals.sumOf { (matched, points) -> if (matched) points else 0 }

internal fun detectionResult(
    confidence: Confidence,
    matchedSignals: Set<DetectorSignal>,
): DetectionResult =
    DetectionResult(
        isShorts = confidence == Confidence.HIGH,
        confidence = confidence,
        reasons = matchedSignals.map { it.reason },
        matchedSignals = matchedSignals.map { it.id },
    )

internal fun AccessibilityTreeSnapshot.hasActionRailSignal(
    actionSignals: Set<String>,
    matchedSignals: MutableSet<DetectorSignal>,
    minVerticalSpreadRatio: Float = DEFAULT_ACTION_RAIL_MIN_VERTICAL_SPREAD_RATIO,
): Boolean {
    val screenWidth = screenRight
    val screenHeight = screenBottom
    if (screenWidth <= 0 || screenHeight <= 0) {
        return false
    }

    val actionNodes =
        nodes.filter { node ->
            node.isVisibleToUser &&
                node.contentDescriptionSignals.any { it in actionSignals }
        }
    val distinctActionSignals =
        actionNodes
            .flatMap { it.contentDescriptionSignals }
            .filter { it in actionSignals }
            .distinct()
    val rightSideActionCount =
        actionNodes.count { node ->
            node.centerX >= screenWidth * ACTION_RAIL_RIGHT_SIDE_MIN_X_RATIO
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
        distinctActionSignals.size >= ACTION_RAIL_MIN_SIGNAL_COUNT &&
            rightSideActionCount >= ACTION_RAIL_MIN_RIGHT_SIDE_COUNT &&
            verticalSpread >= screenHeight * minVerticalSpreadRatio
    if (matched) {
        matchedSignals += DetectorSignal.ACTION_RAIL
    }

    return matched
}

internal fun AccessibilityTreeSnapshot.hasVerticalFullscreenSignal(
    matchedSignals: MutableSet<DetectorSignal>,
    isSurfaceCandidate: (AccessibilityNodeSignal) -> Boolean,
): Boolean {
    val screenWidth = maxWidth
    val screenHeight = maxHeight
    if (screenWidth <= 0 || screenHeight <= 0) {
        return false
    }

    val matched =
        nodes.any { node ->
            node.isVisibleToUser &&
                node.depth > 0 &&
                isSurfaceCandidate(node) &&
                node.width >= screenWidth * FULLSCREEN_MIN_WIDTH_RATIO &&
                node.height >= screenHeight * FULLSCREEN_MIN_HEIGHT_RATIO
        }

    if (matched) {
        matchedSignals += DetectorSignal.VERTICAL_FULLSCREEN
    }

    return matched
}

internal fun AccessibilityTreeSnapshot.hasRepeatedVerticalFeedSignal(
    matchedSignals: MutableSet<DetectorSignal>,
    isFeedCandidate: (AccessibilityNodeSignal) -> Boolean = { true },
): Boolean {
    val screenWidth = maxWidth
    val screenHeight = maxHeight
    if (screenWidth <= 0 || screenHeight <= 0) {
        return false
    }

    val matched =
        nodes.any { node ->
            node.isVisibleToUser &&
                node.isScrollable &&
                isFeedCandidate(node) &&
                node.width >= screenWidth * REPEATED_FEED_MIN_WIDTH_RATIO &&
                node.height >= screenHeight * REPEATED_FEED_MIN_HEIGHT_RATIO
        }

    if (matched) {
        matchedSignals += DetectorSignal.REPEATED_VERTICAL_FEED
    }

    return matched
}

internal fun String?.containsAnyIgnoringCase(vararg needles: String): Boolean {
    val normalized = this?.lowercase(Locale.US) ?: return false
    return needles.any { normalized.contains(it) }
}
