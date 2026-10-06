package com.shortsblockerkids.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.infrastructure.storage.DataStoreSettingsStore
import com.shortsblockerkids.infrastructure.time.SystemTimeProvider
import com.shortsblockerkids.infrastructure.time.ElapsedRealtimeProvider
import com.shortsblockerkids.platform.accessibility.AccessibilityServiceRuntime

class ShortsBlockerAccessibilityService : AccessibilityService() {
    private val settingsStore by lazy { DataStoreSettingsStore(this) }
    private val runtime by lazy {
        AccessibilityServiceRuntime(
            service = this,
            settingsStatePort = settingsStore,
            temporaryAllowStore = settingsStore,
            timeProvider = SystemTimeProvider(),
            intervalClock = ElapsedRealtimeProvider(),
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        runtime.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        runtime.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        runtime.onInterrupt()
    }

    override fun onDestroy() {
        runtime.onDestroy()
        super.onDestroy()
    }
}
