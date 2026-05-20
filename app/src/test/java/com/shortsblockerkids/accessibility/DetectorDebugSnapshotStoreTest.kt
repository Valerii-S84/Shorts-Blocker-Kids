package com.shortsblockerkids.accessibility

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
        assertTrue(text.contains("signals=[like, share]"))
        assertFalse(text.contains("private video title"))
        assertEquals(listOf(file), store.listSnapshots())
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
                ),
        )
}
