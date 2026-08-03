package com.shortsblockerkids.domain.entitlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class FreeTestPolicyTest {
    @RunWith(Parameterized::class)
    class BoundaryMatrix(
        private val boundary: String,
        private val nowMillis: Long,
        private val expectedActive: Boolean,
        private val expectedDaysRemaining: Int,
    ) {
        @Test
        fun isActiveOnlyWithinHalfOpenInterval() {
            assertEquals(
                boundary,
                expectedActive,
                FreeTestPolicy.isActive(
                    startedAtMillis = START,
                    durationDays = DURATION_DAYS,
                    nowMillis = nowMillis,
                ),
            )
        }

        @Test
        fun daysRemainingMatchesHalfOpenInterval() {
            assertEquals(
                boundary,
                expectedDaysRemaining,
                FreeTestPolicy.daysRemaining(
                    startedAtMillis = START,
                    durationDays = DURATION_DAYS,
                    nowMillis = nowMillis,
                ),
            )
        }

        private companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun boundaryMatrix(): List<Array<Any>> =
                listOf(
                    arrayOf("start - 1", START - 1L, false, 0),
                    arrayOf("start", START, true, 1),
                    arrayOf("start + 1", START + 1L, true, 1),
                    arrayOf("expiry - 1", EXPIRY - 1L, true, 1),
                    arrayOf("expiry", EXPIRY, false, 0),
                    arrayOf("expiry + 1", EXPIRY + 1L, false, 0),
                )
        }
    }

    class ContractCases {
        @Test
        fun missingStartIsInactiveAndHasNoRemainingDays() {
            assertFalse(
                FreeTestPolicy.isActive(
                    startedAtMillis = null,
                    durationDays = FreeTestPolicy.DEFAULT_DURATION_DAYS,
                    nowMillis = START,
                ),
            )
            assertEquals(
                null,
                FreeTestPolicy.daysRemaining(
                    startedAtMillis = null,
                    durationDays = FreeTestPolicy.DEFAULT_DURATION_DAYS,
                    nowMillis = START,
                ),
            )
        }

        @Test
        fun nonPositiveDurationStillGetsOneDayMinimum() {
            assertEquals(ONE_DAY, FreeTestPolicy.expiresAtMillis(startedAtMillis = 0L, durationDays = 0))
            assertTrue(FreeTestPolicy.isActive(startedAtMillis = 0L, durationDays = 0, nowMillis = ONE_DAY - 1L))
            assertFalse(FreeTestPolicy.isActive(startedAtMillis = 0L, durationDays = 0, nowMillis = ONE_DAY))
        }

        @Test
        fun invalidTemporalRangeIsFailClosed() {
            assertFalse(
                FreeTestPolicy.isActive(
                    startedAtMillis = Long.MAX_VALUE,
                    durationDays = DURATION_DAYS,
                    nowMillis = Long.MIN_VALUE,
                ),
            )
            assertEquals(
                0,
                FreeTestPolicy.daysRemaining(
                    startedAtMillis = Long.MAX_VALUE,
                    durationDays = DURATION_DAYS,
                    nowMillis = Long.MIN_VALUE,
                ),
            )
        }

        @Test
        fun extremeExpiredTimeHasNoRemainingDays() {
            assertEquals(
                0,
                FreeTestPolicy.daysRemaining(
                    startedAtMillis = Long.MIN_VALUE,
                    durationDays = DURATION_DAYS,
                    nowMillis = Long.MAX_VALUE,
                ),
            )
        }

        @Test
        fun clockRollbackFromExpiryToBeforeStartDoesNotGrantEntitlement() {
            assertFalse(
                FreeTestPolicy.isActive(
                    startedAtMillis = START,
                    durationDays = DURATION_DAYS,
                    nowMillis = EXPIRY,
                ),
            )
            assertFalse(
                FreeTestPolicy.isActive(
                    startedAtMillis = START,
                    durationDays = DURATION_DAYS,
                    nowMillis = START - 1L,
                ),
            )
        }
    }

    private companion object {
        const val ONE_DAY = 24L * 60L * 60L * 1_000L
        const val START = 1_000L
        const val DURATION_DAYS = 1
        const val EXPIRY = START + ONE_DAY
    }
}
