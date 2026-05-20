package com.shortsblockerkids.accessibility

class BlockingDecisionController(
    private val blockCooldownMs: Long = DEFAULT_BLOCK_COOLDOWN_MS,
    private val detectionDebounceMs: Long = DEFAULT_DETECTION_DEBOUNCE_MS,
) {
    private var lastHighDetectionAtMillis: Long? = null
    private var lastBlockAtMillis: Long? = null
    private var overlayVisible = false

    fun evaluate(
        isInYouTube: Boolean,
        isProtectionActive: Boolean,
        result: DetectionResult,
        nowMillis: Long,
    ): BlockingDecision {
        if (!isInYouTube || !isProtectionActive) {
            lastHighDetectionAtMillis = null
            overlayVisible = false
            return BlockingDecision.DismissOverlay
        }

        if (!result.shouldBlock) {
            return BlockingDecision.Ignore
        }

        if (overlayVisible || isWithinCooldown(nowMillis)) {
            return BlockingDecision.Ignore
        }

        if (isWithinDetectionDebounce(nowMillis)) {
            lastHighDetectionAtMillis = nowMillis
            return BlockingDecision.Ignore
        }

        lastHighDetectionAtMillis = nowMillis
        lastBlockAtMillis = nowMillis
        overlayVisible = true
        return BlockingDecision.ShowOverlay
    }

    fun onOverlayDismissed() {
        overlayVisible = false
    }

    fun reset() {
        lastHighDetectionAtMillis = null
        lastBlockAtMillis = null
        overlayVisible = false
    }

    private fun isWithinCooldown(nowMillis: Long): Boolean {
        val lastBlockAt = lastBlockAtMillis ?: return false
        return nowMillis - lastBlockAt < blockCooldownMs
    }

    private fun isWithinDetectionDebounce(nowMillis: Long): Boolean {
        val lastHighDetectionAt = lastHighDetectionAtMillis ?: return false
        return nowMillis - lastHighDetectionAt < detectionDebounceMs
    }

    companion object {
        const val DEFAULT_BLOCK_COOLDOWN_MS = 1_200L
        const val DEFAULT_DETECTION_DEBOUNCE_MS = 500L
    }
}

enum class BlockingDecision {
    Ignore,
    ShowOverlay,
    DismissOverlay,
}
