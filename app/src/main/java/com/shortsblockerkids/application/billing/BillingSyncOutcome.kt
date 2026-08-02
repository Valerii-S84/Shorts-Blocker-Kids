package com.shortsblockerkids.application.billing

import com.shortsblockerkids.core.billing.BillingEntitlementSnapshot
import com.shortsblockerkids.core.billing.BillingMessageCode

data class BillingSyncOutcome(
    val entitlementSnapshot: BillingEntitlementSnapshot,
    val messageCodeToApply: BillingMessageCode?,
    val clearsLoading: Boolean = true,
    val purchaseTokensToAcknowledge: List<String> = emptyList(),
)
