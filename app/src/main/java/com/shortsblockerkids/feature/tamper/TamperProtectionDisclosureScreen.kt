package com.shortsblockerkids.feature.tamper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TamperProtectionDisclosureScreen(
    isTamperProtectionEnabled: Boolean,
    onEnableTamperProtection: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            text = "Tamper Protection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text =
                "Device Admin tamper protection is optional. It makes Shorts " +
                    "Blocker Kids harder to uninstall while it is active, so a " +
                    "child cannot remove protection as easily.\n\n" +
                    "It does not replace Android Accessibility Service. Blocking " +
                    "still works only when Accessibility protection is enabled. " +
                    "This app will not block Android Settings, change hidden " +
                    "system settings, wipe the device, lock the screen, reset " +
                    "passwords, or manage other apps.\n\n" +
                    "If someone tries to disable tamper protection, Android will " +
                    "show a warning before allowing the Device Admin status to be " +
                    "turned off.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text =
                if (isTamperProtectionEnabled) {
                    "Tamper protection is active"
                } else {
                    "Tamper protection is optional and currently inactive"
                },
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (isTamperProtectionEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onEnableTamperProtection,
            enabled = !isTamperProtectionEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("I understand, open Device Admin")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isTamperProtectionEnabled) "Done" else "Not now")
        }
    }
}
