package com.shortsblockerkids.presentation.dashboard

import androidx.annotation.StringRes
import com.shortsblockerkids.R
import com.shortsblockerkids.application.model.EntitlementState
import com.shortsblockerkids.application.protection.LocalEntitlementInput
import com.shortsblockerkids.application.protection.LocalEntitlementResolver
import com.shortsblockerkids.core.billing.BillingUiState
import com.shortsblockerkids.domain.entitlement.EntitlementPolicy
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestState
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionEligibilityPolicy
import com.shortsblockerkids.domain.protection.ProtectionMode
import com.shortsblockerkids.domain.protection.TemporaryAllowPolicy

object DashboardStateFactory {
    data class Input(
        val protection: ProtectionInput,
        val entitlement: EntitlementInput,
        val billing: BillingInput,
        val platforms: List<PlatformInput>,
        val runtime: RuntimeInput,
    )

    data class ProtectionInput(
        val isEnabled: Boolean,
        val isAccessibilityDisclosureAccepted: Boolean,
        val modeName: String,
        val enabledPlatformIds: Set<String>,
        val temporaryAllowUntilMillis: Long?,
        val isPinConfigured: Boolean,
    )

    data class EntitlementInput(
        val freeTestStartedAtMillis: Long?,
        val freeTestDurationDays: Int,
        val isPaidProtectionAllowed: Boolean,
        val paidLastVerifiedAtMillis: Long?,
        val paidActiveUntilMillis: Long?,
    )

    data class BillingInput(
        val uiState: BillingUiState,
        val entitlementStateName: String,
    )

    data class PlatformInput(
        val platformId: String,
        @param:StringRes val nameRes: Int,
        val packageName: String,
        val supportStatusName: String,
    )

    data class RuntimeInput(
        val isAccessibilityServiceEnabled: Boolean,
        val isTamperProtectionEnabled: Boolean,
        val nowMillis: Long,
    )

    fun create(input: Input): DashboardUiState {
        val platformModels = createPlatformModels(input)
        val configuration = createPolicyConfiguration(input, platformModels)
        val entitlement = input.entitlement.toSnapshot()
        val freeTestState = EntitlementPolicy.freeTestState(entitlement, input.runtime.nowMillis)
        val hasBillingEntitlement =
            EntitlementPolicy.hasPaidEntitlement(entitlement, input.runtime.nowMillis)
        val entitlementState = resolveEntitlementState(input, configuration, entitlement)
        val isTemporarilyAllowed =
            TemporaryAllowPolicy.isActive(
                input.protection.temporaryAllowUntilMillis,
                input.runtime.nowMillis,
            )
        val hasProtectedPlatforms = platformModels.protected.any { item -> item.isSelected }

        return DashboardUiState(
            protection =
                createProtectionModel(
                    input = input,
                    configuration = configuration,
                    entitlement = entitlement,
                    entitlementState = entitlementState,
                ),
            setup = createSetupModel(input, hasProtectedPlatforms),
            platforms = platformModels,
            entitlement = createEntitlementModel(input, entitlement, freeTestState, entitlementState),
            billing =
                DashboardBillingUiModel(
                    uiState = input.billing.uiState,
                    hasEntitlement = hasBillingEntitlement,
                    entitlementStateName = input.billing.entitlementStateName,
                ),
            actions = createActionsModel(input),
            warnings =
                createWarnings(
                    input = input,
                    freeTestState = freeTestState,
                    hasBillingEntitlement = hasBillingEntitlement,
                    hasProtectedPlatforms = hasProtectedPlatforms,
                    isTemporarilyAllowed = isTemporarilyAllowed,
                    isProtectionActive = entitlementState == EntitlementState.PROTECTION_ACTIVE,
                ),
        )
    }

    private fun createPlatformModels(input: Input): DashboardPlatformsUiModel {
        val items =
            input.platforms.map { platform ->
                val support = platform.supportStatusName.toPlatformSupport()
                ProtectedPlatformItemUiModel(
                    platformId = platform.platformId,
                    nameRes = platform.nameRes,
                    packageName = platform.packageName,
                    statusRes = support.statusRes,
                    isSupported = support.isSupported,
                    isSelected =
                        support.isSupported &&
                            platform.platformId in input.protection.enabledPlatformIds,
                    isEnabled = support.isSupported,
                )
            }
        return DashboardPlatformsUiModel(
            protected = items.filter(ProtectedPlatformItemUiModel::isSupported),
            unsupported = items.filterNot(ProtectedPlatformItemUiModel::isSupported),
        )
    }

    private fun createPolicyConfiguration(
        input: Input,
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

    private fun EntitlementInput.toSnapshot(): EntitlementSnapshot =
        EntitlementSnapshot(
            freeTestStartedAtMillis = freeTestStartedAtMillis,
            freeTestDurationDays = freeTestDurationDays,
            isPaidProtectionAllowed = isPaidProtectionAllowed,
            paidLastVerifiedAtMillis = paidLastVerifiedAtMillis,
            paidActiveUntilMillis = paidActiveUntilMillis,
        )

    private fun resolveEntitlementState(
        input: Input,
        configuration: ProtectionConfiguration,
        entitlement: EntitlementSnapshot,
    ): EntitlementState =
        LocalEntitlementResolver.resolve(
            LocalEntitlementInput(
                protectionConfiguration = configuration,
                entitlement = entitlement,
                isProtectionPermissionGranted = input.runtime.isAccessibilityServiceEnabled,
                nowMillis = input.runtime.nowMillis,
            ),
        )

    private fun createProtectionModel(
        input: Input,
        configuration: ProtectionConfiguration,
        entitlement: EntitlementSnapshot,
        entitlementState: EntitlementState,
    ): DashboardProtectionUiModel {
        val isLocked = entitlementState == EntitlementState.PROTECTION_LOCKED
        val hasProtectionEntitlement =
            EntitlementPolicy.hasProtectionEntitlement(entitlement, input.runtime.nowMillis)
        val canProtect =
            input.protection.modeName == ProtectionMode.BLOCK_SHORTS.name &&
                ProtectionEligibilityPolicy.canProtect(
                    configuration = configuration,
                    hasProtectionEntitlement = hasProtectionEntitlement,
                    nowMillis = input.runtime.nowMillis,
                )
        return DashboardProtectionUiModel(
            isEnabled = input.protection.isEnabled,
            canProtect = canProtect,
            isActive = entitlementState == EntitlementState.PROTECTION_ACTIVE,
            isLocked = isLocked,
            switchStatusRes = protectionSwitchStatus(input.protection.isEnabled, isLocked),
            protectionStatusRes = entitlementState.protectionStatusRes(),
            modeName = input.protection.modeName,
        )
    }

    private fun createSetupModel(
        input: Input,
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
        input: Input,
        entitlement: EntitlementSnapshot,
        freeTestState: FreeTestState,
        entitlementState: EntitlementState,
    ): DashboardEntitlementUiModel =
        DashboardEntitlementUiModel(
            freeTestStatusRes = freeTestState.statusRes(),
            freeTestDaysRemaining =
                EntitlementPolicy.freeTestDaysRemaining(entitlement, input.runtime.nowMillis),
            isFreeTestStarted = input.entitlement.freeTestStartedAtMillis != null,
            resolvedStateName = entitlementState.name,
        )

    private fun createActionsModel(input: Input): DashboardActionsUiModel =
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

    private fun createWarnings(
        input: Input,
        freeTestState: FreeTestState,
        hasBillingEntitlement: Boolean,
        hasProtectedPlatforms: Boolean,
        isTemporarilyAllowed: Boolean,
        isProtectionActive: Boolean,
    ): List<DashboardWarningUiModel> =
        buildList {
            if (freeTestState == FreeTestState.EXPIRED && !hasBillingEntitlement) {
                add(DashboardWarningUiModel.FREE_TEST_ENDED)
            }
            if (!input.runtime.isAccessibilityServiceEnabled) {
                add(DashboardWarningUiModel.PROTECTION_PERMISSION_MISSING)
            }
            if (!hasProtectedPlatforms) {
                add(DashboardWarningUiModel.NO_PROTECTED_APPS_SELECTED)
            }
            if (!isProtectionActive) {
                add(
                    inactiveWarning(
                        input = input,
                        freeTestState = freeTestState,
                        hasBillingEntitlement = hasBillingEntitlement,
                        hasProtectedPlatforms = hasProtectedPlatforms,
                        isTemporarilyAllowed = isTemporarilyAllowed,
                    ),
                )
            }
        }

    private fun inactiveWarning(
        input: Input,
        freeTestState: FreeTestState,
        hasBillingEntitlement: Boolean,
        hasProtectedPlatforms: Boolean,
        isTemporarilyAllowed: Boolean,
    ): DashboardWarningUiModel =
        when {
            freeTestState == FreeTestState.EXPIRED && !hasBillingEntitlement ->
                DashboardWarningUiModel.FREE_TEST_EXPIRED
            !input.protection.isEnabled -> DashboardWarningUiModel.PROTECTION_DISABLED
            !hasProtectedPlatforms -> DashboardWarningUiModel.NO_PROTECTED_APPS
            !input.runtime.isAccessibilityServiceEnabled ->
                DashboardWarningUiModel.ACCESSIBILITY_DISABLED
            isTemporarilyAllowed -> DashboardWarningUiModel.TEMPORARY_ALLOW_ACTIVE
            freeTestState == FreeTestState.NOT_STARTED ->
                DashboardWarningUiModel.FREE_TEST_NOT_STARTED
            else -> DashboardWarningUiModel.SETUP_INCOMPLETE
        }

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

    private fun String.toPlatformSupport(): PlatformSupport =
        when (this) {
            PLATFORM_SUPPORTED ->
                PlatformSupport(
                    isSupported = true,
                    statusRes = R.string.platform_status_supported,
                )
            PLATFORM_SUPPORTED_NEEDS_QA ->
                PlatformSupport(
                    isSupported = true,
                    statusRes = R.string.platform_status_supported_needs_qa,
                )
            else ->
                PlatformSupport(
                    isSupported = false,
                    statusRes = R.string.platform_status_not_supported,
                )
        }

    private data class PlatformSupport(
        val isSupported: Boolean,
        @param:StringRes val statusRes: Int,
    )

    private const val PLATFORM_SUPPORTED = "SUPPORTED"
    private const val PLATFORM_SUPPORTED_NEEDS_QA =
        "SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA"
}
