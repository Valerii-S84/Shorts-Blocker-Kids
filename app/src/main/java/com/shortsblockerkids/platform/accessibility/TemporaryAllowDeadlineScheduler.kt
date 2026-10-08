package com.shortsblockerkids.platform.accessibility

import com.shortsblockerkids.application.port.MonotonicTimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Rechecks the current window when a persisted temporary allowance expires. */
internal class TemporaryAllowDeadlineScheduler(
    private val scope: CoroutineScope,
    private val monotonicTimeProvider: MonotonicTimeProvider,
    private val onDeadline: () -> Unit,
    private val awaitDelay: suspend (Long) -> Unit = { delay(it) },
) {
    private var deadlineJob: Job? = null
    private var configuredDeadline: Long? = null
    private var paused = false

    fun schedule(allowUntilMillis: Long?) {
        deadlineJob?.cancel()
        configuredDeadline = allowUntilMillis
        if (allowUntilMillis == null || paused) return
        deadlineJob = scope.launch {
            while (true) {
                val nowMillis = monotonicTimeProvider.elapsedRealtimeMillis()
                if (allowUntilMillis <= nowMillis) {
                    onDeadline()
                    break
                }
                awaitDelay(allowUntilMillis - nowMillis)
            }
        }
    }

    fun cancel() {
        deadlineJob?.cancel()
        deadlineJob = null
        configuredDeadline = null
    }

    fun pause() {
        paused = true
        deadlineJob?.cancel()
        deadlineJob = null
    }

    fun resume() {
        if (!paused) return
        paused = false
        schedule(configuredDeadline)
    }
}
