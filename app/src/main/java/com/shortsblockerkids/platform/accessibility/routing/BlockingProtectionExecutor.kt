package com.shortsblockerkids.platform.accessibility.routing

/** Executes a blocking decision and reports only the observed protection outcome. */
class BlockingProtectionExecutor(
    private val showOverlay: () -> Boolean,
    private val startFallback: (String, (Boolean) -> Unit) -> Unit,
    private val recordOutcome: (String) -> Unit,
    private val onOverlayFailed: () -> Unit,
    private val startPlaybackExit: (String, (Boolean) -> Unit) -> Unit,
) {
    private var fallbackPackage: String? = null
    private var fallbackPending = false
    private var generation = 0

    fun protect(packageName: String) {
        if (fallbackPending) return
        if (showOverlay()) {
            recordOutcome("ShowOverlay")
            fallbackPending = true
            val request = generation
            recordOutcome("AutomaticPlaybackExitPending")
            startPlaybackExit(packageName) { completed ->
                if (request == generation) {
                    fallbackPending = false
                    recordOutcome(if (completed) "PlaybackExitConfirmed" else "OverlayRetainedExitUnconfirmed")
                }
            }
            return
        }
        if (fallbackPackage == packageName) {
            onOverlayFailed()
            recordOutcome("ProtectionFailed")
            return
        }
        fallbackPackage = packageName
        fallbackPending = true
        val request = generation
        recordOutcome("OverlayFailedFallbackPending")
        startFallback(packageName) { completed ->
            if (request == generation) {
                fallbackPending = false
                onOverlayFailed()
                recordOutcome(if (completed) "FallbackExecuted" else "ProtectionFailed")
            }
        }
    }

    fun reset() {
        generation++
        fallbackPending = false
        fallbackPackage = null
    }
}
