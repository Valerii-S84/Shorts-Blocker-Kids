package com.shortsblockerkids.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.platform.accessibility.AccessibilityServiceRuntime

class ShortsBlockerAccessibilityService : AccessibilityService() {
    private val runtime = AccessibilityServiceRuntime(this)

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
