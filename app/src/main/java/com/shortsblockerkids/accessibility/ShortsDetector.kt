package com.shortsblockerkids.accessibility

interface ShortsDetector {
    fun detect(
        packageName: String?,
        snapshot: AccessibilityTreeSnapshot,
    ): DetectionResult
}
