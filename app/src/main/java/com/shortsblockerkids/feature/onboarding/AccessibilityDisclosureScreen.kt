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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AccessibilityDisclosureScreen(
    onAccept: () -> Unit,
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
            text = "Accessibility Permission",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text =
                "Shorts Blocker Kids uses Android Accessibility Service only to detect " +
                    "when YouTube Shorts is open and show a blocking screen.\n\n" +
                    "The app does not read messages.\n" +
                    "The app does not record the screen.\n" +
                    "The app does not record audio.\n" +
                    "The app does not save YouTube watch history.\n" +
                    "The app does not send data to our server.\n" +
                    "Rules stay locally on this phone.\n\n" +
                    "Blocking works when Protection is ON and Android Accessibility " +
                    "Service is active. If that permission is turned off or the app is " +
                    "removed, blocking stops.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("I understand and want to enable protection")
        }
    }
}
