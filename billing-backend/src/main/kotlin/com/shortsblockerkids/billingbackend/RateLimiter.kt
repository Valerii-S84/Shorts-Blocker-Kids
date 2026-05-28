package com.shortsblockerkids.billingbackend

import java.util.concurrent.ConcurrentHashMap

class RateLimiter(
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val windows = ConcurrentHashMap<String, RateWindow>()

    fun allow(
        key: String,
        limitPerMinute: Int,
    ): Boolean {
        val now = nowMillis()
        val windowStart = now - (now % WINDOW_MILLIS)
        val window =
            windows.compute(key) { _, existing ->
                if (existing == null || existing.startedAtMillis != windowStart) {
                    RateWindow(windowStart, 1)
                } else {
                    existing.copy(count = existing.count + 1)
                }
            } ?: return false
        cleanup(now)
        return window.count <= limitPerMinute
    }

    private fun cleanup(nowMillis: Long) {
        val oldestAllowed = nowMillis - (WINDOW_MILLIS * 2)
        windows.entries.removeIf { it.value.startedAtMillis < oldestAllowed }
    }

    private data class RateWindow(
        val startedAtMillis: Long,
        val count: Int,
    )

    companion object {
        private const val WINDOW_MILLIS = 60_000L
    }
}
