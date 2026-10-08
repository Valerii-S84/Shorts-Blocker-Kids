package com.shortsblockerkids.domain.protection

object ProtectionEligibilityPolicy {
    fun canProtect(
        configuration: ProtectionConfiguration,
        hasProtectionEntitlement: Boolean,
        nowMillis: Long,
        elapsedRealtimeMillis: Long? = null,
    ): Boolean =
        configuration.isEnabled &&
            configuration.isAccessibilityDisclosureAccepted &&
            configuration.mode == ProtectionMode.BLOCK_SHORTS &&
            configuration.hasEnabledPlatforms &&
            hasProtectionEntitlement &&
            configuration.isPinConfigured &&
            !TemporaryAllowPolicy.isActive(configuration, nowMillis, elapsedRealtimeMillis)
}
