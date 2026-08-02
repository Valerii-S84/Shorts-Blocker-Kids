package com.shortsblockerkids.core.storage

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.core.billing.allowsPaidProtection
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.protection.ProtectionConfiguration

internal fun AppSettings.toSnapshot(): AppSettingsSnapshot =
    AppSettingsSnapshot(
        protectionConfiguration =
            ProtectionConfiguration(
                isEnabled = protectionEnabled,
                isAccessibilityDisclosureAccepted = accessibilityDisclosureAccepted,
                mode = selectedMode,
                enabledPlatformIds = enabledPlatformIds,
                temporaryAllowUntilMillis = temporaryAllowUntil,
                isPinConfigured = isPinCreated,
            ),
        entitlement =
            EntitlementSnapshot(
                freeTestStartedAtMillis = freeTestStartedAt,
                freeTestDurationDays = freeTestDurationDays,
                isPaidProtectionAllowed = billingEntitlementState.allowsPaidProtection(),
                paidLastVerifiedAtMillis = billingLastVerifiedAt,
                paidActiveUntilMillis = billingActiveUntilMillis,
            ),
        billingEntitlementStateName = billingEntitlementState.name,
    )
