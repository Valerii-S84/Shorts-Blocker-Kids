package com.shortsblockerkids.feature.privacy

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.R

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.privacy_policy_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        PrivacySection(
            titleRes = R.string.privacy_policy_local_app_title,
            bodyRes = R.string.privacy_policy_local_app_body,
        )
        PrivacySection(
            titleRes = R.string.privacy_policy_accessibility_service_title,
            bodyRes = R.string.privacy_policy_accessibility_service_body,
        )
        PrivacySection(
            titleRes = R.string.privacy_policy_tamper_protection_title,
            bodyRes = R.string.privacy_policy_tamper_protection_body,
        )
        PrivacySection(
            titleRes = R.string.privacy_policy_local_data_title,
            bodyRes = R.string.privacy_policy_local_data_body,
        )
        PrivacySection(
            titleRes = R.string.privacy_policy_data_collection_title,
            bodyRes = R.string.privacy_policy_data_collection_body,
        )
        PrivacySection(
            titleRes = R.string.privacy_policy_payments_title,
            bodyRes = R.string.privacy_policy_payments_body,
        )
        PrivacySection(
            titleRes = R.string.privacy_policy_limitations_title,
            bodyRes = R.string.privacy_policy_limitations_body,
        )
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.common_back))
        }
    }
}

@Composable
private fun PrivacySection(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
