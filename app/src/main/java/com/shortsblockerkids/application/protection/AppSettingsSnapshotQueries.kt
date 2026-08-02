package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.model.EntitlementState
import com.shortsblockerkids.domain.entitlement.EntitlementPolicy
import com.shortsblockerkids.domain.entitlement.FreeTestState
import com.shortsblockerkids.domain.protection.ProtectionEligibilityPolicy
import com.shortsblockerkids.domain.protection.TemporaryAllowPolicy

fun AppSettingsSnapshot.toLocalEntitlementInput(
    isProtectionPermissionGranted: Boolean,
    nowMillis: Long,
): LocalEntitlementInput =
    LocalEntitlementInput(
        protectionConfiguration = protectionConfiguration,
        entitlement = entitlement,
        isProtectionPermissionGranted = isProtectionPermissionGranted,
        nowMillis = nowMillis,
    )

fun AppSettingsSnapshot.freeTestState(nowMillis: Long): EntitlementState =
    when (EntitlementPolicy.freeTestState(entitlement, nowMillis)) {
        FreeTestState.NOT_STARTED -> EntitlementState.FREE_TEST_NOT_STARTED
        FreeTestState.ACTIVE -> EntitlementState.FREE_TEST_ACTIVE
        FreeTestState.EXPIRED -> EntitlementState.FREE_TEST_EXPIRED
    }

fun AppSettingsSnapshot.freeTestDaysRemaining(nowMillis: Long): Int? = EntitlementPolicy.freeTestDaysRemaining(entitlement, nowMillis)

fun AppSettingsSnapshot.hasBillingEntitlement(nowMillis: Long): Boolean = EntitlementPolicy.hasPaidEntitlement(entitlement, nowMillis)

fun AppSettingsSnapshot.canProtect(nowMillis: Long): Boolean =
    ProtectionEligibilityPolicy.canProtect(
        configuration = protectionConfiguration,
        hasProtectionEntitlement =
            EntitlementPolicy.hasProtectionEntitlement(entitlement, nowMillis),
        nowMillis = nowMillis,
    )

fun AppSettingsSnapshot.isTemporarilyAllowed(nowMillis: Long): Boolean =
    TemporaryAllowPolicy.isActive(
        protectionConfiguration.temporaryAllowUntilMillis,
        nowMillis,
    )
