package com.shortsblockerkids.platform.accessibility

import com.shortsblockerkids.application.port.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Rechecks the current window when a persisted temporary allowance expires. */
internal class TemporaryAllowDeadlineScheduler(
    private val scope: CoroutineScope,
    private val timeProvider: TimeProvider,
    private val onDeadline: () -> Unit,
    private val awaitDelay: suspend (Long) -> Unit = { delay(it) },
) {
    private var deadlineJob: Job? = null

    fun schedule(allowUntilMillis: Long?) {
        cancel()
        if (allowUntilMillis == null) return
        deadlineJob = scope.launch {
            while (true) {
                val nowMillis = timeProvider.currentTimeMillis()
                if (allowUntilMillis <= nowMillis) {
                    onDeadline()
                    break
                }
                // Recheck the persisted wall-clock deadline after waking: clock changes
                // must not prematurely end an allowance that is still active.
                awaitDelay(allowUntilMillis - nowMillis)
            }
        }
    }

    fun cancel() {
        deadlineJob?.cancel()
        deadlineJob = null
    }
}
