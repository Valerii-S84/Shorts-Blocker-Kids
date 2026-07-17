package com.shortsblockerkids.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.accessibility.PlatformSupportMatrix
import com.shortsblockerkids.core.billing.BillingAvailability
import com.shortsblockerkids.core.billing.BillingCopy
import com.shortsblockerkids.core.billing.BillingUiState
import com.shortsblockerkids.core.entitlement.LocalEntitlementResolver
import com.shortsblockerkids.core.model.EntitlementState
import com.shortsblockerkids.core.storage.AppSettings

@Composable
fun DashboardScreen(
    settings: AppSettings,
    isAccessibilityServiceEnabled: Boolean,
    isTamperProtectionEnabled: Boolean,
    billingUiState: BillingUiState,
    onProtectionChanged: (Boolean) -> Unit,
    onPlatformEnabledChanged: (String, Boolean) -> Unit,
    onSubscribe: () -> Unit,
    onRestorePurchases: () -> Unit,
    onManageSubscription: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTamperProtection: () -> Unit,
    onOpenDebugQa: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val nowMillis = System.currentTimeMillis()
    val freeTestState = settings.freeTestState(nowMillis)
    val entitlementState =
        LocalEntitlementResolver.resolve(
            settings = settings,
            isProtectionPermissionGranted = isAccessibilityServiceEnabled,
            nowMillis = nowMillis,
        )
    val isProtectionActive =
        entitlementState == EntitlementState.PROTECTION_ACTIVE
    val isProtectionLocked = entitlementState == EntitlementState.PROTECTION_LOCKED
    val hasBillingEntitlement = settings.hasBillingEntitlement(nowMillis)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Short Video Protection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProtectionRow(
                    label = "Short Video Protection",
                    value = protectionSwitchLabel(settings, isProtectionLocked),
                    control = {
                        Switch(
                            checked = settings.protectionEnabled && !isProtectionLocked,
                            onCheckedChange = onProtectionChanged,
                            enabled = !isProtectionLocked,
                        )
                    },
                )
                Text(
                    text = "Setup checklist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ChecklistRow("Parent PIN", settings.isPinCreated)
                ChecklistRow("Protected apps selected", settings.hasEnabledPlatforms)
                ChecklistRow(
                    "Accessibility disclosure accepted",
                    settings.accessibilityDisclosureAccepted,
                )
                ChecklistRow("Accessibility Service active", isAccessibilityServiceEnabled)
                ChecklistRow(
                    label = "Device Admin tamper protection",
                    isComplete = isTamperProtectionEnabled,
                    incompleteLabel = "optional",
                    isOptional = true,
                )
                StatusRow("Platform support", PlatformSupportMatrix.protectedSummary())
                StatusRow("Enabled apps", enabledAppsLabel(settings))
                PlatformSupportMatrix.protectedEntries.forEach { entry ->
                    ProtectionRow(
                        label = entry.platformName,
                        value =
                            if (settings.isPlatformEnabled(entry.platformId)) {
                                "enabled"
                            } else {
                                "disabled"
                            },
                        control = {
                            Switch(
                                checked = settings.isPlatformEnabled(entry.platformId),
                                onCheckedChange = { enabled ->
                                    onPlatformEnabledChanged(entry.platformId, enabled)
                                },
                            )
                        },
                    )
                }
                StatusRow("Not supported", PlatformSupportMatrix.unsupportedSummary())
                StatusRow("PIN", if (settings.isPinCreated) "created" else "not created")
                StatusRow(
                    "Free test",
                    freeTestState.dashboardLabel(),
                )
                settings.freeTestDaysRemaining(nowMillis)?.let { daysRemaining ->
                    StatusRow("Days remaining", "$daysRemaining days remaining")
                }
                StatusRow(
                    "Subscription",
                    BillingCopy.subscriptionStatusLabel(
                        hasBillingEntitlement = settings.hasBillingEntitlement(nowMillis),
                        billingUiState = billingUiState,
                    ),
                )
                StatusRow(
                    "Protection permission",
                    if (isAccessibilityServiceEnabled) {
                        "enabled"
                    } else {
                        "Protection permission missing"
                    },
                )
                StatusRow(
                    "Tamper protection",
                    if (isTamperProtectionEnabled) {
                        "active"
                    } else {
                        "optional inactive"
                    },
                )
                StatusRow("Protection status", entitlementState.protectionLabel())
                if (freeTestState == EntitlementState.FREE_TEST_EXPIRED && !hasBillingEntitlement) {
                    ErrorText("Free test period ended. ${BillingAvailability.DEFERRED_MESSAGE}")
                }
                if (!isAccessibilityServiceEnabled) {
                    ErrorText("Protection permission missing")
                }
                if (!settings.hasEnabledPlatforms) {
                    ErrorText("No protected apps selected")
                }
                if (!isProtectionActive) {
                    val inactiveMessage =
                        protectionInactiveMessage(
                            settings = settings,
                            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                            nowMillis = nowMillis,
                            hasBillingEntitlement = hasBillingEntitlement,
                        )
                    ErrorText(inactiveMessage)
                }
                Text(
                    text =
                        "Shorts Blocker Kids works when Protection is ON and Android " +
                            "Accessibility Service is active for YouTube Shorts, TikTok " +
                            "short-video feed, Instagram Reels, and Facebook Reels. " +
                            "Optional Device Admin tamper protection can make uninstall " +
                            "harder while active. If Accessibility permission is turned off, " +
                            "blocking stops.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        BillingActions(
            billingUiState = billingUiState,
            hasBillingEntitlement = hasBillingEntitlement,
            onSubscribe = onSubscribe,
            onRestorePurchases = onRestorePurchases,
            onManageSubscription = onManageSubscription,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (settings.accessibilityDisclosureAccepted) {
                    "Open Accessibility Settings"
                } else {
                    "Review Accessibility Disclosure"
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenPrivacyPolicy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Privacy Policy")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenTamperProtection,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (isTamperProtectionEnabled) {
                    "Review Tamper Protection"
                } else {
                    "Enable Tamper Protection"
                },
            )
        }
        onOpenDebugQa?.let { openDebugQa ->
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = openDebugQa,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Local QA")
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    isComplete: Boolean,
    incompleteLabel: String = "missing",
    isOptional: Boolean = false,
) {
    val status =
        when {
            isComplete -> "active"
            isOptional -> incompleteLabel
            else -> incompleteLabel
        }
    val color =
        when {
            isComplete -> MaterialTheme.colorScheme.primary
            isOptional -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.error
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun protectionInactiveMessage(
    settings: AppSettings,
    isAccessibilityServiceEnabled: Boolean,
    nowMillis: Long,
    hasBillingEntitlement: Boolean,
): String {
    if (
        settings.freeTestState(nowMillis) == EntitlementState.FREE_TEST_EXPIRED &&
        !hasBillingEntitlement
    ) {
        return "Free test expired"
    }

    if (!settings.protectionEnabled) {
        return "Protection inactive. Turn Protection ON to block short videos."
    }

    if (!settings.hasEnabledPlatforms) {
        return "Protection inactive. Enable at least one protected app."
    }

    if (!isAccessibilityServiceEnabled) {
        return "Protection inactive. Enable Accessibility Service."
    }

    if (settings.isTemporarilyAllowed(nowMillis)) {
        return "Protection inactive during temporary allow."
    }

    if (settings.freeTestState(nowMillis) == EntitlementState.FREE_TEST_NOT_STARTED) {
        return "Free test has not started. Enable protection permission to start " +
            "the 20-day test."
    }

    return "Protection inactive. Complete setup to block short videos."
}

private fun enabledAppsLabel(settings: AppSettings): String {
    val enabledNames =
        PlatformSupportMatrix.protectedEntries
            .filter { entry -> settings.isPlatformEnabled(entry.platformId) }
            .map { it.platformName }

    return if (enabledNames.isEmpty()) {
        "none"
    } else {
        enabledNames.joinToString()
    }
}

private fun EntitlementState.dashboardLabel(): String =
    when (this) {
        EntitlementState.FREE_TEST_NOT_STARTED -> "not started"
        EntitlementState.FREE_TEST_ACTIVE -> "Free test active"
        EntitlementState.FREE_TEST_EXPIRED -> "Free test expired"
        EntitlementState.SUBSCRIPTION_ACTIVE -> "Subscription active"
        EntitlementState.PROTECTION_PERMISSION_MISSING -> "Protection permission missing"
        EntitlementState.PROTECTION_ACTIVE -> "Protection active"
        EntitlementState.PROTECTION_LOCKED -> "Protection locked"
    }

private fun EntitlementState.protectionLabel(): String =
    when (this) {
        EntitlementState.PROTECTION_ACTIVE -> "Protection active"
        EntitlementState.PROTECTION_PERMISSION_MISSING -> "Protection permission missing"
        EntitlementState.PROTECTION_LOCKED -> "Protection locked"
        EntitlementState.FREE_TEST_ACTIVE -> "inactive"
        EntitlementState.FREE_TEST_NOT_STARTED -> "not started"
        EntitlementState.FREE_TEST_EXPIRED -> "Free test expired"
        EntitlementState.SUBSCRIPTION_ACTIVE -> "inactive"
    }

private fun protectionSwitchLabel(
    settings: AppSettings,
    isProtectionLocked: Boolean,
): String {
    if (isProtectionLocked) {
        return "LOCKED"
    }

    return if (settings.protectionEnabled) "ON" else "OFF"
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun BillingActions(
    billingUiState: BillingUiState,
    hasBillingEntitlement: Boolean,
    onSubscribe: () -> Unit,
    onRestorePurchases: () -> Unit,
    onManageSubscription: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Google Play subscription",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = billingUiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = BillingCopy.subscriptionTermsText(billingUiState.productPrice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (hasBillingEntitlement) {
                OutlinedButton(
                    onClick = onManageSubscription,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Manage subscription")
                }
            } else {
                Button(
                    onClick = onSubscribe,
                    enabled = billingUiState.canStartPurchase && !billingUiState.isPurchaseInProgress,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Subscribe with Google Play")
                }
            }
            OutlinedButton(
                onClick = onRestorePurchases,
                enabled = !billingUiState.isLoading && !billingUiState.isPurchaseInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore purchases")
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
) {
    ProtectionRow(label = label, value = value, control = null)
}

@Composable
private fun ProtectionRow(
    label: String,
    value: String,
    control: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        control?.invoke()
    }
}
