package com.shortsblockerkids.feature.debug

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shortsblockerkids.core.billing.BillingUiState
import com.shortsblockerkids.core.storage.AppSettings

@Composable
fun DetectorPlaygroundScreen(
    settings: AppSettings,
    isAccessibilityServiceEnabled: Boolean,
    billingUiState: BillingUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier)
}
