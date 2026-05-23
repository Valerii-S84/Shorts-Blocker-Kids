package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortVideoDetectionEngineTest {
    @Test
    fun youtubeShortsDetectionStillBlocksThroughEngine() {
        val fixture = DetectorFixtureLoader.load("youtube_shorts_reel_high.json")
        val engine = ShortVideoDetectionEngine.youtubeOnly()

        val result = engine.detect(fixture.packageName, fixture.snapshot)

        assertTrue(result.shouldBlock)
        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.matchedSignals.contains(DetectorSignal.REEL_CONTAINER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.ACTION_RAIL.id))
    }

    @Test
    fun normalYouTubeScreensAreNotBlockedThroughEngine() {
        val fixture = DetectorFixtureLoader.load("youtube_normal_video.json")
        val engine = ShortVideoDetectionEngine.youtubeOnly()

        val result = engine.detect(fixture.packageName, fixture.snapshot)

        assertFalse(result.shouldBlock)
    }

    @Test
    fun unknownPackagesAreIgnoredWithoutCallingDetector() {
        val detector =
            RecordingDetector(
                supportedPackages = setOf(YouTubeShortsDetector.YOUTUBE_PACKAGE),
                result = blockingResult(),
            )
        val engine = ShortVideoDetectionEngine(listOf(detector))

        val result = engine.detect("com.example.unknown", emptySnapshot())

        assertFalse(engine.supportsPackage("com.example.unknown"))
        assertEquals(DetectionResult.None, result)
        assertEquals(0, detector.callCount)
    }

    @Test
    fun packageForPlatformWithoutRegisteredDetectorIsIgnored() {
        val engine = ShortVideoDetectionEngine(listOf(YouTubeShortsDetector()))
        val fixture = DetectorFixtureLoader.load("tiktok_feed_high.json")

        val result = engine.detect(fixture.packageName, fixture.snapshot)

        assertFalse(engine.supportsPackage(fixture.packageName))
        assertEquals(DetectionResult.None, result)
    }

    @Test
    fun packageForRegisteredPlatformCanBlock() {
        val engine = ShortVideoDetectionEngine(listOf(TikTokShortVideoDetector()))
        val fixture = DetectorFixtureLoader.load("tiktok_feed_high.json")

        val result = engine.detect(fixture.packageName, fixture.snapshot)

        assertTrue(engine.supportsPackage(fixture.packageName))
        assertTrue(result.shouldBlock)
    }

    @Test
    fun routesToTheMatchingDetectorWhenMultipleDetectorsAreRegistered() {
        val youtubeDetector =
            RecordingDetector(
                supportedPackages = setOf(YouTubeShortsDetector.YOUTUBE_PACKAGE),
                result = DetectionResult.None,
            )
        val futureDetector =
            RecordingDetector(
                platform = SupportedPlatform(id = "future_platform", displayName = "Future Platform"),
                supportedPackages = setOf(FUTURE_PACKAGE),
                result = blockingResult(),
            )
        val engine = ShortVideoDetectionEngine(listOf(youtubeDetector, futureDetector))

        val result = engine.detect(FUTURE_PACKAGE, emptySnapshot())

        assertTrue(engine.supportsPackage(FUTURE_PACKAGE))
        assertTrue(result.shouldBlock)
        assertEquals(0, youtubeDetector.callCount)
        assertEquals(1, futureDetector.callCount)
    }

    @Test
    fun duplicateDetectorPackagesAreRejected() {
        val first = RecordingDetector(supportedPackages = setOf(FUTURE_PACKAGE))
        val second = RecordingDetector(supportedPackages = setOf(FUTURE_PACKAGE))

        assertThrows(IllegalArgumentException::class.java) {
            ShortVideoDetectionEngine(listOf(first, second))
        }
    }

    private fun emptySnapshot(): AccessibilityTreeSnapshot = AccessibilityTreeSnapshot(nodes = emptyList())

    private fun blockingResult(): DetectionResult =
        DetectionResult(
            isShorts = true,
            confidence = Confidence.HIGH,
            reasons = listOf("test"),
            matchedSignals = listOf("test_signal"),
        )

    private class RecordingDetector(
        override val platform: SupportedPlatform = SupportedPlatform.YOUTUBE_SHORTS,
        override val supportedPackages: Set<String>,
        private val result: DetectionResult = DetectionResult.None,
    ) : ShortVideoDetector {
        var callCount = 0
            private set

        override fun detect(
            packageName: String?,
            snapshot: AccessibilityTreeSnapshot,
        ): DetectionResult {
            callCount += 1
            return result
        }
    }

    private companion object {
        const val FUTURE_PACKAGE = "com.example.future.shortvideo"
    }
}
