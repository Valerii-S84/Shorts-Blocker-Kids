package com.shortsblockerkids.domain.entitlement

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
}
