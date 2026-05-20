package com.shortsblockerkids.accessibility

import com.shortsblockerkids.BuildConfig
import java.io.File

class DetectorDebugSnapshotStore(
    private val directory: File,
    private val isEnabled: Boolean = BuildConfig.ACCESSIBILITY_DEBUG_TOOLS_ENABLED,
) {
    fun save(
        packageName: String,
        eventType: String,
        snapshot: AccessibilityTreeSnapshot,
        nowMillis: Long = System.currentTimeMillis(),
    ): DetectorDebugSnapshot? {
        if (!isEnabled) {
            return null
        }

        directory.mkdirs()
        val file = File(directory, "youtube_snapshot_$nowMillis.txt")
        val content = snapshot.toSanitizedSnapshotText(packageName, eventType, nowMillis)
        file.writeText(content)
        return DetectorDebugSnapshot(
            path = file.absolutePath,
            summary = snapshot.toDebugSummary(),
        )
    }

    fun listSnapshots(): List<File> {
        if (!isEnabled) {
            return emptyList()
        }

        return directory
            .listFiles { file -> file.isFile && file.name.startsWith("youtube_snapshot_") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
    }

    private fun AccessibilityTreeSnapshot.toSanitizedSnapshotText(
        packageName: String,
        eventType: String,
        nowMillis: Long,
    ): String {
        val lines =
            buildList {
                add("package=$packageName")
                add("event=$eventType")
                add("capturedAtMillis=$nowMillis")
                add("summary=${toDebugSummary()}")
                nodes.forEachIndexed { index, node ->
                    add(
                        "node[$index] class=${node.className.substringAfterLast('.')} " +
                            "id=${node.viewIdResourceName?.substringAfterLast('/').orEmpty()} " +
                            "signals=${node.contentDescriptionSignals.sorted()} " +
                            "visible=${node.isVisibleToUser} scrollable=${node.isScrollable} " +
                            "bounds=${node.left},${node.top},${node.right},${node.bottom} " +
                            "depth=${node.depth}",
                    )
                }
            }
        return lines.joinToString(separator = "\n")
    }
}

data class DetectorDebugSnapshot(
    val path: String,
    val summary: String,
)
