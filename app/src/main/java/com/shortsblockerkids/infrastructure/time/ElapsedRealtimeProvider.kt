package com.shortsblockerkids.infrastructure.time

import android.os.SystemClock
import com.shortsblockerkids.application.port.MonotonicTimeProvider

class ElapsedRealtimeProvider : MonotonicTimeProvider {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
