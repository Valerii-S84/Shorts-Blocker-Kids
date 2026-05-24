package com.shortsblockerkids.core.storage

import com.shortsblockerkids.core.entitlement.FreeTestPolicy
import com.shortsblockerkids.core.model.EntitlementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun freeTestDoesNotStartOnFirstAppOpen() {
        val settings = AppSettings()

        assertEquals(
            EntitlementState.FREE_TEST_NOT_STARTED,
            settings.freeTestState(nowMillis = 1_000L),
        )
        assertEquals(null, settings.freeTestDaysRemaining(nowMillis = 1_000L))
        assertFalse(settings.canProtect(nowMillis = 1_000L))
    }

    @Test
    fun canProtectOnlyWhenLocalSetupAndFreeTestAreActive() {
        val settings =
            AppSettings(
                protectionEnabled = true,
                accessibilityDisclosureAccepted = true,
                freeTestStartedAt = TEST_STARTED_AT,
                pinHash = "hash",
                pinSalt = "salt",
            )

        assertTrue(settings.canProtect(nowMillis = 1_000L))
        assertFalse(settings.copy(protectionEnabled = false).canProtect(nowMillis = 1_000L))
        assertFalse(
            settings.copy(accessibilityDisclosureAccepted = false).canProtect(nowMillis = 1_000L),
        )
        assertFalse(settings.copy(pinHash = null).canProtect(nowMillis = 1_000L))
    }

    @Test
    fun pinIsCreatedOnlyWhenHashAndSaltAreBothPresent() {
        assertTrue(AppSettings(pinHash = "hash", pinSalt = "salt").isPinCreated)
        assertFalse(AppSettings(pinHash = "", pinSalt = "salt").isPinCreated)
        assertFalse(AppSettings(pinHash = "hash", pinSalt = " ").isPinCreated)
        assertFalse(AppSettings(pinHash = null, pinSalt = "salt").isPinCreated)
        assertFalse(AppSettings(pinHash = "hash", pinSalt = null).isPinCreated)
    }

    @Test
    fun freeTestIsActiveOnDayOne() {
        val settings =
            activeSettings(
                freeTestStartedAt = TEST_STARTED_AT,
            )

        assertEquals(
            EntitlementState.FREE_TEST_ACTIVE,
            settings.freeTestState(nowMillis = ONE_DAY),
        )
        assertTrue(settings.canProtect(nowMillis = ONE_DAY))
    }

    @Test
    fun freeTestIsActiveOnDayTwentyBeforeExpiryTime() {
        val settings = activeSettings(freeTestStartedAt = TEST_STARTED_AT)
        val beforeExpiry = TWENTY_DAYS - 1L

        assertEquals(EntitlementState.FREE_TEST_ACTIVE, settings.freeTestState(beforeExpiry))
        assertEquals(1, settings.freeTestDaysRemaining(beforeExpiry))
        assertTrue(settings.canProtect(nowMillis = beforeExpiry))
    }

    @Test
    fun freeTestExpiresAfterTwentyDays() {
        val settings = activeSettings(freeTestStartedAt = TEST_STARTED_AT)

        assertEquals(EntitlementState.FREE_TEST_EXPIRED, settings.freeTestState(TWENTY_DAYS))
        assertEquals(0, settings.freeTestDaysRemaining(TWENTY_DAYS))
        assertFalse(settings.canProtect(nowMillis = TWENTY_DAYS))
    }

    @Test
    fun activeBillingEntitlementCanProtectAfterFreeTestExpires() {
        val settings =
            activeSettings(freeTestStartedAt = TEST_STARTED_AT)
                .copy(
                    billingSubscriptionActive = true,
                    billingLastVerifiedAt = TWENTY_DAYS,
                )

        assertEquals(EntitlementState.FREE_TEST_EXPIRED, settings.freeTestState(TWENTY_DAYS))
        assertTrue(settings.hasBillingEntitlement(nowMillis = TWENTY_DAYS))
        assertTrue(settings.canProtect(nowMillis = TWENTY_DAYS))
    }

    @Test
    fun staleBillingEntitlementDoesNotProtectAfterGrace() {
        val settings =
            activeSettings(freeTestStartedAt = TEST_STARTED_AT)
                .copy(
                    billingSubscriptionActive = true,
                    billingLastVerifiedAt = TWENTY_DAYS,
                )
        val afterGrace = TWENTY_DAYS + 72L * 60L * 60L * 1_000L + 1L

        assertFalse(settings.hasBillingEntitlement(nowMillis = afterGrace))
        assertFalse(settings.canProtect(nowMillis = afterGrace))
    }

    @Test
    fun futureBillingVerificationTimestampDoesNotProtect() {
        val settings =
            activeSettings(freeTestStartedAt = TEST_STARTED_AT)
                .copy(
                    billingSubscriptionActive = true,
                    billingLastVerifiedAt = TWENTY_DAYS + 1L,
                )

        assertFalse(settings.hasBillingEntitlement(nowMillis = TWENTY_DAYS))
        assertFalse(settings.canProtect(nowMillis = TWENTY_DAYS))
    }

    @Test
    fun temporaryAllowDisablesProtectionUntilItExpires() {
        val settings =
            AppSettings(
                protectionEnabled = true,
                accessibilityDisclosureAccepted = true,
                freeTestStartedAt = TEST_STARTED_AT,
                temporaryAllowUntil = 2_000L,
                pinHash = "hash",
                pinSalt = "salt",
            )

        assertFalse(settings.canProtect(nowMillis = 1_500L))
        assertTrue(settings.canProtect(nowMillis = 2_500L))
    }

    @Test
    fun temporaryAllowReportsActiveExpiredAndMissingStates() {
        val noAllow = AppSettings(temporaryAllowUntil = null)
        val activeAllow = AppSettings(temporaryAllowUntil = 2_000L)
        val expiredAllow = AppSettings(temporaryAllowUntil = 1_000L)

        assertEquals(null, noAllow.activeTemporaryAllowUntil(nowMillis = 1_500L))
        assertFalse(noAllow.isTemporarilyAllowed(nowMillis = 1_500L))
        assertFalse(noAllow.hasExpiredTemporaryAllow(nowMillis = 1_500L))

        assertEquals(2_000L, activeAllow.activeTemporaryAllowUntil(nowMillis = 1_500L))
        assertTrue(activeAllow.isTemporarilyAllowed(nowMillis = 1_500L))
        assertFalse(activeAllow.hasExpiredTemporaryAllow(nowMillis = 1_500L))

        assertEquals(null, expiredAllow.activeTemporaryAllowUntil(nowMillis = 1_500L))
        assertFalse(expiredAllow.isTemporarilyAllowed(nowMillis = 1_500L))
        assertTrue(expiredAllow.hasExpiredTemporaryAllow(nowMillis = 1_500L))
    }

    private fun activeSettings(freeTestStartedAt: Long): AppSettings =
        AppSettings(
            protectionEnabled = true,
            accessibilityDisclosureAccepted = true,
            freeTestStartedAt = freeTestStartedAt,
            freeTestDurationDays = FreeTestPolicy.DEFAULT_DURATION_DAYS,
            pinHash = "hash",
            pinSalt = "salt",
        )

    private companion object {
        const val TEST_STARTED_AT = 0L
        const val ONE_DAY = 24L * 60L * 60L * 1_000L
        const val TWENTY_DAYS = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY
    }
}
