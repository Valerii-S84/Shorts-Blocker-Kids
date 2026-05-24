package com.shortsblockerkids.core.billing

enum class BillingEntitlementState {
    UNKNOWN,
    ACTIVE,
    CANCELED_ACTIVE,
    PENDING,
    IN_GRACE,
    ON_HOLD,
    EXPIRED,
    REVOKED,
}

fun BillingEntitlementState.allowsPaidProtection(): Boolean =
    this == BillingEntitlementState.ACTIVE ||
        this == BillingEntitlementState.CANCELED_ACTIVE ||
        this == BillingEntitlementState.IN_GRACE
