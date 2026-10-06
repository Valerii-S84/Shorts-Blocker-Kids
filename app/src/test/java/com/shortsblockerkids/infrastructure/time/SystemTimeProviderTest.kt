package com.shortsblockerkids.infrastructure.time

import org.junit.Assert.assertTrue
import org.junit.Test

class SystemTimeProviderTest {
    @Test
    fun returnsCurrentWallClockMillis() {
        val provider = SystemTimeProvider()

        val before = System.currentTimeMillis()
        val current = provider.currentTimeMillis()
        val after = System.currentTimeMillis()

        assertTrue(current in before..after)
    }
}
