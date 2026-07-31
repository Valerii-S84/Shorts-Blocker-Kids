package com.shortsblockerkids.platform.accessibility.detection

import com.shortsblockerkids.accessibility.DebugFixturePackages
import com.shortsblockerkids.domain.detection.FacebookReelsDetector
import com.shortsblockerkids.domain.detection.InstagramReelsDetector
import com.shortsblockerkids.domain.detection.ShortVideoDetectionEngine
import com.shortsblockerkids.domain.detection.TikTokShortVideoDetector
import com.shortsblockerkids.domain.detection.YouTubeShortsDetector

object ProductionDetectorRegistry {
    fun create(): ShortVideoDetectionEngine =
        ShortVideoDetectionEngine(
            detectors =
                listOf(
                    YouTubeShortsDetector(
                        packageAliases = DebugFixturePackages.enabled(DebugFixturePackages.YOUTUBE),
                    ),
                    TikTokShortVideoDetector(
                        packageAliases = DebugFixturePackages.enabled(DebugFixturePackages.TIKTOK),
                    ),
                    InstagramReelsDetector(
                        packageAliases = DebugFixturePackages.enabled(DebugFixturePackages.INSTAGRAM),
                    ),
                    FacebookReelsDetector(
                        packageAliases = DebugFixturePackages.enabled(DebugFixturePackages.FACEBOOK),
                    ),
                ),
        )
}
