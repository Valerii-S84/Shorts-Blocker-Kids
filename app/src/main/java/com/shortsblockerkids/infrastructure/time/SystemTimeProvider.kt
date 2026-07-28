package com.shortsblockerkids.infrastructure.time

import com.shortsblockerkids.application.port.TimeProvider

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
