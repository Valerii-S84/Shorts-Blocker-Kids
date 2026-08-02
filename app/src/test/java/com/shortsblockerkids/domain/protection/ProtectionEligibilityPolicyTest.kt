package com.shortsblockerkids.domain.protection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionEligibilityPolicyTest {
    @Test
    fun protectionRequiresEveryConfigurationGateAndEntitlement() {
        val configuration = activeConfiguration()

        assertTrue(ProtectionEligibilityPolicy.canProtect(configuration, true, 1_000L))
        assertFalse(
            ProtectionEligibilityPolicy.canProtect(
                configuration.copy(isEnabled = false),
                true,
                1_000L,
            ),
        )
        assertFalse(
            ProtectionEligibilityPolicy.canProtect(
                configuration.copy(isAccessibilityDisclosureAccepted = false),
                true,
                1_000L,
            ),
        )
        assertFalse(
            ProtectionEligibilityPolicy.canProtect(
                configuration.copy(enabledPlatformIds = emptySet()),
                true,
                1_000L,
            ),
        )
        assertFalse(
            ProtectionEligibilityPolicy.canProtect(
                configuration.copy(isPinConfigured = false),
                true,
                1_000L,
            ),
        )
        assertFalse(ProtectionEligibilityPolicy.canProtect(configuration, false, 1_000L))
    }

    @Test
    fun temporaryAllowUsesStrictActiveAndExpiryBoundaries() {
        val configuration = activeConfiguration().copy(temporaryAllowUntilMillis = 2_000L)

        assertFalse(ProtectionEligibilityPolicy.canProtect(configuration, true, 1_999L))
        assertTrue(ProtectionEligibilityPolicy.canProtect(configuration, true, 2_000L))
        assertEquals(2_000L, TemporaryAllowPolicy.activeUntil(2_000L, 1_999L))
        assertTrue(TemporaryAllowPolicy.hasExpired(2_000L, 2_000L))
    }

    private fun activeConfiguration(): ProtectionConfiguration =
        ProtectionConfiguration(
            isEnabled = true,
            isAccessibilityDisclosureAccepted = true,
            isPinConfigured = true,
        )
}
