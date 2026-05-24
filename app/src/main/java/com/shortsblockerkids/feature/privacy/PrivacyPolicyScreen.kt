package com.shortsblockerkids.feature.privacy

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
            text = "Privacy Policy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        PrivacySection(
            title = "Local app",
            body =
                "Shorts Blocker Kids works locally on this phone. It does not " +
                    "require an account. Production subscription verification may " +
                    "use a billing backend limited to Google Play entitlement status.",
        )
        PrivacySection(
            title = "Accessibility Service",
            body =
                "The app uses Android Accessibility Service to detect supported " +
                    "short-video surfaces and show a blocking overlay when protection " +
                    "is enabled. YouTube Shorts is supported. TikTok main, Instagram " +
                    "Reels, and Facebook Reels have code-level detectors and still need " +
                    "real-device QA. TikTok regional package com.ss.android.ugc.trill " +
                    "and Facebook Lite are not supported.",
        )
        PrivacySection(
            title = "Local data",
            body =
                "Protection settings and parent PIN hash metadata stay on this " +
                    "phone. The parent PIN is not stored as plain text.",
        )
        PrivacySection(
            title = "Data collection",
            body =
                "The app does not collect or send child data, supported app activity, " +
                    "watch history, video titles, URLs, account names, comments, messages, " +
                    "screen recordings, audio, location, contacts, browsing history, or raw " +
                    "Accessibility tree dumps.",
        )
        PrivacySection(
            title = "Payments",
            body =
                "Subscriptions are handled through Google Play Billing. Payment " +
                    "data is processed by Google Play under Google Play terms. " +
                    "This app stores local subscription entitlement status. Backend " +
                    "verification, when enabled, uses only billing technical data and " +
                    "stores hashed purchase tokens.",
        )
        PrivacySection(
            title = "Limitations",
            body =
                "Blocking works when Protection is ON and Android Accessibility " +
                    "Service is active. If that permission is turned off or the " +
                    "app is removed, blocking stops.",
        )
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
