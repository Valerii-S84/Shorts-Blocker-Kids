package com.shortsblockerkids.composition.dashboard

import com.shortsblockerkids.accessibility.PlatformSupportMatrix
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.entitlement.EntitlementPolicy
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.presentation.billing.BillingUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUiStateAssemblerTest {
    @Test
    fun repeatedAssemblyReevaluatesFreeTestExpiry() {
        val expiryMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY_MILLIS
        val timeProvider = MutableTimeProvider(expiryMillis - 1L)
        val assembler = DashboardUiStateAssembler(timeProvider)
        val settings = eligibleSettings()

        assertTrue(assembler.createState(settings).protection.canProtect)

        timeProvider.nowMillis = expiryMillis

        val expiredState = assembler.createState(settings)
        assertFalse(expiredState.protection.canProtect)
        assertTrue(expiredState.protection.isLocked)
    }

    @Test
    fun repeatedAssemblyReevaluatesTemporaryAllowExpiry() {
        val timeProvider = MutableTimeProvider(NOW_MILLIS)
        val assembler = DashboardUiStateAssembler(timeProvider)
        val settings =
            eligibleSettings(
                protection =
                    eligibleProtection().copy(
                        temporaryAllowUntilMillis = NOW_MILLIS + 1L,
                    ),
            )

        assertFalse(assembler.createState(settings).protection.canProtect)

        timeProvider.nowMillis = NOW_MILLIS + 1L

        assertTrue(assembler.createState(settings).protection.canProtect)
    }

    @Test
    fun repeatedAssemblyReevaluatesPaidOfflineGraceExpiry() {
        val graceExpiry = NOW_MILLIS + EntitlementPolicy.PAID_OFFLINE_GRACE_MILLIS
        val timeProvider = MutableTimeProvider(graceExpiry)
        val assembler = DashboardUiStateAssembler(timeProvider)
        val settings =
            eligibleSettings(
                entitlement =
                    EntitlementSnapshot(
                        freeTestStartedAtMillis = null,
                        isPaidProtectionAllowed = true,
                        paidLastVerifiedAtMillis = NOW_MILLIS,
                    ),
            )

        val activeState = assembler.createState(settings)
        assertTrue(activeState.protection.canProtect)
        assertTrue(activeState.billing.hasEntitlement)

        timeProvider.nowMillis = graceExpiry + 1L

        val expiredState = assembler.createState(settings)
        assertFalse(expiredState.protection.canProtect)
        assertFalse(expiredState.billing.hasEntitlement)
    }

    private fun DashboardUiStateAssembler.createState(settings: AppSettingsSnapshot) =
        create(
            settings = settings,
            billingUiState = BillingUiState(),
            isAccessibilityServiceEnabled = true,
            isTamperProtectionEnabled = false,
        )

    private fun eligibleSettings(
        protection: ProtectionConfiguration = eligibleProtection(),
        entitlement: EntitlementSnapshot =
            EntitlementSnapshot(
                freeTestStartedAtMillis = 0L,
                freeTestDurationDays = FreeTestPolicy.DEFAULT_DURATION_DAYS,
            ),
    ): AppSettingsSnapshot =
        AppSettingsSnapshot(
            protectionConfiguration = protection,
            entitlement = entitlement,
        )

    private fun eligibleProtection(): ProtectionConfiguration =
        ProtectionConfiguration(
            isEnabled = true,
            isAccessibilityDisclosureAccepted = true,
            enabledPlatformIds =
                PlatformSupportMatrix.protectedEntries
                    .mapTo(linkedSetOf()) { entry -> entry.platformId },
            isPinConfigured = true,
        )

    private class MutableTimeProvider(
        var nowMillis: Long,
    ) : TimeProvider {
        override fun currentTimeMillis(): Long = nowMillis
    }

    private companion object {
        const val NOW_MILLIS = 1_000L
        const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
