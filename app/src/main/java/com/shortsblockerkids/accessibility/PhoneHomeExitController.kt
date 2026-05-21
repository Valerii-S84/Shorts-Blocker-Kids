package com.shortsblockerkids.accessibility

class PhoneHomeExitController(
    private val phoneHomeAction: PhoneHomeAction,
    private val delayedActionScheduler: DelayedActionScheduler,
    private val onExitCompleted: () -> Unit,
) {
    fun exitToPhoneHome(): Boolean {
        if (!phoneHomeAction.exitToPhoneHome()) {
            return false
        }

        delayedActionScheduler.schedule(EXIT_DISMISS_DELAY_MS, onExitCompleted)
        return true
    }

    companion object {
        const val EXIT_DISMISS_DELAY_MS = 700L
    }
}

fun interface PhoneHomeAction {
    fun exitToPhoneHome(): Boolean
}

fun interface DelayedActionScheduler {
    fun schedule(
        delayMillis: Long,
        action: () -> Unit,
    )
}
