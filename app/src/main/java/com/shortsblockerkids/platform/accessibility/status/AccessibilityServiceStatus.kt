package com.shortsblockerkids.platform.accessibility.status

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.shortsblockerkids.accessibility.ShortsBlockerAccessibilityService

object AccessibilityServiceStatus {
    fun isEnabled(context: Context): Boolean {
        val expectedName =
            ComponentName(
                context,
                ShortsBlockerAccessibilityService::class.java,
            ).flattenToString()

        val enabledServices =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false

        return enabledServices
            .split(':')
            .any { it.equals(expectedName, ignoreCase = true) }
    }
}
