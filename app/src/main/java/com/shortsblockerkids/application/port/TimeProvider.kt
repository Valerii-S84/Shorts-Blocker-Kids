package com.shortsblockerkids.application.port

fun interface TimeProvider {
    fun currentTimeMillis(): Long
}
