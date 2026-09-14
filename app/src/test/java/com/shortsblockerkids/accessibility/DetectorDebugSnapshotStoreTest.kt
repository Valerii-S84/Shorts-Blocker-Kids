package com.shortsblockerkids.accessibility

import com.shortsblockerkids.domain.detection.AccessibilityNodeSignal
import com.shortsblockerkids.domain.detection.AccessibilityTreeSnapshot
import com.shortsblockerkids.platform.accessibility.diagnostics.DetectorDebugSnapshotStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DetectorDebugSnapshotStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun disabledStoreDoesNotWriteSnapshot() {
        val store = DetectorDebugSnapshotStore(temporaryFolder.root, isEnabled = false)

        val snapshot = store.save("com.google.android.youtube", "TYPE_VIEW_SCROLLED", snapshot())

        assertNull(snapshot)
        assertTrue(
            temporaryFolder.root
                .listFiles()
                .orEmpty()
                .isEmpty(),
        )
    }

    @Test
    fun disabledStoreDoesNotListSnapshots() {
        val store = DetectorDebugSnapshotStore(temporaryFolder.root, isEnabled = false)
        File(temporaryFolder.root, "youtube_snapshot_1000.txt").writeText("snapshot")

        assertTrue(store.listSnapshots().isEmpty())
    }

    @Test
    fun enabledStoreReturnsEmptyListWhenDirectoryDoesNotExist() {
        val store = DetectorDebugSnapshotStore(File(temporaryFolder.root, "missing"), isEnabled = true)

        assertTrue(store.listSnapshots().isEmpty())
    }

    @Test
    fun enabledStoreWritesSanitizedSnapshot() {
        val store = DetectorDebugSnapshotStore(temporaryFolder.root, isEnabled = true)

        val saved =
            store.save(
                packageName = "com.google.android.youtube",
                eventType = "TYPE_VIEW_SCROLLED",
                snapshot = snapshot(),
                nowMillis = 1_000L,
            )

        assertNotNull(saved)
        val file = File(saved!!.path)
        val text = file.readText()
        assertTrue(text.contains("package=com.google.android.youtube"))
        assertTrue(text.contains("id=reel_watch_sequence"))
        assertTrue(text.contains("id= signals=[] visible=false"))
        assertTrue(text.contains("signals=[like, share]"))
        assertFalse(text.contains("private video title"))
        assertEquals(listOf(file), store.listSnapshots())
    }

    @Test
    fun enabledStoreListsOnlySnapshotFilesNewestFirst() {
        val store = DetectorDebugSnapshotStore(temporaryFolder.root, isEnabled = true)
        val older = File(temporaryFolder.root, "youtube_snapshot_1000.txt").apply { writeText("older") }
        val newer = File(temporaryFolder.root, "youtube_snapshot_2000.txt").apply { writeText("newer") }
        File(temporaryFolder.root, "notes.txt").writeText("ignore")
        File(temporaryFolder.root, "youtube_snapshot_directory").mkdir()
        older.setLastModified(1_000L)
        newer.setLastModified(2_000L)

        assertEquals(listOf(newer, older), store.listSnapshots())
    }

    private fun snapshot(): AccessibilityTreeSnapshot =
        AccessibilityTreeSnapshot(
            nodes =
                listOf(
                    AccessibilityNodeSignal(
                        className = "androidx.viewpager.widget.ViewPager",
                        viewIdResourceName = "com.google.android.youtube:id/reel_watch_sequence",
                        contentDescriptionSignals = setOf("share", "like"),
                        isClickable = true,
                        isScrollable = true,
                        isVisibleToUser = true,
                        left = 0,
                        top = 0,
                        right = 1080,
                        bottom = 2140,
                        width = 1080,
                        height = 2140,
                        depth = 2,
                    ),
                    AccessibilityNodeSignal(
                        className = "android.view.View",
                        viewIdResourceName = null,
                        contentDescriptionSignals = emptySet(),
                        isClickable = false,
                        isScrollable = false,
                        isVisibleToUser = false,
                        left = 10,
                        top = 10,
                        right = 20,
                        bottom = 20,
                        width = 10,
                        height = 10,
                        depth = 3,
                    ),
                ),
        )
}
