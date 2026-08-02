package com.shortsblockerkids.application.billing

import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot

interface BillingVerificationPort {
    val isConfigured: Boolean

    suspend fun verifyPurchase(request: BillingVerificationRequest): BillingEntitlementSnapshot

    suspend fun refreshEntitlement(installId: String): BillingEntitlementSnapshot?
}

data class BillingVerificationRequest(
    val installId: String,
    val packageName: String,
    val productId: String,
    val purchaseToken: String,
    val appVersion: String,
)

object DisabledBillingVerificationPort : BillingVerificationPort {
    override val isConfigured: Boolean = false

    override suspend fun verifyPurchase(request: BillingVerificationRequest): BillingEntitlementSnapshot =
        throw UnsupportedOperationException("Billing backend is not configured.")

    override suspend fun refreshEntitlement(installId: String): BillingEntitlementSnapshot? = null
}
