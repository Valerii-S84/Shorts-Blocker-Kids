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
                    "protected short-video surfaces are open and show a blocking overlay. " +
                    "The protected surfaces are YouTube Shorts, TikTok short-video feed, " +
                    "Instagram Reels, and Facebook Reels. TikTok regional package " +
                    "com.ss.android.ugc.trill and Facebook Lite are not supported. " +
                    "YouTube Shorts has prior real-device smoke evidence. TikTok, " +
                    "Instagram Reels, and Facebook Reels have detector and unit coverage, " +
                    "but still require real-device QA before they are production-verified.\n\n" +
                    "When short videos are blocked, the parent PIN can temporarily allow " +
                    "access. Without the parent PIN, the blocking overlay stays active " +
                    "while a protected short-video surface is open.\n\n" +
                    "The app does not collect or send child data, supported app activity, " +
                    "watch history, video titles, URLs, account names, comments, messages, " +
                    "screen recordings, audio, location, contacts, browsing history, or raw " +
                    "Accessibility tree dumps. There is no account system, analytics, or ads. " +
                    "Backend behavior is limited to Google Play billing verification. Rules " +
                    "and PIN protection stay locally on this phone.\n\n" +
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
