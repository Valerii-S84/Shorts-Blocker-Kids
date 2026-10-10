package com.shortsblockerkids.domain.protection

object TemporaryAllowPolicy {
    fun isActive(
        configuration: ProtectionConfiguration,
        nowMillis: Long,
        elapsedRealtimeMillis: Long?,
    ): Boolean =
        configuration.temporaryAllowElapsedRealtimeDeadlineMillis?.let { deadline ->
            elapsedRealtimeMillis != null && deadline > elapsedRealtimeMillis
        } ?: isActive(configuration.temporaryAllowUntilMillis, nowMillis)

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
