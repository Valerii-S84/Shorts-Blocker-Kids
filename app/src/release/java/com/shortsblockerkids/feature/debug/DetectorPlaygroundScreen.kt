package com.shortsblockerkids.feature.debug

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.core.billing.BillingUiState

@Composable
fun DetectorPlaygroundScreen(
    settings: AppSettingsSnapshot,
    isAccessibilityServiceEnabled: Boolean,
    billingUiState: BillingUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier)
}
