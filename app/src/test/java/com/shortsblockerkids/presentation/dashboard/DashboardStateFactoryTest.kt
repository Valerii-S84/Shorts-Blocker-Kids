package com.shortsblockerkids.presentation.dashboard

import com.shortsblockerkids.R
import com.shortsblockerkids.accessibility.PlatformSupportMatrix
import com.shortsblockerkids.accessibility.PlatformSupportStatus
import com.shortsblockerkids.core.billing.BillingMessageCode
import com.shortsblockerkids.core.billing.BillingUiMessage
import com.shortsblockerkids.core.billing.BillingUiState
import com.shortsblockerkids.domain.entitlement.FreeTestPolicy
import com.shortsblockerkids.domain.protection.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardStateFactoryTest {
    @Test
    fun allProtectionPrerequisitesProduceActiveProtection() {
        val state = createState(eligibleInput())

        assertTrue(state.protection.canProtect)
        assertTrue(state.protection.isActive)
        assertFalse(state.protection.isLocked)
        assertEquals(R.string.status_on, state.protection.switchStatusRes)
        assertEquals(R.string.status_protection_active, state.protection.protectionStatusRes)
        assertTrue(state.warnings.isEmpty())
    }

    @Test
    fun disabledProtectionCannotProtect() {
        val input = eligibleInput()
        val state =
            createState(
                input.copy(protection = input.protection.copy(isEnabled = false)),
            )

        assertFalse(state.protection.canProtect)
        assertEquals(R.string.status_off, state.protection.switchStatusRes)
        assertEquals(listOf(DashboardWarningUiModel.PROTECTION_DISABLED), state.warnings)
    }

    @Test
    fun missingDisclosureCannotProtect() {
        val input = eligibleInput()
        val state =
            createState(
                input.copy(
                    protection =
                        input.protection.copy(isAccessibilityDisclosureAccepted = false),
                ),
            )

        assertFalse(state.protection.canProtect)
        assertFalse(state.setup.isAccessibilityDisclosureAccepted)
        assertEquals(R.string.dashboard_review_accessibility_disclosure, state.actions.accessibilitySettingsLabelRes)
        assertEquals(listOf(DashboardWarningUiModel.SETUP_INCOMPLETE), state.warnings)
    }

    @Test
    fun unsupportedProtectionModeCannotProtect() {
        val input = eligibleInput()
        val state =
            createState(
                input.copy(protection = input.protection.copy(modeName = "UNSUPPORTED_MODE")),
            )

        assertFalse(state.protection.canProtect)
        assertFalse(state.protection.isActive)
        assertEquals("UNSUPPORTED_MODE", state.protection.modeName)
        assertEquals(listOf(DashboardWarningUiModel.SETUP_INCOMPLETE), state.warnings)
    }

    @Test
    fun noSelectedPlatformCannotProtect() {
        val input = eligibleInput()
        val state =
            createState(
                input.copy(
                    protection = input.protection.copy(enabledPlatformIds = emptySet()),
                ),
            )

        assertFalse(state.protection.canProtect)
        assertFalse(state.setup.hasProtectedPlatforms)
        assertEquals(
            listOf(
                DashboardWarningUiModel.NO_PROTECTED_APPS_SELECTED,
                DashboardWarningUiModel.NO_PROTECTED_APPS,
            ),
            state.warnings,
        )
    }

    @Test
    fun missingEntitlementCannotProtect() {
        val input = eligibleInput()
        val state =
            createState(
                input.copy(
                    entitlement =
                        input.entitlement.copy(freeTestStartedAtMillis = null),
                ),
            )

        assertFalse(state.protection.canProtect)
        assertFalse(state.entitlement.isFreeTestStarted)
        assertNull(state.entitlement.freeTestDaysRemaining)
        assertEquals(R.string.status_not_started, state.entitlement.freeTestStatusRes)
        assertEquals(listOf(DashboardWarningUiModel.FREE_TEST_NOT_STARTED), state.warnings)
    }

    @Test
    fun missingPinCannotProtect() {
        val input = eligibleInput()
        val state =
            createState(
                input.copy(protection = input.protection.copy(isPinConfigured = false)),
            )

        assertFalse(state.protection.canProtect)
        assertFalse(state.setup.isPinConfigured)
        assertEquals(listOf(DashboardWarningUiModel.SETUP_INCOMPLETE), state.warnings)
    }

    @Test
    fun activeTemporaryAllowCannotProtect() {
        val input = eligibleInput()
        val state =
            createState(
                input.copy(
                    protection =
                        input.protection.copy(temporaryAllowUntilMillis = NOW_MILLIS + 1L),
                ),
            )

        assertFalse(state.protection.canProtect)
        assertEquals(listOf(DashboardWarningUiModel.TEMPORARY_ALLOW_ACTIVE), state.warnings)
    }

    @Test
    fun temporaryAllowExactExpiryRestoresProtection() {
        val input = eligibleInput()
        val state =
            createState(
                input.copy(
                    protection =
                        input.protection.copy(temporaryAllowUntilMillis = NOW_MILLIS),
                ),
            )

        assertTrue(state.protection.canProtect)
        assertTrue(state.protection.isActive)
    }

    @Test
    fun freeEntitlementExactExpiryLocksProtection() {
        val expiryMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY_MILLIS
        val beforeExpiry =
            createState(
                eligibleInput().copy(
                    runtime = eligibleInput().runtime.copy(nowMillis = expiryMillis - 1L),
                ),
            )
        val atExpiry =
            createState(
                eligibleInput().copy(
                    runtime = eligibleInput().runtime.copy(nowMillis = expiryMillis),
                ),
            )

        assertTrue(beforeExpiry.protection.canProtect)
        assertEquals(1, beforeExpiry.entitlement.freeTestDaysRemaining)
        assertFalse(atExpiry.protection.canProtect)
        assertTrue(atExpiry.protection.isLocked)
        assertEquals(0, atExpiry.entitlement.freeTestDaysRemaining)
        assertEquals(R.string.status_locked, atExpiry.protection.switchStatusRes)
        assertEquals(
            listOf(
                DashboardWarningUiModel.FREE_TEST_ENDED,
                DashboardWarningUiModel.FREE_TEST_EXPIRED,
            ),
            atExpiry.warnings,
        )
    }

    @Test
    fun paidEntitlementCanProtectAfterFreeTestExpiry() {
        val expiryMillis = FreeTestPolicy.DEFAULT_DURATION_DAYS * ONE_DAY_MILLIS
        val input = eligibleInput()
        val state =
            createState(
                input.copy(
                    entitlement =
                        input.entitlement.copy(
                            isPaidProtectionAllowed = true,
                            paidLastVerifiedAtMillis = expiryMillis,
                            paidActiveUntilMillis = expiryMillis + ONE_DAY_MILLIS,
                        ),
                    runtime = input.runtime.copy(nowMillis = expiryMillis),
                ),
            )

        assertTrue(state.protection.canProtect)
        assertTrue(state.protection.isActive)
        assertTrue(state.billing.hasEntitlement)
        assertEquals(R.string.status_free_test_expired, state.entitlement.freeTestStatusRes)
    }

    @Test
    fun accessibilityPermissionRemainsAnOuterPrerequisite() {
        val input = eligibleInput()
        val state =
            createState(
                input.copy(
                    runtime = input.runtime.copy(isAccessibilityServiceEnabled = false),
                ),
            )

        assertTrue(state.protection.canProtect)
        assertFalse(state.protection.isActive)
        assertEquals("PROTECTION_PERMISSION_MISSING", state.entitlement.resolvedStateName)
        assertEquals(
            listOf(
                DashboardWarningUiModel.PROTECTION_PERMISSION_MISSING,
                DashboardWarningUiModel.ACCESSIBILITY_DISABLED,
            ),
            state.warnings,
        )
    }

    @Test
    fun unsupportedPlatformDoesNotMakeProtectionAvailable() {
        val input = eligibleInput()
        val unsupportedId = PlatformSupportMatrix.unsupportedEntries.first().platformId
        val state =
            createState(
                input.copy(
                    protection = input.protection.copy(enabledPlatformIds = setOf(unsupportedId)),
                ),
            )

        assertFalse(state.protection.canProtect)
        assertFalse(state.setup.hasProtectedPlatforms)
        assertTrue(state.platforms.unsupported.none(ProtectedPlatformItemUiModel::isSelected))
    }

    @Test
    fun platformMappingPreservesMatrixOrderIdentityResourcesAndAvailability() {
        val state = createState(eligibleInput())

        assertEquals(
            PlatformSupportMatrix.protectedEntries.map { entry -> entry.platformId },
            state.platforms.protected.map(ProtectedPlatformItemUiModel::platformId),
        )
        assertEquals(
            PlatformSupportMatrix.unsupportedEntries.map { entry -> entry.platformId },
            state.platforms.unsupported.map(ProtectedPlatformItemUiModel::platformId),
        )
        assertPlatformMapping(
            entries = PlatformSupportMatrix.protectedEntries,
            items = state.platforms.protected,
        )
        assertPlatformMapping(
            entries = PlatformSupportMatrix.unsupportedEntries,
            items = state.platforms.unsupported,
        )
    }

    private fun assertPlatformMapping(
        entries: List<com.shortsblockerkids.accessibility.PlatformSupportEntry>,
        items: List<ProtectedPlatformItemUiModel>,
    ) {
        entries.zip(items).forEach { (entry, item) ->
            assertEquals(entry.platformNameRes, item.nameRes)
            assertEquals(entry.packageName, item.packageName)
            assertEquals(entry.status != PlatformSupportStatus.NOT_SUPPORTED, item.isSupported)
            assertEquals(item.isSupported, item.isAvailable)
            assertEquals(item.isEnabled, item.isClickable)
            assertEquals(expectedStatusRes(entry.status), item.statusRes)
        }
    }

    @Test
    fun factoryPreservesBillingAndTamperPresentationInputs() {
        val input = eligibleInput()
        val billingUiState =
            BillingUiState(
                isReady = true,
                productPrice = "€1.99",
                message = BillingUiMessage(BillingMessageCode.PRODUCT_LOADED),
                canStartPurchase = true,
            )
        val state =
            createState(
                input.copy(
                    billing =
                        DashboardBillingInput(
                            uiState = billingUiState,
                            entitlementStateName = "PRODUCT_LOADED",
                        ),
                    runtime = input.runtime.copy(isTamperProtectionEnabled = true),
                ),
            )

        assertEquals(billingUiState, state.billing.uiState)
        assertEquals("PRODUCT_LOADED", state.billing.entitlementStateName)
        assertTrue(state.setup.isTamperProtectionEnabled)
        assertEquals(R.string.dashboard_review_tamper_protection, state.actions.tamperProtectionLabelRes)
        assertEquals(R.string.dashboard_open_accessibility_settings, state.actions.accessibilitySettingsLabelRes)
    }

    private fun createState(input: DashboardStateInput): DashboardUiState = DashboardStateFactory.create(input)

    private fun eligibleInput(): DashboardStateInput =
        DashboardStateInput(
            protection =
                DashboardProtectionInput(
                    isEnabled = true,
                    isAccessibilityDisclosureAccepted = true,
                    modeName = ProtectionMode.BLOCK_SHORTS.name,
                    enabledPlatformIds =
                        PlatformSupportMatrix.protectedEntries
                            .mapTo(linkedSetOf()) { entry -> entry.platformId },
                    temporaryAllowUntilMillis = null,
                    isPinConfigured = true,
                ),
            entitlement =
                DashboardEntitlementInput(
                    freeTestStartedAtMillis = 0L,
                    freeTestDurationDays = FreeTestPolicy.DEFAULT_DURATION_DAYS,
                    isPaidProtectionAllowed = false,
                    paidLastVerifiedAtMillis = null,
                    paidActiveUntilMillis = null,
                ),
            billing =
                DashboardBillingInput(
                    uiState = BillingUiState(),
                    entitlementStateName = "UNKNOWN",
                ),
            platforms =
                PlatformSupportMatrix.entries.map { entry ->
                    DashboardPlatformInput(
                        platformId = entry.platformId,
                        nameRes = entry.platformNameRes,
                        packageName = entry.packageName,
                        supportStatusName = entry.status.name,
                    )
                },
            runtime =
                DashboardRuntimeInput(
                    isAccessibilityServiceEnabled = true,
                    isTamperProtectionEnabled = false,
                    nowMillis = NOW_MILLIS,
                ),
        )

    private fun expectedStatusRes(status: PlatformSupportStatus): Int =
        when (status) {
            PlatformSupportStatus.SUPPORTED -> R.string.platform_status_supported
            PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA ->
                R.string.platform_status_supported_needs_qa
            PlatformSupportStatus.NOT_SUPPORTED -> R.string.platform_status_not_supported
        }

    private companion object {
        const val NOW_MILLIS = 1_000L
        const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
