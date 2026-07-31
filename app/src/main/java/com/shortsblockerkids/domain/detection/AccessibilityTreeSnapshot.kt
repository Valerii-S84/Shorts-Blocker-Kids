package com.shortsblockerkids.domain.detection

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
