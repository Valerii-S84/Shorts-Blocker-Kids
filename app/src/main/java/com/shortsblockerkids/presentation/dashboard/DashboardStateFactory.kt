package com.shortsblockerkids.presentation.dashboard

import androidx.annotation.StringRes
import com.shortsblockerkids.R
import com.shortsblockerkids.application.model.EntitlementState
import com.shortsblockerkids.domain.entitlement.EntitlementPolicy
import com.shortsblockerkids.domain.entitlement.FreeTestState
import com.shortsblockerkids.domain.protection.ProtectionEligibilityPolicy
import com.shortsblockerkids.domain.protection.ProtectionMode

object DashboardStateFactory {
    fun create(input: DashboardStateInput): DashboardUiState {
        val platforms = DashboardPlatformUiMapper.create(input)
        val policy = DashboardPolicyStateResolver.resolve(input, platforms)
        val hasProtectedPlatforms =
            platforms.protected.any(ProtectedPlatformItemUiModel::isSelected)

        return DashboardUiState(
            protection = createProtectionModel(input, policy),
            setup = createSetupModel(input, hasProtectedPlatforms),
            platforms = platforms,
            entitlement = createEntitlementModel(input, policy),
            billing =
                DashboardBillingUiModel(
                    uiState = input.billing.uiState,
                    hasEntitlement = policy.hasBillingEntitlement,
                    entitlementStateName = input.billing.entitlementStateName,
                ),
            actions = createActionsModel(input),
            warnings =
                DashboardWarningUiMapper.create(
                    input = input,
                    policy = policy,
                    hasProtectedPlatforms = hasProtectedPlatforms,
                ),
        )
    }

    private fun createProtectionModel(
        input: DashboardStateInput,
        policy: DashboardPolicyState,
    ): DashboardProtectionUiModel {
        val isLocked = policy.entitlementState == EntitlementState.PROTECTION_LOCKED
        val hasProtectionEntitlement =
            EntitlementPolicy.hasProtectionEntitlement(
                policy.entitlement,
                input.runtime.nowMillis,
            )
        val canProtect =
            input.protection.modeName == ProtectionMode.BLOCK_SHORTS.name &&
                ProtectionEligibilityPolicy.canProtect(
                    configuration = policy.configuration,
                    hasProtectionEntitlement = hasProtectionEntitlement,
                    nowMillis = input.runtime.nowMillis,
                )
        return DashboardProtectionUiModel(
            isEnabled = input.protection.isEnabled,
            canProtect = canProtect,
            isActive = policy.entitlementState == EntitlementState.PROTECTION_ACTIVE,
            isLocked = isLocked,
            switchStatusRes = protectionSwitchStatus(input.protection.isEnabled, isLocked),
            protectionStatusRes = policy.entitlementState.protectionStatusRes(),
            modeName = input.protection.modeName,
        )
    }

    private fun createSetupModel(
        input: DashboardStateInput,
        hasProtectedPlatforms: Boolean,
    ): DashboardSetupUiModel =
        DashboardSetupUiModel(
            isPinConfigured = input.protection.isPinConfigured,
            hasProtectedPlatforms = hasProtectedPlatforms,
            isAccessibilityDisclosureAccepted =
                input.protection.isAccessibilityDisclosureAccepted,
            isAccessibilityServiceEnabled = input.runtime.isAccessibilityServiceEnabled,
            isTamperProtectionEnabled = input.runtime.isTamperProtectionEnabled,
        )

    private fun createEntitlementModel(
        input: DashboardStateInput,
        policy: DashboardPolicyState,
    ): DashboardEntitlementUiModel =
        DashboardEntitlementUiModel(
            freeTestStatusRes = policy.freeTestState.statusRes(),
            freeTestDaysRemaining =
                EntitlementPolicy.freeTestDaysRemaining(
                    policy.entitlement,
                    input.runtime.nowMillis,
                ),
            isFreeTestStarted = input.entitlement.freeTestStartedAtMillis != null,
            resolvedStateName = policy.entitlementState.name,
        )

    private fun createActionsModel(input: DashboardStateInput): DashboardActionsUiModel =
        DashboardActionsUiModel(
            accessibilitySettingsLabelRes =
                if (input.protection.isAccessibilityDisclosureAccepted) {
                    R.string.dashboard_open_accessibility_settings
                } else {
                    R.string.dashboard_review_accessibility_disclosure
                },
            tamperProtectionLabelRes =
                if (input.runtime.isTamperProtectionEnabled) {
                    R.string.dashboard_review_tamper_protection
                } else {
                    R.string.dashboard_enable_tamper_protection
                },
        )

    @StringRes
    private fun protectionSwitchStatus(
        isEnabled: Boolean,
        isLocked: Boolean,
    ): Int =
        when {
            isLocked -> R.string.status_locked
            isEnabled -> R.string.status_on
            else -> R.string.status_off
        }

    @StringRes
    private fun FreeTestState.statusRes(): Int =
        when (this) {
            FreeTestState.NOT_STARTED -> R.string.status_not_started
            FreeTestState.ACTIVE -> R.string.status_free_test_active
            FreeTestState.EXPIRED -> R.string.status_free_test_expired
        }

    @StringRes
    private fun EntitlementState.protectionStatusRes(): Int =
        when (this) {
            EntitlementState.PROTECTION_ACTIVE -> R.string.status_protection_active
            EntitlementState.PROTECTION_PERMISSION_MISSING ->
                R.string.status_protection_permission_missing
            EntitlementState.PROTECTION_LOCKED -> R.string.status_protection_locked
            EntitlementState.FREE_TEST_ACTIVE,
            EntitlementState.SUBSCRIPTION_ACTIVE,
            -> R.string.status_inactive
            EntitlementState.FREE_TEST_NOT_STARTED -> R.string.status_not_started
            EntitlementState.FREE_TEST_EXPIRED -> R.string.status_free_test_expired
        }
}
