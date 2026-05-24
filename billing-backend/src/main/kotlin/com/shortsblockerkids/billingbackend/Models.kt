package com.shortsblockerkids.billingbackend

enum class SubscriptionEntitlementState {
    UNKNOWN,
    ACTIVE,
    CANCELED_ACTIVE,
    PENDING,
    IN_GRACE,
    ON_HOLD,
    EXPIRED,
    REVOKED,
}

fun SubscriptionEntitlementState.allowsProtection(): Boolean =
    this == SubscriptionEntitlementState.ACTIVE ||
        this == SubscriptionEntitlementState.CANCELED_ACTIVE ||
        this == SubscriptionEntitlementState.IN_GRACE

data class VerificationRequest(
    val installId: String,
    val packageName: String,
    val productId: String,
    val purchaseToken: String,
    val appVersion: String?,
)

data class VerifiedSubscription(
    val productId: String,
    val purchaseToken: String,
    val state: SubscriptionEntitlementState,
    val activeUntilMillis: Long?,
    val acknowledged: Boolean,
    val verifiedAtMillis: Long,
)

data class EntitlementRecord(
    val installId: String,
    val packageName: String,
    val productId: String,
    val purchaseTokenHash: String,
    val state: SubscriptionEntitlementState,
    val activeUntilMillis: Long?,
    val acknowledged: Boolean,
    val lastVerifiedAtMillis: Long,
    val appVersion: String?,
)
