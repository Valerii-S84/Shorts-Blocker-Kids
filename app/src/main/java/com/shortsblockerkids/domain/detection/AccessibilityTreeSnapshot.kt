package com.shortsblockerkids.domain.detection

data class AccessibilityTreeSnapshot(
    val nodes: List<AccessibilityNodeSignal>,
) {
    val visibleNodeCount: Int
        get() = nodes.count { it.isVisibleToUser }

    val maxWidth: Int
        get() = visibleGeometryNodes.maxOfOrNull { it.width } ?: 0

    val maxHeight: Int
        get() = visibleGeometryNodes.maxOfOrNull { it.height } ?: 0

    private val visibleGeometryNodes: Sequence<AccessibilityNodeSignal>
        get() = nodes.asSequence().filter { it.isVisibleWithArea }

    val screenLeft: Int
        get() = visibleGeometryNodes.minOfOrNull { it.left } ?: 0

    val screenTop: Int
        get() = visibleGeometryNodes.minOfOrNull { it.top } ?: 0

    val screenRight: Int
        get() = visibleGeometryNodes.maxOfOrNull { it.right } ?: 0

    val screenBottom: Int
        get() = visibleGeometryNodes.maxOfOrNull { it.bottom } ?: 0

    val screenWidth: Int
        get() = screenRight - screenLeft

    val screenHeight: Int
        get() = screenBottom - screenTop

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
    internal val isVisibleWithArea: Boolean
        get() = isVisibleToUser && right > left && bottom > top && width > 0 && height > 0

    val centerX: Int
        get() = left + width / 2

    val centerY: Int
        get() = top + height / 2
}
