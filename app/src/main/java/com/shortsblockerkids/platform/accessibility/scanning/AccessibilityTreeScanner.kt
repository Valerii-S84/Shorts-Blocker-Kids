package com.shortsblockerkids.platform.accessibility.scanning

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.shortsblockerkids.domain.detection.AccessibilityNodeSignal
import com.shortsblockerkids.domain.detection.AccessibilityTreeSnapshot
import com.shortsblockerkids.domain.detection.ShortVideoTextSignals
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

    @Suppress("DEPRECATION")
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
            try {
                collectNodeSignals(child, depth + 1, nodes)
            } finally {
                child.recycle()
            }
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

        return ShortVideoTextSignals.knownContentSignals
            .filter { signal -> normalized.contains(signal) }
            .toSet()
    }

    companion object {
        private const val MAX_DEPTH = 12
        private const val MAX_NODES = 180
    }
}
