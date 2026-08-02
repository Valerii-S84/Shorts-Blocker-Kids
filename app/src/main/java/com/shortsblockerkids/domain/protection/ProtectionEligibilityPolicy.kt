package com.shortsblockerkids.domain.protection

object ProtectionEligibilityPolicy {
    fun canProtect(
        configuration: ProtectionConfiguration,
        hasProtectionEntitlement: Boolean,
        nowMillis: Long,
    ): Boolean =
        configuration.isEnabled &&
            configuration.isAccessibilityDisclosureAccepted &&
            configuration.mode == ProtectionMode.BLOCK_SHORTS &&
            configuration.hasEnabledPlatforms &&
            hasProtectionEntitlement &&
            configuration.isPinConfigured &&
            !TemporaryAllowPolicy.isActive(configuration.temporaryAllowUntilMillis, nowMillis)
}
