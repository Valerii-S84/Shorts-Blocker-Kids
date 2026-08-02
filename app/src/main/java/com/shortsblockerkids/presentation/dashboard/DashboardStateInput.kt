package com.shortsblockerkids.presentation.dashboard

import androidx.annotation.StringRes
import com.shortsblockerkids.presentation.billing.BillingUiState

data class DashboardStateInput(
    val protection: DashboardProtectionInput,
    val entitlement: DashboardEntitlementInput,
    val billing: DashboardBillingInput,
    val platforms: List<DashboardPlatformInput>,
    val runtime: DashboardRuntimeInput,
)

data class DashboardProtectionInput(
    val isEnabled: Boolean,
    val isAccessibilityDisclosureAccepted: Boolean,
    val modeName: String,
    val enabledPlatformIds: Set<String>,
    val temporaryAllowUntilMillis: Long?,
    val isPinConfigured: Boolean,
)

data class DashboardEntitlementInput(
    val freeTestStartedAtMillis: Long?,
    val freeTestDurationDays: Int,
    val isPaidProtectionAllowed: Boolean,
    val paidLastVerifiedAtMillis: Long?,
    val paidActiveUntilMillis: Long?,
)

data class DashboardBillingInput(
    val uiState: BillingUiState,
    val entitlementStateName: String,
)

data class DashboardPlatformInput(
    val platformId: String,
    @param:StringRes val nameRes: Int,
    val packageName: String,
    val supportStatusName: String,
)

data class DashboardRuntimeInput(
    val isAccessibilityServiceEnabled: Boolean,
    val isTamperProtectionEnabled: Boolean,
    val nowMillis: Long,
)
