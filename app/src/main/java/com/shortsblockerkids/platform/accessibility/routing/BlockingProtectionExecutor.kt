package com.shortsblockerkids.platform.accessibility.routing

/** Executes a blocking decision and reports only the observed protection outcome. */
class BlockingProtectionExecutor(
    private val showOverlay: (String) -> Boolean,
    private val startFallback: (String, (Boolean) -> Unit) -> Unit,
    private val recordOutcome: (String) -> Unit,
    private val onOverlayFailed: () -> Unit,
    private val scheduleRetry: (Long, () -> Unit) -> Unit = { _, _ -> },
    private val shouldRetryProtection: (String) -> Boolean = { true },
) {
    private var fallbackPending = false
    private var overlayPresented = false
    private var generation = 0
    private var protectionAttempts = 0

    fun protect(packageName: String) {
        if (fallbackPending || overlayPresented || protectionAttempts >= MAX_PROTECTION_ATTEMPTS) return
        protectionAttempts++
        val request = ++generation
        val shown =
            try {
                showOverlay(packageName)
            } catch (_: RuntimeException) {
                false
            }
        if (shown) {
            overlayPresented = true
            recordOutcome("ShowOverlay")
            return
        }
        fallbackPending = true
        recordOutcome("OverlayFailedFallbackPending")
        startExit(packageName, request)
    }

    private fun startExit(
        packageName: String,
        request: Int,
    ) {
        val completed: (Boolean) -> Unit = callback@{ success ->
            if (request != generation || !fallbackPending) return@callback
            fallbackPending = false
            onOverlayFailed()
            recordOutcome(if (success) "FallbackExecuted" else "ProtectionFailed")
            if (!success) retryLater(packageName, request)
        }
        try {
            startFallback(packageName, completed)
        } catch (_: RuntimeException) {
            // Platform/window failures are a failed attempt, never an accepted exit.
            completed(false)
        }
    }

    private fun retryLater(
        packageName: String,
        request: Int,
    ) {
        if (protectionAttempts >= MAX_PROTECTION_ATTEMPTS) {
            recordOutcome("ProtectionRetryExhausted")
            return
        }
        try {
            scheduleRetry(PROTECTION_RETRY_DELAY_MS) {
                if (request != generation) return@scheduleRetry
                val stillBlocked =
                    try {
                        shouldRetryProtection(packageName)
                    } catch (_: RuntimeException) {
                        // An unavailable observation does not establish that protection is unnecessary.
                        recordOutcome("ProtectionRetryCheckFailed")
                        protectionAttempts++
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
        overlayPresented = false
        protectionAttempts = 0
    }

    companion object {
        const val PROTECTION_RETRY_DELAY_MS = 1_500L
        const val MAX_PROTECTION_ATTEMPTS = 4
    }
}
