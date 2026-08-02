package com.shortsblockerkids.presentation.dashboard

import com.shortsblockerkids.application.model.EntitlementState
import com.shortsblockerkids.domain.entitlement.FreeTestState

internal object DashboardWarningUiMapper {
    fun create(
        input: DashboardStateInput,
        policy: DashboardPolicyState,
        hasProtectedPlatforms: Boolean,
    ): List<DashboardWarningUiModel> =
        buildList {
            if (
                policy.freeTestState == FreeTestState.EXPIRED &&
                !policy.hasBillingEntitlement
            ) {
                add(DashboardWarningUiModel.FREE_TEST_ENDED)
            }
            if (!input.runtime.isAccessibilityServiceEnabled) {
                add(DashboardWarningUiModel.PROTECTION_PERMISSION_MISSING)
            }
            if (!hasProtectedPlatforms) {
                add(DashboardWarningUiModel.NO_PROTECTED_APPS_SELECTED)
            }
            if (policy.entitlementState != EntitlementState.PROTECTION_ACTIVE) {
                add(inactiveWarning(input, policy, hasProtectedPlatforms))
            }
        }

    private fun inactiveWarning(
        input: DashboardStateInput,
        policy: DashboardPolicyState,
        hasProtectedPlatforms: Boolean,
    ): DashboardWarningUiModel =
        when {
            policy.freeTestState == FreeTestState.EXPIRED &&
                !policy.hasBillingEntitlement -> DashboardWarningUiModel.FREE_TEST_EXPIRED
            !input.protection.isEnabled -> DashboardWarningUiModel.PROTECTION_DISABLED
            !hasProtectedPlatforms -> DashboardWarningUiModel.NO_PROTECTED_APPS
            !input.runtime.isAccessibilityServiceEnabled ->
                DashboardWarningUiModel.ACCESSIBILITY_DISABLED
            policy.isTemporarilyAllowed -> DashboardWarningUiModel.TEMPORARY_ALLOW_ACTIVE
            policy.freeTestState == FreeTestState.NOT_STARTED ->
                DashboardWarningUiModel.FREE_TEST_NOT_STARTED
            else -> DashboardWarningUiModel.SETUP_INCOMPLETE
        }
}
