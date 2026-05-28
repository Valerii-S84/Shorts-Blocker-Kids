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

    fun localPurchaseStatusMessage(
        hasPurchasedSubscription: Boolean,
        hasPendingSubscription: Boolean,
    ): String =
        when {
            hasPurchasedSubscription && canUseClientOnlyEntitlement -> "Subscription active."
            hasPurchasedSubscription ->
                "Subscription requires backend verification. Restore or try again."
            hasPendingSubscription ->
                "Purchase pending. Protection unlocks after payment completes."
            else -> "No active Google Play subscription found."
        }
}
