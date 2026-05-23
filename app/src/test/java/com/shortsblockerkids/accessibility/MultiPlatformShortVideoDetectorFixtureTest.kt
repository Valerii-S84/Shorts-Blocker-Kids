package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiPlatformShortVideoDetectorFixtureTest {
    @Test
    fun productionEngineSupportsPrimaryPlatformPackages() {
        val engine = ShortVideoDetectionEngine.production()

        assertTrue(engine.supportsPackage(YouTubeShortsDetector.YOUTUBE_PACKAGE))
        assertTrue(engine.supportsPackage(TikTokShortVideoDetector.TIKTOK_PACKAGE))
        assertTrue(engine.supportsPackage(InstagramReelsDetector.INSTAGRAM_PACKAGE))
        assertTrue(engine.supportsPackage(FacebookReelsDetector.FACEBOOK_PACKAGE))
        assertFalse(engine.supportsPackage("com.facebook.lite"))
        assertFalse(engine.supportsPackage("com.ss.android.ugc.trill"))
    }

    @Test
    fun productionEngineReportsProtectedPlatforms() {
        val engine = ShortVideoDetectionEngine.production()

        assertEquals(
            listOf(
                SupportedPlatform.YOUTUBE_SHORTS,
                SupportedPlatform.TIKTOK,
                SupportedPlatform.INSTAGRAM_REELS,
                SupportedPlatform.FACEBOOK_REELS,
            ),
            engine.supportedPlatforms,
        )
    }

    @Test
    fun tiktokFeedFixtureIsHighAndBlocked() {
        val result = detectFixture("tiktok_feed_high.json")

        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.shouldBlock)
        assertTrue(result.matchedSignals.contains(DetectorSignal.SHORT_VIDEO_IDENTIFIER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.ACTION_RAIL.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.VERTICAL_FULLSCREEN.id))
    }

    @Test
    fun tiktokProfileFixtureIsNotBlocked() {
        val result = detectFixture("tiktok_profile.json")

        assertFalse(result.shouldBlock)
    }

    @Test
    fun instagramReelsFixtureIsHighAndBlocked() {
        val result = detectFixture("instagram_reels_high.json")

        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.shouldBlock)
        assertTrue(result.matchedSignals.contains(DetectorSignal.SHORT_VIDEO_IDENTIFIER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.REEL_CONTAINER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.ACTION_RAIL.id))
    }

    @Test
    fun instagramFeedFixtureIsNotBlocked() {
        val result = detectFixture("instagram_feed.json")

        assertFalse(result.shouldBlock)
    }

    @Test
    fun facebookReelsFixtureIsHighAndBlocked() {
        val result = detectFixture("facebook_reels_high.json")

        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.shouldBlock)
        assertTrue(result.matchedSignals.contains(DetectorSignal.SHORT_VIDEO_IDENTIFIER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.REEL_CONTAINER.id))
        assertTrue(result.matchedSignals.contains(DetectorSignal.ACTION_RAIL.id))
    }

    @Test
    fun facebookFeedFixtureIsNotBlocked() {
        val result = detectFixture("facebook_feed.json")

        assertFalse(result.shouldBlock)
    }

    @Test
    fun unknownPackageWithReelsFixtureIsNotBlocked() {
        val fixture = DetectorFixtureLoader.load("instagram_reels_high.json")
        val engine = ShortVideoDetectionEngine.production()

        val result = engine.detect("com.example.other", fixture.snapshot)

        assertEquals(DetectionResult.None, result)
        assertFalse(result.shouldBlock)
    }

    private fun detectFixture(fileName: String): DetectionResult {
        val fixture = DetectorFixtureLoader.load(fileName)
        val engine = ShortVideoDetectionEngine.production()
        return engine.detect(fixture.packageName, fixture.snapshot)
    }
}
