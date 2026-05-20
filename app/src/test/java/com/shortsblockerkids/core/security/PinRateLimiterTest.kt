package com.shortsblockerkids.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinRateLimiterTest {
    private val rateLimiter = PinRateLimiter()

    @Test
    fun doesNotLockBeforeFifthFailedAttempt() {
        val result = rateLimiter.recordFailure(previousFailedAttempts = 3, nowMillis = 1_000L)

        assertEquals(4, result.failedAttempts)
        assertNull(result.lockoutUntil)
        assertEquals(1, rateLimiter.remainingAttemptsBeforeLockout(result.failedAttempts))
    }

    @Test
    fun locksForThirtySecondsOnFifthFailedAttempt() {
        val result = rateLimiter.recordFailure(previousFailedAttempts = 4, nowMillis = 1_000L)

        assertEquals(5, result.failedAttempts)
        assertEquals(31_000L, result.lockoutUntil)
    }

    @Test
    fun escalatesLockoutAfterMoreFailedAttempts() {
        val sixth = rateLimiter.recordFailure(previousFailedAttempts = 5, nowMillis = 1_000L)
        val seventh = rateLimiter.recordFailure(previousFailedAttempts = 6, nowMillis = 1_000L)

        assertEquals(61_000L, sixth.lockoutUntil)
        assertEquals(301_000L, seventh.lockoutUntil)
    }
}
