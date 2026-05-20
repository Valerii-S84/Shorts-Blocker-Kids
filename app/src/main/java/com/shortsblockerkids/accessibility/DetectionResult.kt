package com.shortsblockerkids.accessibility

data class DetectionResult(
    val isShorts: Boolean,
    val confidence: Confidence,
    val reasons: List<String>,
    val matchedSignals: List<String> = emptyList(),
) {
    val shouldBlock: Boolean
        get() = isShorts && confidence == Confidence.HIGH

    companion object {
        val None =
            DetectionResult(
                isShorts = false,
                confidence = Confidence.NONE,
                reasons = emptyList(),
                matchedSignals = emptyList(),
            )
    }
}
