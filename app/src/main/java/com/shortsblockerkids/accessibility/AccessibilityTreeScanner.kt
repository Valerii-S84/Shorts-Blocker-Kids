package com.shortsblockerkids.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class AccessibilityTreeScanner {
    fun scan(root: AccessibilityNodeInfo?): AccessibilityTreeSnapshot {
        if (root == null) {
            return AccessibilityTreeSnapshot.Empty
        }

        val nodes = mutableListOf<AccessibilityNodeSignal>()
        collectNodeSignals(root, depth = 0, nodes = nodes)
        return AccessibilityTreeSnapshot(nodes = nodes)
    }

    private fun collectNodeSignals(
        node: AccessibilityNodeInfo,
        depth: Int,
        nodes: MutableList<AccessibilityNodeSignal>,
    ) {
        if (depth > MAX_DEPTH || nodes.size >= MAX_NODES) {
            return
        }

        nodes += node.toSignal(depth)

        for (index in 0 until node.childCount) {
            if (nodes.size >= MAX_NODES) {
                return
            }

            val child = node.getChild(index) ?: continue
            collectNodeSignals(child, depth + 1, nodes)
        }
    }

    private fun AccessibilityNodeInfo.toSignal(depth: Int): AccessibilityNodeSignal {
        val bounds = Rect()
        getBoundsInScreen(bounds)
        return AccessibilityNodeSignal(
            className = className?.toString().orEmpty(),
            viewIdResourceName = viewIdResourceName,
            contentDescriptionSignals = contentDescription.extractKnownSignals(),
            isClickable = isClickable,
            isScrollable = isScrollable,
            isVisibleToUser = isVisibleToUser,
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
            width = bounds.width().coerceAtLeast(0),
            height = bounds.height().coerceAtLeast(0),
            depth = depth,
        )
    }

    private fun CharSequence?.extractKnownSignals(): Set<String> {
        val normalized =
            this
                ?.toString()
                ?.lowercase(Locale.US)
                ?: return emptySet()

        return knownContentSignals
            .filter { signal -> normalized.contains(signal) }
            .toSet()
    }

    companion object {
        private const val MAX_DEPTH = 12
        private const val MAX_NODES = 180

        private val knownContentSignals =
            setOf(
                "shorts",
                "like",
                "dislike",
                "comment",
                "comments",
                "share",
                "remix",
                "subscribe",
                "home",
                "subscriptions",
            )
    }
}

data class AccessibilityTreeSnapshot(
    val nodes: List<AccessibilityNodeSignal>,
) {
    val visibleNodeCount: Int
        get() = nodes.count { it.isVisibleToUser }

    val maxWidth: Int
        get() = nodes.maxOfOrNull { it.width } ?: 0

    val maxHeight: Int
        get() = nodes.maxOfOrNull { it.height } ?: 0

    val screenRight: Int
        get() = nodes.maxOfOrNull { it.right } ?: 0

    val screenBottom: Int
        get() = nodes.maxOfOrNull { it.bottom } ?: 0

    fun toDebugSummary(): String {
        val classNames =
            nodes
                .asSequence()
                .map { it.className.substringAfterLast('.') }
                .filter { it.isNotBlank() }
                .distinct()
                .take(8)
                .joinToString()

        val viewIds =
            nodes
                .asSequence()
                .mapNotNull { it.viewIdResourceName?.substringAfterLast('/') }
                .distinct()
                .take(8)
                .joinToString()

        val contentSignals =
            nodes
                .flatMap { it.contentDescriptionSignals }
                .distinct()
                .take(8)
                .joinToString()

        return "visible=$visibleNodeCount classes=[$classNames] ids=[$viewIds] signals=[$contentSignals]"
    }

    companion object {
        val Empty = AccessibilityTreeSnapshot(emptyList())
    }
}

data class AccessibilityNodeSignal(
    val className: String,
    val viewIdResourceName: String?,
    val contentDescriptionSignals: Set<String>,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isVisibleToUser: Boolean,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val width: Int,
    val height: Int,
    val depth: Int,
) {
    val centerX: Int
        get() = left + width / 2

    val centerY: Int
        get() = top + height / 2
}
