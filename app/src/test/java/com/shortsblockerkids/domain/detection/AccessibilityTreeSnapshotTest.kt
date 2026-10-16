package com.shortsblockerkids.domain.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityTreeSnapshotTest {
    @Test
    fun geometryExtremaFollowAllVisibleNodesRegardlessOfTraversalOrder() {
        val snapshot =
            AccessibilityTreeSnapshot(
                listOf(
                    node(left = 300, top = 500, right = 500, bottom = 700),
                    node(left = 350, top = 600, right = 450, bottom = 650),
                    node(left = 200, top = 400, right = 1_300, bottom = 2_600),
                ),
            )

        assertEquals(1_100, snapshot.maxWidth)
        assertEquals(2_200, snapshot.maxHeight)
        assertEquals(200, snapshot.screenLeft)
        assertEquals(400, snapshot.screenTop)
        assertEquals(1_300, snapshot.screenRight)
        assertEquals(2_600, snapshot.screenBottom)
        assertEquals(1_100, snapshot.screenWidth)
        assertEquals(2_200, snapshot.screenHeight)
    }

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

    @Test
    fun visibleBoundsUseWindowOriginAndExcludeHiddenAndEmptyOutliers() {
        val snapshot =
            AccessibilityTreeSnapshot(
                listOf(
                    node(left = 300, top = 500, right = 1_300, bottom = 2_500),
                    node(isVisibleToUser = false, left = -5_000, top = -5_000, right = 10_000, bottom = 10_000),
                    node(left = -9_000, top = -9_000, right = -9_000, bottom = 20_000),
                ),
            )

        assertEquals(300, snapshot.screenLeft)
        assertEquals(500, snapshot.screenTop)
        assertEquals(1_300, snapshot.screenRight)
        assertEquals(2_500, snapshot.screenBottom)
        assertEquals(1_000, snapshot.screenWidth)
        assertEquals(2_000, snapshot.screenHeight)
        assertEquals(1_000, snapshot.maxWidth)
        assertEquals(2_000, snapshot.maxHeight)
    }

    @Test
    fun fullyHiddenSnapshotHasNoGeometryOrStructuralSignals() {
        val snapshot =
            AccessibilityTreeSnapshot(
                listOf(node(isVisibleToUser = false, isScrollable = true, right = 1_080, bottom = 2_200)),
            )
        val matchedSignals = linkedSetOf<DetectorSignal>()

        assertEquals(0, snapshot.screenLeft)
        assertEquals(0, snapshot.screenTop)
        assertEquals(0, snapshot.screenWidth)
        assertEquals(0, snapshot.screenHeight)
        assertFalse(snapshot.hasActionRailSignal(setOf("like", "comments", "share"), matchedSignals))
        assertFalse(snapshot.hasVerticalFullscreenSignal(matchedSignals) { true })
        assertFalse(snapshot.hasRepeatedVerticalFeedSignal(matchedSignals) { true })
        assertTrue(matchedSignals.isEmpty())
    }

    @Test
    fun zeroAreaCandidatesCannotSupplySignalsInsideVisibleWindow() {
        val actions = listOf("like", "comments", "share")
        val snapshot =
            AccessibilityTreeSnapshot(
                listOf(node(right = 1_080, bottom = 2_200, depth = 0)) +
                    actions.mapIndexed { index, action ->
                        node(
                            contentDescriptionSignals = setOf(action),
                            isScrollable = true,
                            left = 930,
                            top = 700 + index * 400,
                            right = 930,
                            width = 1_080,
                            height = 2_200,
                        )
                    },
            )
        val matchedSignals = linkedSetOf<DetectorSignal>()

        assertFalse(snapshot.hasActionRailSignal(actions.toSet(), matchedSignals))
        assertFalse(snapshot.hasVerticalFullscreenSignal(matchedSignals) { true })
        assertFalse(snapshot.hasRepeatedVerticalFeedSignal(matchedSignals) { true })
        assertTrue(matchedSignals.isEmpty())
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
