package com.shortsblockerkids.application.model

import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.protection.ProtectionConfiguration

data class AppSettingsSnapshot(
    val protectionConfiguration: ProtectionConfiguration = ProtectionConfiguration(),
    val entitlement: EntitlementSnapshot = EntitlementSnapshot(),
    val billingEntitlementStateName: String = "UNKNOWN",
)
