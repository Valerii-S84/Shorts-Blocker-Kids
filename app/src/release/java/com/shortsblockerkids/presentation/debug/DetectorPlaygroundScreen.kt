package com.shortsblockerkids.presentation.debug

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shortsblockerkids.presentation.dashboard.DashboardUiState

@Composable
fun DetectorPlaygroundScreen(
    uiState: DashboardUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier)
}
