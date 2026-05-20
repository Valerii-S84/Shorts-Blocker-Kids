package com.shortsblockerkids.core.model

enum class SubscriptionState {
    UNKNOWN,
    TRIAL,
    ACTIVE,
    GRACE_PERIOD,
    ON_HOLD,
    CANCELED_BUT_ACTIVE_UNTIL_END,
    EXPIRED,
}
