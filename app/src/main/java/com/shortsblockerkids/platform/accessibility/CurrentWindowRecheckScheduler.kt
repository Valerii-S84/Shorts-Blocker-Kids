package com.shortsblockerkids.platform.accessibility

import com.shortsblockerkids.platform.accessibility.routing.AccessibilityEventPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Retries an unavailable current window without requiring another accessibility event. */
internal class CurrentWindowRecheckScheduler(
    private val scope: CoroutineScope,
    private val tryRoute: () -> Boolean,
    private val awaitDelay: suspend (Long) -> Unit = { delay(it) },
) {
    private var recheckJob: Job? = null

    fun recheckNow() {
        cancel()
        if (!tryRoute()) {
            schedule(AccessibilityEventPolicy.DEFAULT_SCAN_DEBOUNCE_MS)
        }
    }

    fun schedule(delayMillis: Long) {
        cancel()
        recheckJob = scope.launch {
            awaitDelay(delayMillis)
            // The attempt may schedule its successor; it must not cancel its own coroutine.
            recheckJob = null
            recheckNow()
        }
    }

    fun cancel() {
        recheckJob?.cancel()
        recheckJob = null
    }
}
