package com.shortsblockerkids.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.accessibility.PlatformSupportMatrix
import com.shortsblockerkids.core.storage.AppSettings

@Composable
fun ProtectedAppsScreen(
    settings: AppSettings,
    onPlatformEnabledChanged: (String, Boolean) -> Unit,
    onContinue: () -> Unit,
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
            text = "Select Protected Apps",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text =
                "Choose which supported short-video surfaces this parent PIN " +
                    "should protect on this phone.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PlatformSupportMatrix.protectedEntries.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.platformName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = settings.isPlatformEnabled(entry.platformId),
                    onCheckedChange = { enabled ->
                        onPlatformEnabledChanged(entry.platformId, enabled)
                    },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onContinue,
            enabled = settings.hasEnabledPlatforms,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
    }
}
