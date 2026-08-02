package com.shortsblockerkids.presentation.dashboard

import com.shortsblockerkids.application.model.EntitlementState
import com.shortsblockerkids.application.protection.LocalEntitlementInput
import com.shortsblockerkids.application.protection.LocalEntitlementResolver
import com.shortsblockerkids.domain.entitlement.EntitlementPolicy
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestState
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionMode
import com.shortsblockerkids.domain.protection.TemporaryAllowPolicy

internal object DashboardPolicyStateResolver {
    fun resolve(
        input: DashboardStateInput,
        platforms: DashboardPlatformsUiModel,
    ): DashboardPolicyState {
        val configuration = createConfiguration(input, platforms)
        val entitlement = input.entitlement.toSnapshot()
        return DashboardPolicyState(
            configuration = configuration,
            entitlement = entitlement,
            freeTestState = EntitlementPolicy.freeTestState(entitlement, input.runtime.nowMillis),
            hasBillingEntitlement =
                EntitlementPolicy.hasPaidEntitlement(entitlement, input.runtime.nowMillis),
            entitlementState =
                LocalEntitlementResolver.resolve(
                    LocalEntitlementInput(
                        protectionConfiguration = configuration,
                        entitlement = entitlement,
                        isProtectionPermissionGranted =
                            input.runtime.isAccessibilityServiceEnabled,
                        nowMillis = input.runtime.nowMillis,
                    ),
                ),
            isTemporarilyAllowed =
                TemporaryAllowPolicy.isActive(
                    input.protection.temporaryAllowUntilMillis,
                    input.runtime.nowMillis,
                ),
        )
    }

    private fun createConfiguration(
        input: DashboardStateInput,
        platforms: DashboardPlatformsUiModel,
    ): ProtectionConfiguration {
        val isModeSupported = input.protection.modeName == ProtectionMode.BLOCK_SHORTS.name
        return ProtectionConfiguration(
            isEnabled = input.protection.isEnabled && isModeSupported,
            isAccessibilityDisclosureAccepted =
                input.protection.isAccessibilityDisclosureAccepted,
            mode = ProtectionMode.BLOCK_SHORTS,
            enabledPlatformIds =
                platforms.protected
                    .filter(ProtectedPlatformItemUiModel::isSelected)
                    .mapTo(linkedSetOf(), ProtectedPlatformItemUiModel::platformId),
            temporaryAllowUntilMillis = input.protection.temporaryAllowUntilMillis,
            isPinConfigured = input.protection.isPinConfigured,
        )
    }

    private fun DashboardEntitlementInput.toSnapshot(): EntitlementSnapshot =
        EntitlementSnapshot(
            freeTestStartedAtMillis = freeTestStartedAtMillis,
            freeTestDurationDays = freeTestDurationDays,
            isPaidProtectionAllowed = isPaidProtectionAllowed,
            paidLastVerifiedAtMillis = paidLastVerifiedAtMillis,
            paidActiveUntilMillis = paidActiveUntilMillis,
        )
}

internal data class DashboardPolicyState(
    val configuration: ProtectionConfiguration,
    val entitlement: EntitlementSnapshot,
    val freeTestState: FreeTestState,
    val hasBillingEntitlement: Boolean,
    val entitlementState: EntitlementState,
    val isTemporarilyAllowed: Boolean,
)
