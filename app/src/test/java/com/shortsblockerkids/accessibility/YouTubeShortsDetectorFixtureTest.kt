package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeShortsDetectorFixtureTest {
    private val detector = YouTubeShortsDetector()

    @Test
    fun homeFixtureIsNotBlocked() {
        val result = detectFixture("youtube_home.json")

        assertFalse(result.shouldBlock)
        assertTrue(result.confidence == Confidence.NONE || result.confidence == Confidence.LOW)
    }

    @Test
    fun normalVideoFixtureIsNotBlocked() {
        val result = detectFixture("youtube_normal_video.json")

        assertFalse(result.shouldBlock)
        assertTrue(result.confidence == Confidence.NONE || result.confidence == Confidence.LOW)
    }

    @Test
    fun searchFixtureIsNotBlocked() {
        val result = detectFixture("youtube_search.json")

        assertFalse(result.shouldBlock)
        assertTrue(result.confidence == Confidence.NONE || result.confidence == Confidence.LOW)
    }

    @Test
    fun subscriptionsFixtureIsNotBlocked() {
        val result = detectFixture("youtube_subscriptions.json")

        assertFalse(result.shouldBlock)
        assertTrue(result.confidence == Confidence.NONE || result.confidence == Confidence.LOW)
    }

    @Test
    fun libraryFixtureIsNotBlocked() {
        val result = detectFixture("youtube_library.json")

        assertFalse(result.shouldBlock)
        assertTrue(result.confidence == Confidence.NONE || result.confidence == Confidence.LOW)
    }

    @Test
    fun shortsTabOnlyFixtureIsLowAndNotBlocked() {
        val result = detectFixture("youtube_shorts_tab_low.json")

        assertEquals(Confidence.LOW, result.confidence)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun shortsReelFixtureIsHighAndBlocked() {
        val result = detectFixture("youtube_shorts_reel_high.json")

        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.shouldBlock)
        assertTrue(result.matchedSignals.contains(DetectorSignal.REEL_CONTAINER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.ACTION_RAIL.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.VERTICAL_FULLSCREEN.id))
    }

    @Test
    fun shortsSwipeFixtureIsHighAndBlocked() {
        val result = detectFixture("youtube_shorts_swipe_high.json")

        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.shouldBlock)
        assertTrue(result.matchedSignals.contains(DetectorSignal.SHORTS_IDENTIFIER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.REPEATED_VERTICAL_FEED.id))
    }

    @Test
    fun nonYouTubePackageIsNoneEvenWithShortsStructure() {
        val fixture = DetectorFixtureLoader.load("youtube_shorts_reel_high.json")
        val result =
            detector.detect(
                packageName = "com.example.other",
                snapshot = fixture.snapshot,
            )

        assertEquals(Confidence.NONE, result.confidence)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun repeatedShortsDetectionsStayHighConfidence() {
        val fixture = DetectorFixtureLoader.load("youtube_shorts_reel_high.json")

        repeat(20) {
            val result = detector.detect(fixture.packageName, fixture.snapshot)

            assertEquals(Confidence.HIGH, result.confidence)
            assertTrue(result.shouldBlock)
            assertTrue(result.matchedSignals.size >= 3)
        }
    }

    private fun detectFixture(fileName: String): DetectionResult {
        val fixture = DetectorFixtureLoader.load(fileName)
        return detector.detect(fixture.packageName, fixture.snapshot)
    }
}
