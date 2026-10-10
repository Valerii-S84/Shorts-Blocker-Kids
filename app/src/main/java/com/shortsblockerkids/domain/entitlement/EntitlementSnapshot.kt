package com.shortsblockerkids.domain.entitlement

data class EntitlementSnapshot(
    val freeTestStartedAtMillis: Long? = null,
    val freeTestDurationDays: Int = FreeTestPolicy.DEFAULT_DURATION_DAYS,
    val isPaidProtectionAllowed: Boolean = false,
)
