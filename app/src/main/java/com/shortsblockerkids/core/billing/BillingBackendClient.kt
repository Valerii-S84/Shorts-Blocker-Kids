package com.shortsblockerkids.core.billing

interface BillingBackendClient {
    val isConfigured: Boolean

    suspend fun verifyPurchase(request: BillingBackendPurchaseRequest): BillingEntitlementSnapshot

    suspend fun refreshEntitlement(installId: String): BillingEntitlementSnapshot?
}

data class BillingBackendPurchaseRequest(
    val installId: String,
    val packageName: String,
    val productId: String,
    val purchaseToken: String,
    val appVersion: String,
)

object DisabledBillingBackendClient : BillingBackendClient {
    override val isConfigured: Boolean = false

    override suspend fun verifyPurchase(request: BillingBackendPurchaseRequest): BillingEntitlementSnapshot =
        throw UnsupportedOperationException("Billing backend is not configured.")

    override suspend fun refreshEntitlement(installId: String): BillingEntitlementSnapshot? = null
}
