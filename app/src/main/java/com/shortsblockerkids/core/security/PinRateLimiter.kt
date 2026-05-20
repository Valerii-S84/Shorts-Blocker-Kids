package com.shortsblockerkids.core.security

class PinRateLimiter {
    fun recordFailure(
        previousFailedAttempts: Int,
        nowMillis: Long,
    ): PinRateLimitResult {
        val failedAttempts = previousFailedAttempts + 1
        val lockoutMillis =
            when {
                failedAttempts == 5 -> 30_000L
                failedAttempts == 6 -> 60_000L
                failedAttempts >= 7 -> 5 * 60_000L
                else -> null
            }

        return PinRateLimitResult(
            failedAttempts = failedAttempts,
            lockoutUntil = lockoutMillis?.let { nowMillis + it },
        )
    }

    fun remainingAttemptsBeforeLockout(failedAttempts: Int): Int = (FIRST_LOCKOUT_ATTEMPT - failedAttempts).coerceAtLeast(0)

    companion object {
        private const val FIRST_LOCKOUT_ATTEMPT = 5
    }
}

data class PinRateLimitResult(
    val failedAttempts: Int,
    val lockoutUntil: Long?,
)
