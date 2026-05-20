package com.shortsblockerkids.core.entitlement

import com.shortsblockerkids.core.model.EntitlementState
import com.shortsblockerkids.core.storage.AppSettings

object LocalEntitlementResolver {
    fun resolve(
        settings: AppSettings,
        isProtectionPermissionGranted: Boolean,
        nowMillis: Long,
    ): EntitlementState {
        if (settings.freeTestState(nowMillis) == EntitlementState.FREE_TEST_EXPIRED) {
            return EntitlementState.PROTECTION_LOCKED
        }

        if (!isProtectionPermissionGranted) {
            return EntitlementState.PROTECTION_PERMISSION_MISSING
        }

        if (settings.canProtect(nowMillis)) {
            return EntitlementState.PROTECTION_ACTIVE
        }

        return settings.freeTestState(nowMillis)
    }
}
