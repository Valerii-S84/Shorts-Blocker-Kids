package com.shortsblockerkids.application.billing

import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import com.shortsblockerkids.domain.entitlement.BillingVerificationPolicy

data class BillingSyncConfiguration(
    val installId: String?,
    val packageName: String,
    val productId: String,
    val appVersion: String,
    val clientOnlyModeRequested: Boolean,
    val internalTestingBuild: Boolean,
)

class SyncBillingEntitlementUseCase(
    private val billingVerificationPort: BillingVerificationPort,
    private val timeProvider: TimeProvider,
    private val configuration: BillingSyncConfiguration,
) {
    private val verificationPolicy =
        BillingVerificationPolicy(
            clientOnlyModeRequested = configuration.clientOnlyModeRequested,
            internalTestingBuild = configuration.internalTestingBuild,
        )

    fun willVerifyPurchaseWithBackend(purchaseSummary: BillingPurchaseSummary): Boolean =
        billingVerificationPort.isConfigured &&
            purchaseSummary.hasPurchasedSubscription &&
            !configuration.installId.isNullOrBlank()

    suspend operator fun invoke(purchaseSummary: BillingPurchaseSummary): BillingSyncOutcome =
        if (billingVerificationPort.isConfigured) {
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
                status = BillingSyncStatus.BACKEND_INSTALLATION_ID_UNAVAILABLE,
                hasPendingSubscription = hasPendingSubscription,
                clearsLoading = false,
            )
        }

        return runCatching {
            billingVerificationPort.verifyPurchase(
                BillingVerificationRequest(
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
                failClosedOutcome(
                    status = BillingSyncStatus.BACKEND_PURCHASE_VERIFICATION_FAILED,
                    hasPendingSubscription = hasPendingSubscription,
                )
            },
        )
    }

    private suspend fun refreshEntitlement(hasPendingSubscription: Boolean): BillingSyncOutcome {
        val installId = configuration.installId
        if (installId.isNullOrBlank()) {
            return failClosedOutcome(
                status = null,
                hasPendingSubscription = hasPendingSubscription,
                clearsLoading = false,
            )
        }

        return runCatching {
            billingVerificationPort.refreshEntitlement(installId)
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
                failClosedOutcome(
                    status = BillingSyncStatus.BACKEND_ENTITLEMENT_REFRESH_FAILED,
                    hasPendingSubscription = hasPendingSubscription,
                )
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
            status =
                if (
                    purchaseSummary.hasPurchasedSubscription &&
                    !verificationPolicy.canUseClientOnlyEntitlement
                ) {
                    BillingSyncStatus.BACKEND_VERIFICATION_REQUIRED
                } else {
                    BillingSyncStatus.ENTITLEMENT_RESOLVED
                },
            hasPendingSubscription = purchaseSummary.hasPendingSubscription,
            purchaseTokensToAcknowledge = acknowledgementTokens,
        )
    }

    private fun failClosedOutcome(
        status: BillingSyncStatus?,
        hasPendingSubscription: Boolean,
        clearsLoading: Boolean = true,
    ): BillingSyncOutcome =
        BillingSyncOutcome(
            entitlementSnapshot =
                verificationPolicy.failClosedSnapshot(
                    checkedAtMillis = timeProvider.currentTimeMillis(),
                ),
            status = status,
            hasPendingSubscription = hasPendingSubscription,
            clearsLoading = clearsLoading,
        )

    private fun BillingEntitlementSnapshot.toOutcome(hasPendingSubscription: Boolean): BillingSyncOutcome =
        BillingSyncOutcome(
            entitlementSnapshot = this,
            status = BillingSyncStatus.ENTITLEMENT_RESOLVED,
            hasPendingSubscription = hasPendingSubscription,
        )
}
