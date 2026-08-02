package com.shortsblockerkids.feature.onboarding

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.R
import com.shortsblockerkids.presentation.dashboard.ProtectedPlatformItemUiModel

data class ProtectedAppsCallbacks(
    val onPlatformEnabledChanged: (String, Boolean) -> Unit,
    val onContinue: () -> Unit,
)

@Composable
fun ProtectedAppsScreen(
    items: List<ProtectedPlatformItemUiModel>,
    callbacks: ProtectedAppsCallbacks,
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
            text = stringResource(R.string.protected_apps_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.protected_apps_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(20.dp))
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(item.nameRes),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = item.isSelected,
                    onCheckedChange =
                        if (item.isClickable) {
                            { enabled ->
                                callbacks.onPlatformEnabledChanged(item.platformId, enabled)
                            }
                        } else {
                            null
                        },
                    enabled = item.isEnabled,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = callbacks.onContinue,
            enabled = items.any(ProtectedPlatformItemUiModel::isSelected),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.protected_apps_continue))
        }
    }
}
