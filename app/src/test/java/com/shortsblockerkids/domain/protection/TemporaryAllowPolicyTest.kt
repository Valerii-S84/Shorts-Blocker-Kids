package com.shortsblockerkids.domain.protection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryAllowPolicyTest {
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
