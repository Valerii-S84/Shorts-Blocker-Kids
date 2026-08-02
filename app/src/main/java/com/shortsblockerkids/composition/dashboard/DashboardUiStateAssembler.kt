package com.shortsblockerkids.composition.dashboard

import com.shortsblockerkids.accessibility.PlatformSupportEntry
import com.shortsblockerkids.accessibility.PlatformSupportMatrix
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.core.billing.BillingUiState
import com.shortsblockerkids.presentation.dashboard.DashboardBillingInput
import com.shortsblockerkids.presentation.dashboard.DashboardEntitlementInput
import com.shortsblockerkids.presentation.dashboard.DashboardPlatformInput
import com.shortsblockerkids.presentation.dashboard.DashboardProtectionInput
import com.shortsblockerkids.presentation.dashboard.DashboardRuntimeInput
import com.shortsblockerkids.presentation.dashboard.DashboardStateFactory
import com.shortsblockerkids.presentation.dashboard.DashboardStateInput
import com.shortsblockerkids.presentation.dashboard.DashboardUiState

internal class DashboardUiStateAssembler(
    private val timeProvider: TimeProvider,
    private val platformEntries: List<PlatformSupportEntry> = PlatformSupportMatrix.entries,
) {
    fun create(
        settings: AppSettingsSnapshot,
        billingUiState: BillingUiState,
        isAccessibilityServiceEnabled: Boolean,
        isTamperProtectionEnabled: Boolean,
    ): DashboardUiState =
        DashboardStateFactory.create(
            DashboardStateInput(
                protection =
                    DashboardProtectionInput(
                        isEnabled = settings.protectionConfiguration.isEnabled,
                        isAccessibilityDisclosureAccepted =
                            settings.protectionConfiguration
                                .isAccessibilityDisclosureAccepted,
                        modeName = settings.protectionConfiguration.mode.name,
                        enabledPlatformIds =
                            settings.protectionConfiguration.enabledPlatformIds,
                        temporaryAllowUntilMillis =
                            settings.protectionConfiguration.temporaryAllowUntilMillis,
                        isPinConfigured = settings.protectionConfiguration.isPinConfigured,
                    ),
                entitlement =
                    DashboardEntitlementInput(
                        freeTestStartedAtMillis = settings.entitlement.freeTestStartedAtMillis,
                        freeTestDurationDays = settings.entitlement.freeTestDurationDays,
                        isPaidProtectionAllowed = settings.entitlement.isPaidProtectionAllowed,
                        paidLastVerifiedAtMillis = settings.entitlement.paidLastVerifiedAtMillis,
                        paidActiveUntilMillis = settings.entitlement.paidActiveUntilMillis,
                    ),
                billing =
                    DashboardBillingInput(
                        uiState = billingUiState,
                        entitlementStateName = settings.billingEntitlementStateName,
                    ),
                platforms = platformEntries.map { entry -> entry.toDashboardInput() },
                runtime =
                    DashboardRuntimeInput(
                        isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                        isTamperProtectionEnabled = isTamperProtectionEnabled,
                        nowMillis = timeProvider.currentTimeMillis(),
                    ),
            ),
        )

    private fun PlatformSupportEntry.toDashboardInput(): DashboardPlatformInput =
        DashboardPlatformInput(
            platformId = platformId,
            nameRes = platformNameRes,
            packageName = packageName,
            supportStatusName = status.name,
        )
}
