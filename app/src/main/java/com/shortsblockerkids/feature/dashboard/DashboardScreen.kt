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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.core.model.SubscriptionState
import com.shortsblockerkids.core.storage.AppSettings

@Composable
fun DashboardScreen(
    settings: AppSettings,
    isAccessibilityServiceEnabled: Boolean,
    lastDetectorResult: String?,
    billingMessage: String?,
    onOpenDetectorPlayground: (() -> Unit)?,
    onProtectionChanged: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onStartMonthlySubscription: () -> Unit,
    onStartYearlySubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isProtectionActive =
        settings.canProtect(System.currentTimeMillis()) && isAccessibilityServiceEnabled
    val runtimeStatus =
        if (isProtectionActive) {
            "active"
        } else {
            "inactive"
        }

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
                    value = if (settings.protectionEnabled) "ON" else "OFF",
                    control = {
                        Switch(
                            checked = settings.protectionEnabled,
                            onCheckedChange = onProtectionChanged,
                        )
                    },
                )
                StatusRow("PIN", if (settings.isPinCreated) "created" else "not created")
                StatusRow(
                    "Accessibility Service",
                    if (isAccessibilityServiceEnabled) "enabled" else "disabled",
                )
                StatusRow("Protection status", runtimeStatus)
                StatusRow("Subscription", settings.subscriptionStateCached.dashboardLabel())
                if (settings.subscriptionStateCached == SubscriptionState.EXPIRED) {
                    Text(
                        text = "Subscription ended. Shorts blocking is paused until access is active.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (settings.subscriptionStateCached == SubscriptionState.ON_HOLD) {
                    Text(
                        text = "Subscription is on hold. Update Google Play billing to resume protection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (!isProtectionActive) {
                    val inactiveMessage =
                        protectionInactiveMessage(
                            settings = settings,
                            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
                        )
                    Text(
                        text = inactiveMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text =
                        "Shorts Blocker Kids works when Protection is ON and Android " +
                            "Accessibility Service is active. If that permission is turned off " +
                            "or the app is removed, blocking stops.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (lastDetectorResult != null) {
                    StatusRow("Last detector result", lastDetectorResult)
                }
                if (billingMessage != null) {
                    StatusRow("Billing", billingMessage)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onStartMonthlySubscription,
                modifier = Modifier.weight(1f),
            ) {
                Text("Monthly")
            }
            OutlinedButton(
                onClick = onStartYearlySubscription,
                modifier = Modifier.weight(1f),
            ) {
                Text("Yearly")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Accessibility Settings")
        }
        if (onOpenDetectorPlayground != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenDetectorPlayground,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Detector Playground")
            }
        }
    }
}

private fun protectionInactiveMessage(
    settings: AppSettings,
    isAccessibilityServiceEnabled: Boolean,
): String {
    if (!settings.protectionEnabled) {
        return "Protection inactive. Turn Protection ON to block Shorts."
    }

    if (!isAccessibilityServiceEnabled) {
        return "Protection inactive. Enable Accessibility Service."
    }

    if (settings.isTemporarilyAllowed(System.currentTimeMillis())) {
        return "Protection inactive during temporary allow."
    }

    if (settings.subscriptionStateCached == SubscriptionState.EXPIRED) {
        return "Protection inactive. Subscription ended."
    }

    if (settings.subscriptionStateCached == SubscriptionState.ON_HOLD) {
        return "Protection inactive. Subscription is on hold."
    }

    if (settings.subscriptionStateCached == SubscriptionState.UNKNOWN) {
        return "Protection inactive. Subscription status is still checking."
    }

    return "Protection inactive. Complete setup to block Shorts."
}

private fun SubscriptionState.dashboardLabel(): String =
    when (this) {
        SubscriptionState.UNKNOWN -> "checking"
        SubscriptionState.TRIAL -> "trial"
        SubscriptionState.ACTIVE -> "active"
        SubscriptionState.GRACE_PERIOD -> "grace period"
        SubscriptionState.ON_HOLD -> "on hold"
        SubscriptionState.CANCELED_BUT_ACTIVE_UNTIL_END -> "active until paid period ends"
        SubscriptionState.EXPIRED -> "expired"
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
