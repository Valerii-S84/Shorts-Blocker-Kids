package com.shortsblockerkids.domain.protection

object TemporaryAllowPolicy {
    fun activeUntil(
        temporaryAllowUntilMillis: Long?,
        nowMillis: Long,
    ): Long? = temporaryAllowUntilMillis?.takeIf { it > nowMillis }

    fun isActive(
        temporaryAllowUntilMillis: Long?,
        nowMillis: Long,
    ): Boolean = activeUntil(temporaryAllowUntilMillis, nowMillis) != null

    fun hasExpired(
        temporaryAllowUntilMillis: Long?,
        nowMillis: Long,
    ): Boolean = temporaryAllowUntilMillis != null && temporaryAllowUntilMillis <= nowMillis
}
