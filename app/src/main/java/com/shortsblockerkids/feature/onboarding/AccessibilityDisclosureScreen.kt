package com.shortsblockerkids.feature.onboarding

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
fun AccessibilityDisclosureScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
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
            text = "Accessibility Permission",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text =
                "Shorts Blocker Kids uses Android Accessibility Service to detect when " +
                    "YouTube Shorts is open and show a blocking overlay.\n\n" +
                    "When Shorts is blocked, the parent PIN can temporarily allow access. " +
                    "Without the parent PIN, the blocking overlay stays active while Shorts " +
                    "is open.\n\n" +
                    "The app does not collect or send child data, YouTube history, video " +
                    "data, screen recordings, audio, messages, location, contacts, or " +
                    "browsing history. There is no app server, account, analytics, or ads. " +
                    "Rules and PIN protection stay locally on this phone.\n\n" +
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
            Text("I agree and continue")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onDecline,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Not now")
        }
    }
}
