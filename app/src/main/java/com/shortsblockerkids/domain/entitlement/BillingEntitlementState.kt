package com.shortsblockerkids.domain.entitlement

enum class BillingEntitlementState {
    ACTIVE,
    PENDING,
    INACTIVE,
}

fun BillingEntitlementState.allowsPaidProtection(): Boolean = this == BillingEntitlementState.ACTIVE
