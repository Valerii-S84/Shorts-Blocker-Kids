package com.shortsblockerkids.domain.protection

enum class TemporaryAllowDuration(
    val minutes: Int,
) {
    FIVE_MINUTES(5),
    TEN_MINUTES(10),
    FIFTEEN_MINUTES(15),
}
