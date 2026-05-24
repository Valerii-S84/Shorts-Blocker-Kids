package com.shortsblockerkids.core.entitlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeTestPolicyTest {
    @Test
    fun missingStartIsInactiveAndHasNoRemainingDays() {
        assertFalse(
            FreeTestPolicy.isActive(
                startedAtMillis = null,
                durationDays = FreeTestPolicy.DEFAULT_DURATION_DAYS,
                nowMillis = 1_000L,
            ),
        )
        assertEquals(
            null,
            FreeTestPolicy.daysRemaining(
                startedAtMillis = null,
                durationDays = FreeTestPolicy.DEFAULT_DURATION_DAYS,
                nowMillis = 1_000L,
            ),
        )
    }

    @Test
    fun nonPositiveDurationStillGetsOneDayMinimum() {
        assertEquals(ONE_DAY, FreeTestPolicy.expiresAtMillis(startedAtMillis = 0L, durationDays = 0))
        assertTrue(FreeTestPolicy.isActive(startedAtMillis = 0L, durationDays = 0, nowMillis = ONE_DAY - 1L))
        assertFalse(FreeTestPolicy.isActive(startedAtMillis = 0L, durationDays = 0, nowMillis = ONE_DAY))
    }

    private companion object {
        const val ONE_DAY = 24L * 60L * 60L * 1_000L
    }
}
