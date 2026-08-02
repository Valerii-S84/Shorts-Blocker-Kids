package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.model.EntitlementState
import com.shortsblockerkids.domain.entitlement.EntitlementPolicy
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestState
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.domain.protection.ProtectionEligibilityPolicy

data class LocalEntitlementInput(
    val protectionConfiguration: ProtectionConfiguration,
    val entitlement: EntitlementSnapshot,
    val isProtectionPermissionGranted: Boolean,
    val nowMillis: Long,
)

object LocalEntitlementResolver {
    fun resolve(input: LocalEntitlementInput): EntitlementState {
        val freeTestState = EntitlementPolicy.freeTestState(input.entitlement, input.nowMillis)
        val hasPaidEntitlement =
            EntitlementPolicy.hasPaidEntitlement(input.entitlement, input.nowMillis)
        if (freeTestState == FreeTestState.EXPIRED && !hasPaidEntitlement) {
            return EntitlementState.PROTECTION_LOCKED
        }

        if (!input.isProtectionPermissionGranted) {
            return EntitlementState.PROTECTION_PERMISSION_MISSING
        }

        if (
            ProtectionEligibilityPolicy.canProtect(
                configuration = input.protectionConfiguration,
                hasProtectionEntitlement =
                    EntitlementPolicy.hasProtectionEntitlement(
                        input.entitlement,
                        input.nowMillis,
                    ),
                nowMillis = input.nowMillis,
            )
        ) {
            return EntitlementState.PROTECTION_ACTIVE
        }

        return when {
            hasPaidEntitlement -> EntitlementState.SUBSCRIPTION_ACTIVE
            freeTestState == FreeTestState.NOT_STARTED -> EntitlementState.FREE_TEST_NOT_STARTED
            else -> EntitlementState.FREE_TEST_ACTIVE
        }
    }
}
