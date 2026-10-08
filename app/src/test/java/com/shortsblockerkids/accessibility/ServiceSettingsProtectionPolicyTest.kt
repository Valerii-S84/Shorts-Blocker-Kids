package com.shortsblockerkids.accessibility

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.domain.detection.SupportedPlatform
import com.shortsblockerkids.domain.entitlement.EntitlementSnapshot
import com.shortsblockerkids.domain.protection.ProtectionConfiguration
import com.shortsblockerkids.platform.accessibility.resolveServiceProtectionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceSettingsProtectionPolicyTest {
    @Test
    fun unavailableStartupProtectsKnownPackagesWithoutInventingSettingsSnapshot() {
        val state = resolveServiceProtectionState(null, 1_000L, 1_000L)
        assertTrue(state.isProtectionActive)
        assertTrue(SupportedPlatform.PROTECTED_PLATFORMS.all { state.isPlatformEnabled(it.id) })
        assertFalse(state.isPlatformEnabled("unrelated.app"))
    }

    @Test
    fun realDisabledSnapshotReplacesConservativeStartupPolicy() {
        val snapshot =
            AppSettingsSnapshot(
                protectionConfiguration =
                    ProtectionConfiguration(
                        isAccessibilityDisclosureAccepted = true,
                        isPinConfigured = true,
                    ),
                entitlement = EntitlementSnapshot(freeTestStartedAtMillis = 500L),
            )
        assertTrue(resolveServiceProtectionState(snapshot, 1_000L, 1_000L).isProtectionActive)
        val disabled = snapshot.copy(protectionConfiguration = snapshot.protectionConfiguration.copy(isEnabled = false))
        assertFalse(resolveServiceProtectionState(disabled, 1_000L, 1_000L).isProtectionActive)
    }
}
