package com.shortsblockerkids.domain.protection

import com.shortsblockerkids.domain.detection.SupportedPlatform

data class ProtectionConfiguration(
    val isEnabled: Boolean = true,
    val isAccessibilityDisclosureAccepted: Boolean = false,
    val mode: ProtectionMode = ProtectionMode.BLOCK_SHORTS,
    val enabledPlatformIds: Set<String> = DEFAULT_ENABLED_PLATFORM_IDS,
    val temporaryAllowUntilMillis: Long? = null,
    val isPinConfigured: Boolean = false,
) {
    val hasEnabledPlatforms: Boolean
        get() = enabledPlatformIds.isNotEmpty()

    fun isPlatformEnabled(platformId: String): Boolean = platformId in enabledPlatformIds

    companion object {
        val DEFAULT_ENABLED_PLATFORM_IDS =
            SupportedPlatform.PROTECTED_PLATFORMS
                .mapTo(linkedSetOf()) { platform -> platform.id }
    }
}
