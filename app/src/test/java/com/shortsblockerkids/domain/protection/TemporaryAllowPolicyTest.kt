package com.shortsblockerkids.domain.protection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryAllowPolicyTest {
    @Test
    fun monotonicDeadlineOverridesWallClockAndExpiresAtItsBoundary() {
        val configuration =
            ProtectionConfiguration(
                temporaryAllowUntilMillis = 500L,
                temporaryAllowElapsedRealtimeDeadlineMillis = 2_000L,
            )

        assertTrue(TemporaryAllowPolicy.isActive(configuration, 10_000L, 1_999L))
        assertFalse(TemporaryAllowPolicy.isActive(configuration, 100L, 2_000L))
        assertFalse(TemporaryAllowPolicy.isActive(configuration, 100L, 2_001L))
    }

    @Test
    fun unavailableElapsedClockDoesNotGrantPersistedMonotonicAllowance() {
        val configuration =
            ProtectionConfiguration(
                temporaryAllowUntilMillis = 10_000L,
                temporaryAllowElapsedRealtimeDeadlineMillis = 2_000L,
            )

        assertFalse(TemporaryAllowPolicy.isActive(configuration, 1_000L, null))
    }

    @Test
    fun configurationWithoutMonotonicDeadlineUsesWallClockBoundary() {
        val configuration = ProtectionConfiguration(temporaryAllowUntilMillis = 2_000L)

        assertTrue(TemporaryAllowPolicy.isActive(configuration, 1_999L, 50_000L))
        assertFalse(TemporaryAllowPolicy.isActive(configuration, 2_000L, 1L))
    }

    @Test
    fun activeUntilKeepsOnlyFutureTemporaryAllowExpiry() {
        assertEquals(1_001L, TemporaryAllowPolicy.activeUntil(1_001L, nowMillis = 1_000L))
        assertEquals(null, TemporaryAllowPolicy.activeUntil(1_000L, nowMillis = 1_000L))
        assertEquals(null, TemporaryAllowPolicy.activeUntil(999L, nowMillis = 1_000L))
        assertEquals(null, TemporaryAllowPolicy.activeUntil(null, nowMillis = 1_000L))
    }

    @Test
    fun activeAndExpiredStatesTreatNullAndBoundaryAsClosed() {
        assertTrue(TemporaryAllowPolicy.isActive(1_001L, nowMillis = 1_000L))
        assertFalse(TemporaryAllowPolicy.isActive(1_000L, nowMillis = 1_000L))
        assertFalse(TemporaryAllowPolicy.isActive(null, nowMillis = 1_000L))

        assertFalse(TemporaryAllowPolicy.hasExpired(null, nowMillis = 1_000L))
        assertFalse(TemporaryAllowPolicy.hasExpired(1_001L, nowMillis = 1_000L))
        assertTrue(TemporaryAllowPolicy.hasExpired(1_000L, nowMillis = 1_000L))
        assertTrue(TemporaryAllowPolicy.hasExpired(999L, nowMillis = 1_000L))
    }
}
