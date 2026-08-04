package com.shortsblockerkids.domain.pin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinRateLimiterTest {
    private val rateLimiter = PinRateLimiter()

    @Test
    fun attemptsOneThroughFourRemainUnlockedWithDescendingRemainingCounts() {
        (1..4).forEach { failedAttempt ->
            val result =
                rateLimiter.recordFailure(
                    previousFailedAttempts = failedAttempt - 1,
                    nowMillis = 1_000L,
                )

            assertEquals(failedAttempt, result.failedAttempts)
            assertNull(result.lockoutUntil)
            assertEquals(5 - failedAttempt, rateLimiter.remainingAttemptsBeforeLockout(failedAttempt))
        }
    }

    @Test
    fun fifthAttemptLocksForThirtySeconds() {
        val result = rateLimiter.recordFailure(previousFailedAttempts = 4, nowMillis = 1_000L)

        assertEquals(5, result.failedAttempts)
        assertEquals(31_000L, result.lockoutUntil)
        assertEquals(0, rateLimiter.remainingAttemptsBeforeLockout(result.failedAttempts))
    }

    @Test
    fun sixthAttemptLocksForSixtySeconds() {
        val result = rateLimiter.recordFailure(previousFailedAttempts = 5, nowMillis = 1_000L)

        assertEquals(6, result.failedAttempts)
        assertEquals(61_000L, result.lockoutUntil)
        assertEquals(0, rateLimiter.remainingAttemptsBeforeLockout(result.failedAttempts))
    }

    @Test
    fun seventhAndLaterAttemptsLockForFiveMinutes() {
        listOf(7, 8, 12).forEach { failedAttempt ->
            val result =
                rateLimiter.recordFailure(
                    previousFailedAttempts = failedAttempt - 1,
                    nowMillis = 1_000L,
                )

            assertEquals(failedAttempt, result.failedAttempts)
            assertEquals(301_000L, result.lockoutUntil)
            assertEquals(0, rateLimiter.remainingAttemptsBeforeLockout(result.failedAttempts))
        }
    }

    @Test
    fun remainingAttemptsNeverBecomeNegative() {
        val expectedCounts = listOf(5, 4, 3, 2, 1, 0, 0, 0, 0)

        expectedCounts.forEachIndexed { failedAttempts, expected ->
            assertEquals(expected, rateLimiter.remainingAttemptsBeforeLockout(failedAttempts))
        }
    }
}
