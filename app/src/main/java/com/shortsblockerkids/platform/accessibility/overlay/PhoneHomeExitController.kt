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
        if (!platformResult(exitAction)) {
            scheduleCheck {
                if (request == generation) retry(request, attempt)
            }
            return
        }
        scheduleCheck {
            if (request == generation) checkExit(request, attempt)
        }
    }

    private fun checkExit(
        request: Int,
        attempt: Int,
    ) {
        if (platformResult(isPlaybackClosed)) {
            // A second observation avoids dismissing during a window transition to PiP.
            scheduleCheck {
                if (request == generation) {
                    if (platformResult(isPlaybackClosed)) finish(onExitCompleted) else retry(request, attempt)
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

    private fun platformResult(action: () -> Boolean): Boolean =
        try {
            action()
        } catch (_: RuntimeException) {
            false
        }

    private fun scheduleCheck(action: () -> Unit) {
        try {
            delayedActionScheduler.schedule(EXIT_CHECK_DELAY_MS, action)
        } catch (_: RuntimeException) {
            // A stopped looper/window is a controlled failure, with the barrier retained.
            finish(onExitFailed)
        }
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
