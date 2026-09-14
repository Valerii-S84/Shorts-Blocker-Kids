package com.shortsblockerkids.platform.accessibility.overlay

class PhoneHomeExitController(
    private val exitAction: () -> Boolean,
    private val isPlaybackClosed: () -> Boolean,
    private val delayedActionScheduler: DelayedActionScheduler,
    private val onExitCompleted: () -> Unit,
    private val onExitFailed: () -> Unit,
) {
    private var generation = 0
    private var exiting = false

    fun exitShorts() {
        if (exiting) return
        exiting = true
        attemptExit(generation, 0)
    }

    fun cancel() {
        generation += 1
        exiting = false
    }

    private fun attemptExit(
        request: Int,
        attempt: Int,
    ) {
        if (request != generation) return
        if (!exitAction()) {
            delayedActionScheduler.schedule(EXIT_CHECK_DELAY_MS) {
                if (request == generation) retry(request, attempt)
            }
            return
        }
        delayedActionScheduler.schedule(EXIT_CHECK_DELAY_MS) {
            if (request == generation) checkExit(request, attempt)
        }
    }

    private fun checkExit(
        request: Int,
        attempt: Int,
    ) {
        if (isPlaybackClosed()) {
            // A second observation avoids dismissing during a window transition to PiP.
            delayedActionScheduler.schedule(EXIT_CHECK_DELAY_MS) {
                if (request == generation) {
                    if (isPlaybackClosed()) finish(onExitCompleted) else retry(request, attempt)
                }
            }
        } else {
            retry(request, attempt)
        }
    }

    private fun retry(
        request: Int,
        attempt: Int,
    ) {
        if (attempt + 1 < MAX_EXIT_ATTEMPTS) attemptExit(request, attempt + 1) else finish(onExitFailed)
    }

    private fun finish(callback: () -> Unit) {
        exiting = false
        callback()
    }

    companion object {
        const val EXIT_CHECK_DELAY_MS = 350L
        const val MAX_EXIT_ATTEMPTS = 4
    }
}

fun interface DelayedActionScheduler {
    fun schedule(
        delayMillis: Long,
        action: () -> Unit,
    )
}
