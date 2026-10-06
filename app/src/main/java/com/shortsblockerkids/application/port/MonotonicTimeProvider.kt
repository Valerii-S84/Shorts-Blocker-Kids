package com.shortsblockerkids.application.port

/** Time since boot, for process/session intervals only; never persist as a calendar timestamp. */
fun interface MonotonicTimeProvider {
    fun elapsedRealtimeMillis(): Long
}
