package com.shortsblockerkids.core.entitlement

import com.shortsblockerkids.core.model.EntitlementState
import com.shortsblockerkids.core.storage.AppSettings

object LocalEntitlementResolver {
    fun resolve(
        settings: AppSettings,
        isProtectionPermissionGranted: Boolean,
        nowMillis: Long,
    ): EntitlementState {
        val hasBillingEntitlement = settings.hasBillingEntitlement(nowMillis)
        if (
            settings.freeTestState(nowMillis) == EntitlementState.FREE_TEST_EXPIRED &&
            !hasBillingEntitlement
        ) {
            return EntitlementState.PROTECTION_LOCKED
        }

        if (!isProtectionPermissionGranted) {
            return EntitlementState.PROTECTION_PERMISSION_MISSING
        }

        if (settings.canProtect(nowMillis)) {
            return EntitlementState.PROTECTION_ACTIVE
        }

        if (hasBillingEntitlement) {
            return EntitlementState.SUBSCRIPTION_ACTIVE
        }

        return settings.freeTestState(nowMillis)
    }
}
