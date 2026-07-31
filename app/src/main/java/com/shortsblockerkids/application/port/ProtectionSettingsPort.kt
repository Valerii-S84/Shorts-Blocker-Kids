package com.shortsblockerkids.application.port

import com.shortsblockerkids.application.model.AccessibilityProtectionState

fun interface ProtectionSettingsPort {
    fun protectionState(nowMillis: Long): AccessibilityProtectionState
}
