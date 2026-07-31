package com.shortsblockerkids.domain.detection

interface ShortVideoDetector {
    val platform: SupportedPlatform
    val supportedPackages: Set<String>

    fun detect(
        packageName: String?,
        snapshot: AccessibilityTreeSnapshot,
    ): DetectionResult
}
