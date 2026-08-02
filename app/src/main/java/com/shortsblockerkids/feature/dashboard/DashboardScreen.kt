package com.shortsblockerkids.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.shortsblockerkids.presentation.billing.BillingUiState
import com.shortsblockerkids.presentation.billing.billingMessageText
import com.shortsblockerkids.presentation.billing.billingSubscriptionStatusText
import com.shortsblockerkids.presentation.billing.billingSubscriptionTermsText
import com.shortsblockerkids.presentation.dashboard.DashboardUiState
import com.shortsblockerkids.presentation.dashboard.DashboardWarningUiModel
import com.shortsblockerkids.presentation.dashboard.ProtectedPlatformItemUiModel

data class DashboardCallbacks(
    val protection: DashboardProtectionCallbacks,
    val billing: DashboardBillingCallbacks,
    val navigation: DashboardNavigationCallbacks,
)

data class DashboardProtectionCallbacks(
    val onProtectionChanged: (Boolean) -> Unit,
    val onPlatformEnabledChanged: (String, Boolean) -> Unit,
)

data class DashboardBillingCallbacks(
    val onSubscribe: () -> Unit,
    val onRestorePurchases: () -> Unit,
    val onManageSubscription: () -> Unit,
)

data class DashboardNavigationCallbacks(
    val onOpenAccessibilitySettings: () -> Unit,
    val onOpenPrivacyPolicy: () -> Unit,
    val onOpenTamperProtection: () -> Unit,
    val onOpenDebugQa: (() -> Unit)? = null,
)

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    callbacks: DashboardCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
                ).verticalScroll(rememberScrollState())
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
                    value = stringResource(uiState.protection.switchStatusRes),
                    control = {
                        Switch(
                            checked =
                                uiState.protection.isEnabled &&
                                    !uiState.protection.isLocked,
                            onCheckedChange = callbacks.protection.onProtectionChanged,
                            enabled = !uiState.protection.isLocked,
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
                    uiState.setup.isPinConfigured,
                )
                ChecklistRow(
                    stringResource(R.string.dashboard_checklist_protected_apps),
                    uiState.setup.hasProtectedPlatforms,
                )
                ChecklistRow(
                    stringResource(R.string.dashboard_checklist_accessibility_disclosure),
                    uiState.setup.isAccessibilityDisclosureAccepted,
                )
                ChecklistRow(
                    stringResource(R.string.dashboard_checklist_accessibility_service),
                    uiState.setup.isAccessibilityServiceEnabled,
                )
                ChecklistRow(
                    label = stringResource(R.string.dashboard_checklist_tamper_protection),
                    isComplete = uiState.setup.isTamperProtectionEnabled,
                    incompleteLabel = stringResource(R.string.status_optional),
                    isOptional = true,
                )
                StatusRow(
                    stringResource(R.string.dashboard_platform_support),
                    protectedPlatformSummary(uiState.platforms.protected),
                )
                StatusRow(
                    stringResource(R.string.dashboard_enabled_apps),
                    enabledAppsLabel(uiState.platforms.protected),
                )
                uiState.platforms.protected.forEach { item ->
                    ProtectionRow(
                        label = stringResource(item.nameRes),
                        value =
                            if (item.isSelected) {
                                stringResource(R.string.status_enabled)
                            } else {
                                stringResource(R.string.status_disabled)
                            },
                        control = {
                            Switch(
                                checked = item.isSelected,
                                onCheckedChange = { enabled ->
                                    callbacks.protection.onPlatformEnabledChanged(
                                        item.platformId,
                                        enabled,
                                    )
                                },
                                enabled = item.isEnabled && item.isClickable,
                            )
                        },
                    )
                }
                StatusRow(
                    stringResource(R.string.dashboard_not_supported),
                    unsupportedPlatformSummary(uiState.platforms.unsupported),
                )
                StatusRow(
                    stringResource(R.string.dashboard_pin),
                    if (uiState.setup.isPinConfigured) {
                        stringResource(R.string.status_created)
                    } else {
                        stringResource(R.string.status_not_created)
                    },
                )
                StatusRow(
                    stringResource(R.string.dashboard_free_test),
                    stringResource(uiState.entitlement.freeTestStatusRes),
                )
                uiState.entitlement.freeTestDaysRemaining?.let { daysRemaining ->
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
                    billingSubscriptionStatusText(
                        hasBillingEntitlement = uiState.billing.hasEntitlement,
                        billingUiState = uiState.billing.uiState,
                    ),
                )
                StatusRow(
                    stringResource(R.string.dashboard_protection_permission),
                    if (uiState.setup.isAccessibilityServiceEnabled) {
                        stringResource(R.string.status_enabled)
                    } else {
                        stringResource(R.string.status_protection_permission_missing)
                    },
                )
                StatusRow(
                    stringResource(R.string.dashboard_tamper_protection),
                    if (uiState.setup.isTamperProtectionEnabled) {
                        stringResource(R.string.status_active)
                    } else {
                        stringResource(R.string.status_optional_inactive)
                    },
                )
                StatusRow(
                    stringResource(R.string.dashboard_protection_status),
                    stringResource(uiState.protection.protectionStatusRes),
                )
                uiState.warnings.forEach { warning ->
                    ErrorText(warningText(warning))
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
            billingUiState = uiState.billing.uiState,
            hasBillingEntitlement = uiState.billing.hasEntitlement,
            onSubscribe = callbacks.billing.onSubscribe,
            onRestorePurchases = callbacks.billing.onRestorePurchases,
            onManageSubscription = callbacks.billing.onManageSubscription,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = callbacks.navigation.onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(uiState.actions.accessibilitySettingsLabelRes))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = callbacks.navigation.onOpenPrivacyPolicy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.dashboard_privacy_policy))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = callbacks.navigation.onOpenTamperProtection,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(uiState.actions.tamperProtectionLabelRes))
        }
        callbacks.navigation.onOpenDebugQa?.let { openDebugQa ->
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
private fun enabledAppsLabel(items: List<ProtectedPlatformItemUiModel>): String {
    val enabledNames =
        items.filter(ProtectedPlatformItemUiModel::isSelected).map { item ->
            stringResource(item.nameRes)
        }

    return if (enabledNames.isEmpty()) {
        stringResource(R.string.status_none)
    } else {
        enabledNames.joinToString()
    }
}

@Composable
private fun protectedPlatformSummary(items: List<ProtectedPlatformItemUiModel>): String =
    items
        .map { item ->
            stringResource(
                R.string.platform_status_item_format,
                stringResource(item.nameRes),
                stringResource(item.statusRes),
            )
        }.joinToString()

@Composable
private fun unsupportedPlatformSummary(items: List<ProtectedPlatformItemUiModel>): String =
    items
        .map { item ->
            stringResource(
                R.string.platform_unsupported_item_format,
                stringResource(item.nameRes),
                item.packageName,
            )
        }.joinToString()

@Composable
private fun warningText(warning: DashboardWarningUiModel): String =
    when (warning) {
        DashboardWarningUiModel.FREE_TEST_ENDED ->
            stringResource(
                R.string.error_free_test_ended_with_detail,
                stringResource(R.string.billing_subscription_managed_by_google_play),
            )
        DashboardWarningUiModel.PROTECTION_PERMISSION_MISSING ->
            stringResource(R.string.error_protection_permission_missing)
        DashboardWarningUiModel.NO_PROTECTED_APPS_SELECTED ->
            stringResource(R.string.error_no_protected_apps_selected)
        DashboardWarningUiModel.FREE_TEST_EXPIRED ->
            stringResource(R.string.error_free_test_expired)
        DashboardWarningUiModel.PROTECTION_DISABLED ->
            stringResource(R.string.error_protection_inactive_disabled)
        DashboardWarningUiModel.NO_PROTECTED_APPS ->
            stringResource(R.string.error_protection_inactive_no_apps)
        DashboardWarningUiModel.ACCESSIBILITY_DISABLED ->
            stringResource(R.string.error_protection_inactive_accessibility)
        DashboardWarningUiModel.TEMPORARY_ALLOW_ACTIVE ->
            stringResource(R.string.error_protection_inactive_temporary_allow)
        DashboardWarningUiModel.FREE_TEST_NOT_STARTED ->
            stringResource(R.string.error_free_test_not_started)
        DashboardWarningUiModel.SETUP_INCOMPLETE ->
            stringResource(R.string.error_protection_inactive_setup)
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
                text = billingMessageText(billingUiState.message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = billingSubscriptionTermsText(billingUiState.productPrice),
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
