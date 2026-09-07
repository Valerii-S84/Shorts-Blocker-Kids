package com.shortsblockerkids.infrastructure.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.shortsblockerkids.application.port.PinLockoutClock
import com.shortsblockerkids.application.port.PinLockoutTime

class AndroidPinLockoutClock(
    context: Context,
) : PinLockoutClock {
    private val contentResolver = context.applicationContext.contentResolver

    override fun read(): PinLockoutTime =
        PinLockoutTime(
            elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            bootCount = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT),
        )
}
