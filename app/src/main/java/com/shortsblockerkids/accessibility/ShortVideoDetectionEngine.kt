package com.shortsblockerkids.accessibility

class ShortVideoDetectionEngine(
    detectors: List<ShortVideoDetector>,
) {
    private val detectorPackagePairs =
        detectors.flatMap { detector ->
            detector.supportedPackages.map { packageName -> packageName to detector }
        }
    private val detectorsByPackage = detectorPackagePairs.toMap()

    init {
        val duplicatePackages =
            detectorPackagePairs
                .groupingBy { it.first }
                .eachCount()
                .filterValues { count -> count > 1 }
                .keys
        require(duplicatePackages.isEmpty()) {
            "Duplicate short-video detector packages: $duplicatePackages"
        }
    }

    fun supportsPackage(packageName: String?): Boolean = detectorFor(packageName) != null

    fun detect(
        packageName: String?,
        snapshot: AccessibilityTreeSnapshot,
    ): DetectionResult {
        val detector = detectorFor(packageName) ?: return DetectionResult.None
        return detector.detect(packageName, snapshot)
    }

    private fun detectorFor(packageName: String?): ShortVideoDetector? {
        val safePackageName = packageName ?: return null
        return detectorsByPackage[safePackageName]
    }

    companion object {
        fun youtubeOnly(): ShortVideoDetectionEngine =
            ShortVideoDetectionEngine(
                detectors = listOf(YouTubeShortsDetector()),
            )
    }
}
