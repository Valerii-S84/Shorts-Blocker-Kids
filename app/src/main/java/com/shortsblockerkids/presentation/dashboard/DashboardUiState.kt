package com.shortsblockerkids.presentation.dashboard

import androidx.annotation.StringRes
import com.shortsblockerkids.core.billing.BillingUiState

data class DashboardUiState(
    val protection: DashboardProtectionUiModel,
    val setup: DashboardSetupUiModel,
    val platforms: DashboardPlatformsUiModel,
    val entitlement: DashboardEntitlementUiModel,
    val billing: DashboardBillingUiModel,
    val actions: DashboardActionsUiModel,
    val warnings: List<DashboardWarningUiModel>,
)

data class DashboardProtectionUiModel(
    val isEnabled: Boolean,
    val canProtect: Boolean,
    val isActive: Boolean,
    val isLocked: Boolean,
    @param:StringRes val switchStatusRes: Int,
    @param:StringRes val protectionStatusRes: Int,
    val modeName: String,
)

data class DashboardSetupUiModel(
    val isPinConfigured: Boolean,
    val hasProtectedPlatforms: Boolean,
    val isAccessibilityDisclosureAccepted: Boolean,
    val isAccessibilityServiceEnabled: Boolean,
    val isTamperProtectionEnabled: Boolean,
)

data class DashboardPlatformsUiModel(
    val protected: List<ProtectedPlatformItemUiModel>,
    val unsupported: List<ProtectedPlatformItemUiModel>,
)

data class DashboardEntitlementUiModel(
    @param:StringRes val freeTestStatusRes: Int,
    val freeTestDaysRemaining: Int?,
    val isFreeTestStarted: Boolean,
    val resolvedStateName: String,
)

data class DashboardBillingUiModel(
    val uiState: BillingUiState,
    val hasEntitlement: Boolean,
    val entitlementStateName: String,
)

data class DashboardActionsUiModel(
    @param:StringRes val accessibilitySettingsLabelRes: Int,
    @param:StringRes val tamperProtectionLabelRes: Int,
)

enum class DashboardWarningUiModel {
    FREE_TEST_ENDED,
    PROTECTION_PERMISSION_MISSING,
    NO_PROTECTED_APPS_SELECTED,
    FREE_TEST_EXPIRED,
    PROTECTION_DISABLED,
    NO_PROTECTED_APPS,
    ACCESSIBILITY_DISABLED,
    TEMPORARY_ALLOW_ACTIVE,
    FREE_TEST_NOT_STARTED,
    SETUP_INCOMPLETE,
}
