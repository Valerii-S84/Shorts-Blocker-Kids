package com.shortsblockerkids.application.port

fun interface PinLockoutClock {
    fun read(): PinLockoutTime
}

data class PinLockoutTime(
    val elapsedRealtimeMillis: Long,
    val bootCount: Int,
)
