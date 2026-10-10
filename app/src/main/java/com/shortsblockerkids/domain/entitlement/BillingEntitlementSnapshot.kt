package com.shortsblockerkids.domain.entitlement

data class BillingEntitlementSnapshot(
    val state: BillingEntitlementState,
) {
    constructor(isActive: Boolean) : this(
        state = if (isActive) BillingEntitlementState.ACTIVE else BillingEntitlementState.INACTIVE,
    )

    val isActive: Boolean
        get() = state.allowsPaidProtection()
}
