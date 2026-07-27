package com.shortsblockerkids.core.billing

data class BillingVerificationPolicy(
    val clientOnlyModeRequested: Boolean = false,
    val internalTestingBuild: Boolean = false,
) {
    val canUseClientOnlyEntitlement: Boolean
        get() = clientOnlyModeRequested && internalTestingBuild

    fun localPurchaseSnapshot(
        hasPurchasedSubscription: Boolean,
        checkedAtMillis: Long,
    ): BillingEntitlementSnapshot =
        BillingEntitlementSnapshot(
            state =
                if (hasPurchasedSubscription && canUseClientOnlyEntitlement) {
                    BillingEntitlementState.ACTIVE
                } else {
                    BillingEntitlementState.EXPIRED
                },
            checkedAtMillis = checkedAtMillis,
        )

    fun failClosedSnapshot(checkedAtMillis: Long): BillingEntitlementSnapshot =
        BillingEntitlementSnapshot(
            state = BillingEntitlementState.UNKNOWN,
            checkedAtMillis = checkedAtMillis,
        )

    fun localPurchaseMessageCode(
        hasPurchasedSubscription: Boolean,
        hasPendingSubscription: Boolean,
    ): BillingMessageCode =
        when {
            hasPurchasedSubscription && canUseClientOnlyEntitlement ->
                BillingMessageCode.SUBSCRIPTION_ACTIVE
            hasPurchasedSubscription ->
                BillingMessageCode.BACKEND_VERIFICATION_REQUIRED
            hasPendingSubscription ->
                BillingMessageCode.PURCHASE_PENDING
            else -> BillingMessageCode.NO_ACTIVE_SUBSCRIPTION
        }
}
