package com.shortsblockerkids.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.R

@Composable
fun EnableAccessibilityScreen(
    isAccessibilityServiceEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onEnabled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.enable_accessibility_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.enable_accessibility_instructions),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text =
                if (isAccessibilityServiceEnabled) {
                    stringResource(R.string.enable_accessibility_status_enabled)
                } else {
                    stringResource(R.string.enable_accessibility_status_disabled)
                },
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (isAccessibilityServiceEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.enable_accessibility_open_settings))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.enable_accessibility_confirm_enabled))
        }
    }
}
