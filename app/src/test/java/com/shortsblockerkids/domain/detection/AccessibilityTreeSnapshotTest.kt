package com.shortsblockerkids.domain.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityTreeSnapshotTest {
    @Test
    fun emptySnapshotUsesZeroDimensionsAndEmptyDebugSummary() {
        val snapshot = AccessibilityTreeSnapshot.Empty

        assertEquals(0, snapshot.visibleNodeCount)
        assertEquals(0, snapshot.maxWidth)
        assertEquals(0, snapshot.maxHeight)
        assertEquals(0, snapshot.screenRight)
        assertEquals(0, snapshot.screenBottom)
        assertEquals("visible=0 classes=[] ids=[] signals=[]", snapshot.toDebugSummary())
    }

    @Test
    fun debugSummarySkipsBlankClassesAndNullViewIds() {
        val snapshot =
            AccessibilityTreeSnapshot(
                nodes =
                    listOf(
                        node(className = "", viewIdResourceName = null, isVisibleToUser = false),
                        node(
                            className = "android.view.View",
                            viewIdResourceName = "com.google.android.youtube:id/reel_watch_sequence",
                            contentDescriptionSignals = setOf("share", "like"),
                        ),
                    ),
            )

        assertEquals(
            "visible=1 classes=[View] ids=[reel_watch_sequence] signals=[share, like]",
            snapshot.toDebugSummary(),
        )
    }

    @Test
    fun zeroSizedSnapshotsDoNotEmitStructuralSignals() {
        val snapshot =
            AccessibilityTreeSnapshot(
                nodes =
                    listOf(
                        node(
                            viewIdResourceName = "com.google.android.youtube:id/reel_watch_sequence",
                            contentDescriptionSignals = setOf("like", "comments", "share"),
                            isScrollable = true,
                            right = 0,
                            bottom = 0,
                            width = 0,
                            height = 0,
                        ),
                    ),
            )
        val matchedSignals = linkedSetOf<DetectorSignal>()

        assertFalse(snapshot.hasActionRailSignal(setOf("like", "comments", "share"), matchedSignals))
        assertFalse(snapshot.hasVerticalFullscreenSignal(matchedSignals) { true })
        assertFalse(snapshot.hasRepeatedVerticalFeedSignal(matchedSignals) { true })
        assertTrue(matchedSignals.isEmpty())
    }

    @Test
    fun positiveWidthWithZeroHeightDoesNotEmitStructuralSignals() {
        val snapshot =
            AccessibilityTreeSnapshot(
                nodes =
                    listOf(
                        node(
                            viewIdResourceName = "com.google.android.youtube:id/reel_watch_sequence",
                            contentDescriptionSignals = setOf("like", "comments", "share"),
                            isScrollable = true,
                            right = 1080,
                            bottom = 0,
                            width = 1080,
                            height = 0,
                        ),
                    ),
            )
        val matchedSignals = linkedSetOf<DetectorSignal>()

        assertFalse(snapshot.hasActionRailSignal(setOf("like", "comments", "share"), matchedSignals))
        assertFalse(snapshot.hasVerticalFullscreenSignal(matchedSignals) { true })
        assertFalse(snapshot.hasRepeatedVerticalFeedSignal(matchedSignals) { true })
        assertTrue(matchedSignals.isEmpty())
    }

    @Test
    fun containsAnyIgnoringCaseHandlesNullMissAndMatch() {
        assertFalse(null.containsAnyIgnoringCase("shorts"))
        assertFalse("watch_player".containsAnyIgnoringCase("shorts", "reel"))
        assertTrue("Reel_Watch_Sequence".containsAnyIgnoringCase("reel"))
    }

    private fun node(
        className: String = "android.view.View",
        viewIdResourceName: String? = null,
        contentDescriptionSignals: Set<String> = emptySet(),
        isScrollable: Boolean = false,
        isVisibleToUser: Boolean = true,
        left: Int = 0,
        top: Int = 0,
        right: Int = left + 120,
        bottom: Int = top + 120,
        width: Int = right - left,
        height: Int = bottom - top,
        depth: Int = 1,
    ): AccessibilityNodeSignal =
        AccessibilityNodeSignal(
            className = className,
            viewIdResourceName = viewIdResourceName,
            contentDescriptionSignals = contentDescriptionSignals,
            isClickable = true,
            isScrollable = isScrollable,
            isVisibleToUser = isVisibleToUser,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            width = width,
            height = height,
            depth = depth,
        )
}
