package com.shortsblockerkids.application.billing

import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.core.billing.BillingBackendClient
import com.shortsblockerkids.core.billing.BillingBackendPurchaseRequest
import com.shortsblockerkids.core.billing.BillingEntitlementSnapshot
import com.shortsblockerkids.core.billing.BillingEntitlementState
import com.shortsblockerkids.core.billing.BillingMessageCode
import com.shortsblockerkids.core.billing.BillingVerificationPolicy

data class BillingSyncConfiguration(
    val installId: String?,
    val packageName: String,
    val productId: String,
    val appVersion: String,
    val clientOnlyModeRequested: Boolean,
    val internalTestingBuild: Boolean,
)

class SyncBillingEntitlementUseCase(
    private val billingBackendClient: BillingBackendClient,
    private val timeProvider: TimeProvider,
    private val configuration: BillingSyncConfiguration,
) {
    private val verificationPolicy =
        BillingVerificationPolicy(
            clientOnlyModeRequested = configuration.clientOnlyModeRequested,
            internalTestingBuild = configuration.internalTestingBuild,
        )

    fun messageCodeWhileSyncing(purchaseSummary: BillingPurchaseSummary): BillingMessageCode? =
        if (
            billingBackendClient.isConfigured &&
            purchaseSummary.hasPurchasedSubscription &&
            !configuration.installId.isNullOrBlank()
        ) {
            BillingMessageCode.VERIFYING_WITH_BACKEND
        } else {
            null
        }

    suspend operator fun invoke(purchaseSummary: BillingPurchaseSummary): BillingSyncOutcome =
        if (billingBackendClient.isConfigured) {
            syncWithBackend(purchaseSummary)
        } else {
            syncWithoutBackend(purchaseSummary)
        }

    private suspend fun syncWithBackend(purchaseSummary: BillingPurchaseSummary): BillingSyncOutcome =
        purchaseSummary.purchasedSubscriptionToken?.let { purchaseToken ->
            verifyPurchase(
                purchaseToken = purchaseToken,
                hasPendingSubscription = purchaseSummary.hasPendingSubscription,
            )
        } ?: refreshEntitlement(purchaseSummary.hasPendingSubscription)

    private suspend fun verifyPurchase(
        purchaseToken: String,
        hasPendingSubscription: Boolean,
    ): BillingSyncOutcome {
        val installId = configuration.installId
        if (installId.isNullOrBlank()) {
            return failClosedOutcome(
                messageCodeToApply = BillingMessageCode.BACKEND_VERIFICATION_NOT_READY,
                clearsLoading = false,
            )
        }

        return runCatching {
            billingBackendClient.verifyPurchase(
                BillingBackendPurchaseRequest(
                    installId = installId,
                    packageName = configuration.packageName,
                    productId = configuration.productId,
                    purchaseToken = purchaseToken,
                    appVersion = configuration.appVersion,
                ),
            )
        }.fold(
            onSuccess = { snapshot ->
                snapshot.toOutcome(hasPendingSubscription)
            },
            onFailure = {
                failClosedOutcome(BillingMessageCode.VERIFY_WITH_BACKEND_FAILED)
            },
        )
    }

    private suspend fun refreshEntitlement(hasPendingSubscription: Boolean): BillingSyncOutcome {
        val installId = configuration.installId
        if (installId.isNullOrBlank()) {
            return failClosedOutcome(
                messageCodeToApply = null,
                clearsLoading = false,
            )
        }

        return runCatching {
            billingBackendClient.refreshEntitlement(installId)
        }.fold(
            onSuccess = { snapshot ->
                val refreshedSnapshot =
                    snapshot
                        ?: BillingEntitlementSnapshot(
                            state = BillingEntitlementState.EXPIRED,
                            checkedAtMillis = timeProvider.currentTimeMillis(),
                        )
                refreshedSnapshot.toOutcome(hasPendingSubscription)
            },
            onFailure = {
                failClosedOutcome(BillingMessageCode.REFRESH_BACKEND_FAILED)
            },
        )
    }

    private fun syncWithoutBackend(purchaseSummary: BillingPurchaseSummary): BillingSyncOutcome {
        val snapshot =
            verificationPolicy.localPurchaseSnapshot(
                hasPurchasedSubscription = purchaseSummary.hasPurchasedSubscription,
                checkedAtMillis = timeProvider.currentTimeMillis(),
            )
        val acknowledgementTokens =
            if (
                purchaseSummary.hasPurchasedSubscription &&
                verificationPolicy.canUseClientOnlyEntitlement
            ) {
                purchaseSummary.unacknowledgedPurchasedSubscriptionTokens
            } else {
                emptyList()
            }

        return BillingSyncOutcome(
            entitlementSnapshot = snapshot,
            messageCodeToApply =
                verificationPolicy.localPurchaseMessageCode(
                    hasPurchasedSubscription = purchaseSummary.hasPurchasedSubscription,
                    hasPendingSubscription = purchaseSummary.hasPendingSubscription,
                ),
            purchaseTokensToAcknowledge = acknowledgementTokens,
        )
    }

    private fun failClosedOutcome(
        messageCodeToApply: BillingMessageCode?,
        clearsLoading: Boolean = true,
    ): BillingSyncOutcome =
        BillingSyncOutcome(
            entitlementSnapshot =
                verificationPolicy.failClosedSnapshot(
                    checkedAtMillis = timeProvider.currentTimeMillis(),
                ),
            messageCodeToApply = messageCodeToApply,
            clearsLoading = clearsLoading,
        )

    private fun BillingEntitlementSnapshot.toOutcome(hasPendingSubscription: Boolean): BillingSyncOutcome =
        BillingSyncOutcome(
            entitlementSnapshot = this,
            messageCodeToApply = messageCode(hasPendingSubscription),
        )

    private fun BillingEntitlementSnapshot.messageCode(hasPendingSubscription: Boolean): BillingMessageCode =
        when {
            state == BillingEntitlementState.ACTIVE -> BillingMessageCode.SUBSCRIPTION_ACTIVE
            state == BillingEntitlementState.CANCELED_ACTIVE ->
                BillingMessageCode.SUBSCRIPTION_CANCELED_ACTIVE
            state == BillingEntitlementState.IN_GRACE ->
                BillingMessageCode.SUBSCRIPTION_IN_GRACE
            state == BillingEntitlementState.PENDING || hasPendingSubscription ->
                BillingMessageCode.PURCHASE_PENDING
            state == BillingEntitlementState.ON_HOLD -> BillingMessageCode.PAYMENT_ISSUE
            state == BillingEntitlementState.REVOKED -> BillingMessageCode.SUBSCRIPTION_REVOKED
            state == BillingEntitlementState.EXPIRED -> BillingMessageCode.NO_ACTIVE_SUBSCRIPTION
            else -> BillingMessageCode.VERIFICATION_UNAVAILABLE
        }
}
