package com.shortsblockerkids.application.billing

import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot

data class BillingSyncOutcome(
    val entitlementSnapshot: BillingEntitlementSnapshot,
    val hasPendingSubscription: Boolean,
    val purchaseTokensToAcknowledge: List<String> = emptyList(),
)
