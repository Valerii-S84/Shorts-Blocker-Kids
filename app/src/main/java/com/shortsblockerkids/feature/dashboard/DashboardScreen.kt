package com.shortsblockerkids.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.core.billing.BillingAvailability
import com.shortsblockerkids.core.entitlement.LocalEntitlementResolver
import com.shortsblockerkids.core.model.EntitlementState
import com.shortsblockerkids.core.storage.AppSettings

@Composable
fun DashboardScreen(
    settings: AppSettings,
    isAccessibilityServiceEnabled: Boolean,
    onProtectionChanged: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
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

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "YouTube Shorts Protection",
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
                    label = "YouTube Shorts Protection",
                    value = protectionSwitchLabel(settings, isProtectionLocked),
                    control = {
                        Switch(
                            checked = settings.protectionEnabled && !isProtectionLocked,
                            onCheckedChange = onProtectionChanged,
                            enabled = !isProtectionLocked,
                        )
                    },
                )
                StatusRow("PIN", if (settings.isPinCreated) "created" else "not created")
                StatusRow(
                    "Free test",
                    freeTestState.dashboardLabel(),
                )
                settings.freeTestDaysRemaining(nowMillis)?.let { daysRemaining ->
                    StatusRow("Days remaining", "$daysRemaining days remaining")
                }
                StatusRow(
                    "Protection permission",
                    if (isAccessibilityServiceEnabled) {
                        "enabled"
                    } else {
                        "Protection permission missing"
                    },
                )
                StatusRow("Protection status", entitlementState.protectionLabel())
                if (freeTestState == EntitlementState.FREE_TEST_EXPIRED) {
                    ErrorText("Free test period ended. ${BillingAvailability.DEFERRED_MESSAGE}")
                }
                if (!isAccessibilityServiceEnabled) {
                    ErrorText("Protection permission missing")
                }
                if (!isProtectionActive) {
                    val inactiveMessage =
                        protectionInactiveMessage(
                            settings = settings,
                            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                            nowMillis = nowMillis,
                        )
                    ErrorText(inactiveMessage)
                }
                Text(
                    text =
                        "Shorts Blocker Kids works when Protection is ON and Android " +
                            "Accessibility Service is active. If that permission is turned off " +
                            "or the app is removed, blocking stops.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Accessibility Settings")
        }
    }
}

private fun protectionInactiveMessage(
    settings: AppSettings,
    isAccessibilityServiceEnabled: Boolean,
    nowMillis: Long,
): String {
    if (settings.freeTestState(nowMillis) == EntitlementState.FREE_TEST_EXPIRED) {
        return "Free test expired"
    }

    if (!settings.protectionEnabled) {
        return "Protection inactive. Turn Protection ON to block Shorts."
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

    return "Protection inactive. Complete setup to block Shorts."
}

private fun EntitlementState.dashboardLabel(): String =
    when (this) {
        EntitlementState.FREE_TEST_NOT_STARTED -> "not started"
        EntitlementState.FREE_TEST_ACTIVE -> "Free test active"
        EntitlementState.FREE_TEST_EXPIRED -> "Free test expired"
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
