package com.shortsblockerkids.domain.protection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionActivationPolicyTest {
    @Test
    fun disabledAccessibilityServiceReturnsFalse() {
        assertFalse(
            ProtectionActivationPolicy.shouldStartFreeTest(
                isAccessibilityServiceEnabled = false,
                isProtectionEnabled = true,
                isAccessibilityDisclosureAccepted = true,
                isPinConfigured = true,
                isFreeTestAlreadyStarted = false,
            ),
        )
    }

    @Test
    fun disabledProtectionReturnsFalse() {
        assertFalse(
            ProtectionActivationPolicy.shouldStartFreeTest(
                isAccessibilityServiceEnabled = true,
                isProtectionEnabled = false,
                isAccessibilityDisclosureAccepted = true,
                isPinConfigured = true,
                isFreeTestAlreadyStarted = false,
            ),
        )
    }

    @Test
    fun missingAccessibilityDisclosureReturnsFalse() {
        assertFalse(
            ProtectionActivationPolicy.shouldStartFreeTest(
                isAccessibilityServiceEnabled = true,
                isProtectionEnabled = true,
                isAccessibilityDisclosureAccepted = false,
                isPinConfigured = true,
                isFreeTestAlreadyStarted = false,
            ),
        )
    }

    @Test
    fun missingPinReturnsFalse() {
        assertFalse(
            ProtectionActivationPolicy.shouldStartFreeTest(
                isAccessibilityServiceEnabled = true,
                isProtectionEnabled = true,
                isAccessibilityDisclosureAccepted = true,
                isPinConfigured = false,
                isFreeTestAlreadyStarted = false,
            ),
        )
    }

    @Test
    fun alreadyStartedFreeTestReturnsFalse() {
        assertFalse(
            ProtectionActivationPolicy.shouldStartFreeTest(
                isAccessibilityServiceEnabled = true,
                isProtectionEnabled = true,
                isAccessibilityDisclosureAccepted = true,
                isPinConfigured = true,
                isFreeTestAlreadyStarted = true,
            ),
        )
    }

    @Test
    fun allPrerequisitesWithFreeTestNotStartedReturnsTrue() {
        assertTrue(
            ProtectionActivationPolicy.shouldStartFreeTest(
                isAccessibilityServiceEnabled = true,
                isProtectionEnabled = true,
                isAccessibilityDisclosureAccepted = true,
                isPinConfigured = true,
                isFreeTestAlreadyStarted = false,
            ),
        )
    }
}
