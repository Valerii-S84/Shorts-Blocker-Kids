package com.shortsblockerkids.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

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
