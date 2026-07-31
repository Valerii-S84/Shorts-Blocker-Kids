package com.shortsblockerkids.application.model

data class AccessibilityProtectionState(
    val isProtectionActive: Boolean,
    private val enabledPlatformIds: Set<String>,
) {
    fun isPlatformEnabled(platformId: String): Boolean = platformId in enabledPlatformIds
}
