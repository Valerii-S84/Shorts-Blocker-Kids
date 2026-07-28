package com.shortsblockerkids.domain.protection

object ProtectionActivationPolicy {
    fun shouldStartFreeTest(
        isAccessibilityServiceEnabled: Boolean,
        isProtectionEnabled: Boolean,
        isAccessibilityDisclosureAccepted: Boolean,
        isPinConfigured: Boolean,
        isFreeTestAlreadyStarted: Boolean,
    ): Boolean =
        isAccessibilityServiceEnabled &&
            isProtectionEnabled &&
            isAccessibilityDisclosureAccepted &&
            isPinConfigured &&
            !isFreeTestAlreadyStarted
}
