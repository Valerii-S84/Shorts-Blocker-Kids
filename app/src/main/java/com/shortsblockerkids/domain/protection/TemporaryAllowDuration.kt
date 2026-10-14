package com.shortsblockerkids.domain.protection

enum class TemporaryAllowDuration(
    val minutes: Int,
) {
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    SIXTY_MINUTES(60),
}
