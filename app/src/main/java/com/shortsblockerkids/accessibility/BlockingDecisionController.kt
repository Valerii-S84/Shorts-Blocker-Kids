package com.shortsblockerkids.accessibility

class BlockingDecisionController(
    private val blockCooldownMs: Long = DEFAULT_BLOCK_COOLDOWN_MS,
    private val detectionDebounceMs: Long = DEFAULT_DETECTION_DEBOUNCE_MS,
    private val pinEntryLaunchGraceMs: Long = DEFAULT_PIN_ENTRY_LAUNCH_GRACE_MS,
) {
    private var lastHighDetectionAtMillis: Long? = null
    private var lastBlockAtMillis: Long? = null
    private var pinEntryRequestedAtMillis: Long? = null
    private var overlayVisible = false

    fun evaluate(
        isInSupportedApp: Boolean,
        isProtectionActive: Boolean,
        result: DetectionResult,
        nowMillis: Long,
    ): BlockingDecision {
        if (!isInSupportedApp || !isProtectionActive) {
            lastHighDetectionAtMillis = null
            pinEntryRequestedAtMillis = null
            overlayVisible = false
            return BlockingDecision.DismissOverlay
        }

        if (!result.shouldBlock) {
            val keepPinEntryGrace = isPinEntryLaunchGraceActive(nowMillis)
            lastHighDetectionAtMillis = null
            if (!keepPinEntryGrace) {
                lastBlockAtMillis = null
                pinEntryRequestedAtMillis = null
            }
            if (overlayVisible) {
                overlayVisible = false
                return BlockingDecision.DismissOverlay
            }
            return BlockingDecision.Ignore
        }

        if (overlayVisible || isPinEntryLaunchGraceActive(nowMillis) || isWithinCooldown(nowMillis)) {
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

    fun onPinEntryRequested(nowMillis: Long = System.currentTimeMillis()) {
        pinEntryRequestedAtMillis = nowMillis
    }

    fun shouldIgnoreUnsupportedPackageEvent(nowMillis: Long): Boolean = overlayVisible || isPinEntryLaunchGraceActive(nowMillis)

    fun reset() {
        lastHighDetectionAtMillis = null
        lastBlockAtMillis = null
        pinEntryRequestedAtMillis = null
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

    private fun isPinEntryLaunchGraceActive(nowMillis: Long): Boolean {
        val requestedAt = pinEntryRequestedAtMillis ?: return false
        return nowMillis - requestedAt < pinEntryLaunchGraceMs
    }

    companion object {
        const val DEFAULT_BLOCK_COOLDOWN_MS = 1_200L
        const val DEFAULT_DETECTION_DEBOUNCE_MS = 500L
        const val DEFAULT_PIN_ENTRY_LAUNCH_GRACE_MS = 5_000L
    }
}

enum class BlockingDecision {
    Ignore,
    ShowOverlay,
    DismissOverlay,
}
