package com.shortsblockerkids.core.billing

data class BillingEntitlementSnapshot(
    val state: BillingEntitlementState,
    val checkedAtMillis: Long,
    val activeUntilMillis: Long? = null,
) {
    constructor(
        isActive: Boolean,
        checkedAtMillis: Long,
    ) : this(
        state = if (isActive) BillingEntitlementState.ACTIVE else BillingEntitlementState.EXPIRED,
        checkedAtMillis = checkedAtMillis,
        activeUntilMillis = null,
    )

    val isActive: Boolean
        get() = state.allowsPaidProtection()
}
