package com.shortsblockerkids.domain.entitlement

object FreeTestPolicy {
    const val DEFAULT_DURATION_DAYS = 20
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L

    fun expiresAtMillis(
        startedAtMillis: Long,
        durationDays: Int,
    ): Long = startedAtMillis + durationDays.coerceAtLeast(1) * MILLIS_PER_DAY

    fun isActive(
        startedAtMillis: Long?,
        durationDays: Int,
        nowMillis: Long,
    ): Boolean {
        val startedAt = startedAtMillis ?: return false
        val expiresAt = expiresAtMillis(startedAt, durationDays)
        if (expiresAt <= startedAt) {
            return false
        }
        return nowMillis >= startedAt && nowMillis < expiresAt
    }

    fun daysRemaining(
        startedAtMillis: Long?,
        durationDays: Int,
        nowMillis: Long,
    ): Int? {
        val startedAt = startedAtMillis ?: return null
        val expiresAt = expiresAtMillis(startedAt, durationDays)
        if (expiresAt <= startedAt || nowMillis < startedAt || nowMillis >= expiresAt) {
            return 0
        }
        val remainingMillis = expiresAt - nowMillis
        val days = (remainingMillis + MILLIS_PER_DAY - 1L) / MILLIS_PER_DAY
        return days.toInt()
    }
}
