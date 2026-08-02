package com.shortsblockerkids.application.billing

import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot

data class BillingSyncOutcome(
    val entitlementSnapshot: BillingEntitlementSnapshot,
    val status: BillingSyncStatus?,
    val hasPendingSubscription: Boolean,
    val clearsLoading: Boolean = true,
    val purchaseTokensToAcknowledge: List<String> = emptyList(),
)

enum class BillingSyncStatus {
    ENTITLEMENT_RESOLVED,
    BACKEND_VERIFICATION_REQUIRED,
    BACKEND_INSTALLATION_ID_UNAVAILABLE,
    BACKEND_PURCHASE_VERIFICATION_FAILED,
    BACKEND_ENTITLEMENT_REFRESH_FAILED,
}
