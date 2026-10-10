package com.shortsblockerkids.platform.accessibility.routing

/** Executes a blocking decision and reports only the observed protection outcome. */
class BlockingProtectionExecutor(
    private val showOverlay: (String) -> Boolean,
    private val startFallback: (String, (Boolean) -> Unit) -> Unit,
    private val recordOutcome: (String) -> Unit,
    private val onOverlayFailed: () -> Unit,
    private val startPlaybackExit: (String, (Boolean) -> Unit) -> Unit,
    private val scheduleRetry: (Long, () -> Unit) -> Unit = { _, _ -> },
    private val shouldRetryProtection: (String) -> Boolean = { true },
) {
    private var fallbackPending = false
    private var generation = 0

    fun protect(packageName: String) {
        if (fallbackPending) return
        val request = ++generation
        val shown =
            try {
                showOverlay(packageName)
            } catch (_: RuntimeException) {
                false
            }
        if (shown) {
            recordOutcome("ShowOverlay")
            fallbackPending = true
            recordOutcome("AutomaticPlaybackExitPending")
            startExit(startPlaybackExit, packageName, request, overlayShown = true)
            return
        }
        fallbackPending = true
        recordOutcome("OverlayFailedFallbackPending")
        startExit(startFallback, packageName, request, overlayShown = false)
    }

    private fun startExit(
        start: (String, (Boolean) -> Unit) -> Unit,
        packageName: String,
        request: Int,
        overlayShown: Boolean,
    ) {
        val completed: (Boolean) -> Unit = callback@{ success ->
            if (request != generation || !fallbackPending) return@callback
            fallbackPending = false
            if (!overlayShown) onOverlayFailed()
            recordOutcome(
                if (overlayShown) {
                    if (success) "PlaybackExitConfirmed" else "OverlayRetainedExitUnconfirmed"
                } else {
                    if (success) "FallbackExecuted" else "ProtectionFailed"
                },
            )
            if (!success) retryLater(packageName, request)
        }
        try {
            start(packageName, completed)
        } catch (_: RuntimeException) {
            // Platform/window failures are a failed attempt, never an accepted exit.
            completed(false)
        }
    }

    private fun retryLater(
        packageName: String,
        request: Int,
    ) {
        try {
            scheduleRetry(PROTECTION_RETRY_DELAY_MS) {
                if (request != generation) return@scheduleRetry
                val stillBlocked =
                    try {
                        shouldRetryProtection(packageName)
                    } catch (_: RuntimeException) {
                        // An unavailable observation does not establish that protection is unnecessary.
                        recordOutcome("ProtectionRetryCheckFailed")
                        retryLater(packageName, request)
                        return@scheduleRetry
                    }
                if (stillBlocked) protect(packageName)
            }
        } catch (_: RuntimeException) {
            recordOutcome("ProtectionRetryFailed")
        }
    }

    fun reset() {
        generation++
        fallbackPending = false
    }

    companion object {
        const val PROTECTION_RETRY_DELAY_MS = 1_500L
    }
}
