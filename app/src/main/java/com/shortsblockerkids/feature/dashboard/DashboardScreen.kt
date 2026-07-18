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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.R
import com.shortsblockerkids.accessibility.PlatformSupportMatrix
import com.shortsblockerkids.accessibility.PlatformSupportStatus
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
            text = stringResource(R.string.dashboard_title),
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
                    label = stringResource(R.string.dashboard_title),
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
                    text = stringResource(R.string.dashboard_setup_checklist),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ChecklistRow(
                    stringResource(R.string.dashboard_checklist_parent_pin),
                    settings.isPinCreated,
                )
                ChecklistRow(
                    stringResource(R.string.dashboard_checklist_protected_apps),
                    settings.hasEnabledPlatforms,
                )
                ChecklistRow(
                    stringResource(R.string.dashboard_checklist_accessibility_disclosure),
                    settings.accessibilityDisclosureAccepted,
                )
                ChecklistRow(
                    stringResource(R.string.dashboard_checklist_accessibility_service),
                    isAccessibilityServiceEnabled,
                )
                ChecklistRow(
                    label = stringResource(R.string.dashboard_checklist_tamper_protection),
                    isComplete = isTamperProtectionEnabled,
                    incompleteLabel = stringResource(R.string.status_optional),
                    isOptional = true,
                )
                StatusRow(
                    stringResource(R.string.dashboard_platform_support),
                    protectedPlatformSummary(),
                )
                StatusRow(
                    stringResource(R.string.dashboard_enabled_apps),
                    enabledAppsLabel(settings),
                )
                PlatformSupportMatrix.protectedEntries.forEach { entry ->
                    ProtectionRow(
                        label = stringResource(entry.platformNameRes),
                        value =
                            if (settings.isPlatformEnabled(entry.platformId)) {
                                stringResource(R.string.status_enabled)
                            } else {
                                stringResource(R.string.status_disabled)
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
                StatusRow(
                    stringResource(R.string.dashboard_not_supported),
                    unsupportedPlatformSummary(),
                )
                StatusRow(
                    stringResource(R.string.dashboard_pin),
                    if (settings.isPinCreated) {
                        stringResource(R.string.status_created)
                    } else {
                        stringResource(R.string.status_not_created)
                    },
                )
                StatusRow(
                    stringResource(R.string.dashboard_free_test),
                    freeTestState.dashboardLabel(),
                )
                settings.freeTestDaysRemaining(nowMillis)?.let { daysRemaining ->
                    StatusRow(
                        stringResource(R.string.dashboard_days_remaining_label),
                        pluralStringResource(
                            R.plurals.dashboard_days_remaining,
                            daysRemaining,
                            daysRemaining,
                        ),
                    )
                }
                StatusRow(
                    stringResource(R.string.dashboard_subscription),
                    BillingCopy.subscriptionStatusLabel(
                        hasBillingEntitlement = settings.hasBillingEntitlement(nowMillis),
                        billingUiState = billingUiState,
                    ),
                )
                StatusRow(
                    stringResource(R.string.dashboard_protection_permission),
                    if (isAccessibilityServiceEnabled) {
                        stringResource(R.string.status_enabled)
                    } else {
                        stringResource(R.string.status_protection_permission_missing)
                    },
                )
                StatusRow(
                    stringResource(R.string.dashboard_tamper_protection),
                    if (isTamperProtectionEnabled) {
                        stringResource(R.string.status_active)
                    } else {
                        stringResource(R.string.status_optional_inactive)
                    },
                )
                StatusRow(
                    stringResource(R.string.dashboard_protection_status),
                    entitlementState.protectionLabel(),
                )
                if (freeTestState == EntitlementState.FREE_TEST_EXPIRED && !hasBillingEntitlement) {
                    ErrorText(
                        stringResource(
                            R.string.error_free_test_ended_with_detail,
                            BillingAvailability.DEFERRED_MESSAGE,
                        ),
                    )
                }
                if (!isAccessibilityServiceEnabled) {
                    ErrorText(stringResource(R.string.error_protection_permission_missing))
                }
                if (!settings.hasEnabledPlatforms) {
                    ErrorText(stringResource(R.string.error_no_protected_apps_selected))
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
                    text = stringResource(R.string.dashboard_description),
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
                    stringResource(R.string.dashboard_open_accessibility_settings)
                } else {
                    stringResource(R.string.dashboard_review_accessibility_disclosure)
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenPrivacyPolicy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.dashboard_privacy_policy))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenTamperProtection,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (isTamperProtectionEnabled) {
                    stringResource(R.string.dashboard_review_tamper_protection)
                } else {
                    stringResource(R.string.dashboard_enable_tamper_protection)
                },
            )
        }
        onOpenDebugQa?.let { openDebugQa ->
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = openDebugQa,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.dashboard_local_qa))
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    isComplete: Boolean,
    incompleteLabel: String? = null,
    isOptional: Boolean = false,
) {
    val status =
        when {
            isComplete -> stringResource(R.string.status_active)
            else -> incompleteLabel ?: stringResource(R.string.status_missing)
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

@Composable
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
        return stringResource(R.string.error_free_test_expired)
    }

    if (!settings.protectionEnabled) {
        return stringResource(R.string.error_protection_inactive_disabled)
    }

    if (!settings.hasEnabledPlatforms) {
        return stringResource(R.string.error_protection_inactive_no_apps)
    }

    if (!isAccessibilityServiceEnabled) {
        return stringResource(R.string.error_protection_inactive_accessibility)
    }

    if (settings.isTemporarilyAllowed(nowMillis)) {
        return stringResource(R.string.error_protection_inactive_temporary_allow)
    }

    if (settings.freeTestState(nowMillis) == EntitlementState.FREE_TEST_NOT_STARTED) {
        return stringResource(R.string.error_free_test_not_started)
    }

    return stringResource(R.string.error_protection_inactive_setup)
}

@Composable
private fun enabledAppsLabel(settings: AppSettings): String {
    val enabledNames = mutableListOf<String>()
    for (entry in PlatformSupportMatrix.protectedEntries) {
        if (settings.isPlatformEnabled(entry.platformId)) {
            enabledNames += stringResource(entry.platformNameRes)
        }
    }

    return if (enabledNames.isEmpty()) {
        stringResource(R.string.status_none)
    } else {
        enabledNames.joinToString()
    }
}

@Composable
private fun protectedPlatformSummary(): String {
    val summaries = mutableListOf<String>()
    for (entry in PlatformSupportMatrix.protectedEntries) {
        summaries +=
            stringResource(
                R.string.platform_status_item_format,
                stringResource(entry.platformNameRes),
                platformStatusLabel(entry.status),
            )
    }
    return summaries.joinToString()
}

@Composable
private fun unsupportedPlatformSummary(): String {
    val summaries = mutableListOf<String>()
    for (entry in PlatformSupportMatrix.unsupportedEntries) {
        summaries +=
            stringResource(
                R.string.platform_unsupported_item_format,
                stringResource(entry.platformNameRes),
                entry.packageName,
            )
    }
    return summaries.joinToString()
}

@Composable
private fun platformStatusLabel(status: PlatformSupportStatus): String =
    when (status) {
        PlatformSupportStatus.SUPPORTED ->
            stringResource(R.string.platform_status_supported)
        PlatformSupportStatus.SUPPORTED_BY_CODE_NEEDS_REAL_DEVICE_QA ->
            stringResource(R.string.platform_status_supported_needs_qa)
        PlatformSupportStatus.NOT_SUPPORTED ->
            stringResource(R.string.platform_status_not_supported)
    }

@Composable
private fun EntitlementState.dashboardLabel(): String =
    when (this) {
        EntitlementState.FREE_TEST_NOT_STARTED -> stringResource(R.string.status_not_started)
        EntitlementState.FREE_TEST_ACTIVE -> stringResource(R.string.status_free_test_active)
        EntitlementState.FREE_TEST_EXPIRED -> stringResource(R.string.status_free_test_expired)
        EntitlementState.SUBSCRIPTION_ACTIVE -> stringResource(R.string.status_subscription_active)
        EntitlementState.PROTECTION_PERMISSION_MISSING ->
            stringResource(R.string.status_protection_permission_missing)
        EntitlementState.PROTECTION_ACTIVE -> stringResource(R.string.status_protection_active)
        EntitlementState.PROTECTION_LOCKED -> stringResource(R.string.status_protection_locked)
    }

@Composable
private fun EntitlementState.protectionLabel(): String =
    when (this) {
        EntitlementState.PROTECTION_ACTIVE -> stringResource(R.string.status_protection_active)
        EntitlementState.PROTECTION_PERMISSION_MISSING ->
            stringResource(R.string.status_protection_permission_missing)
        EntitlementState.PROTECTION_LOCKED -> stringResource(R.string.status_protection_locked)
        EntitlementState.FREE_TEST_ACTIVE -> stringResource(R.string.status_inactive)
        EntitlementState.FREE_TEST_NOT_STARTED -> stringResource(R.string.status_not_started)
        EntitlementState.FREE_TEST_EXPIRED -> stringResource(R.string.status_free_test_expired)
        EntitlementState.SUBSCRIPTION_ACTIVE -> stringResource(R.string.status_inactive)
    }

@Composable
private fun protectionSwitchLabel(
    settings: AppSettings,
    isProtectionLocked: Boolean,
): String {
    if (isProtectionLocked) {
        return stringResource(R.string.status_locked)
    }

    return if (settings.protectionEnabled) {
        stringResource(R.string.status_on)
    } else {
        stringResource(R.string.status_off)
    }
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
                text = stringResource(R.string.dashboard_google_play_subscription),
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
                    Text(stringResource(R.string.dashboard_manage_subscription))
                }
            } else {
                Button(
                    onClick = onSubscribe,
                    enabled = billingUiState.canStartPurchase && !billingUiState.isPurchaseInProgress,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.dashboard_subscribe_google_play))
                }
            }
            OutlinedButton(
                onClick = onRestorePurchases,
                enabled = !billingUiState.isLoading && !billingUiState.isPurchaseInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.dashboard_restore_purchases))
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
