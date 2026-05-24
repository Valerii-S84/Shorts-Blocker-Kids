package com.shortsblockerkids.feature.debug

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.BuildConfig
import com.shortsblockerkids.accessibility.AccessibilityNodeSignal
import com.shortsblockerkids.accessibility.AccessibilityTreeSnapshot
import com.shortsblockerkids.accessibility.DetectionResult
import com.shortsblockerkids.accessibility.RuntimeProtectionState
import com.shortsblockerkids.accessibility.YouTubeShortsDetector
import com.shortsblockerkids.core.billing.BillingUiState
import com.shortsblockerkids.core.entitlement.LocalEntitlementResolver
import com.shortsblockerkids.core.storage.AppSettings

@Composable
fun DetectorPlaygroundScreen(
    settings: AppSettings,
    isAccessibilityServiceEnabled: Boolean,
    billingUiState: BillingUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detector = remember { YouTubeShortsDetector() }
    val scenarios = remember { detectorPlaygroundScenarios() }
    val nowMillis = System.currentTimeMillis()
    val entitlementState =
        LocalEntitlementResolver.resolve(
            settings = settings,
            isProtectionPermissionGranted = isAccessibilityServiceEnabled,
            nowMillis = nowMillis,
        )
    var selectedResult by remember {
        mutableStateOf(
            PlaygroundResult(
                name = "No scenario",
                result = DetectionResult.None,
                overlayMessage = "not requested",
            ),
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Detector Playground",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QaRow("Detected package", RuntimeProtectionState.lastDetectorResultText() ?: "disabled")
                QaRow("Protection mode", settings.selectedMode.name)
                QaRow("Blocking decision", RuntimeProtectionState.lastBlockingDecisionText() ?: "disabled")
                QaRow("Entitlement", entitlementState.name)
                QaRow("Subscription state", settings.billingEntitlementState.name)
                QaRow("Backend", if (BuildConfig.BILLING_BACKEND_BASE_URL.isBlank()) "disabled" else "enabled")
                QaRow("Billing UI", billingUiState.statusMessage)
                QaRow("Protection permission", if (isAccessibilityServiceEnabled) "enabled" else "missing")
            }
        }
        scenarios.forEach { scenario ->
            Button(
                onClick = {
                    selectedResult =
                        PlaygroundResult(
                            name = scenario.name,
                            result = detector.detect(scenario.packageName, scenario.snapshot),
                            overlayMessage = selectedResult.overlayMessage,
                        )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(scenario.name)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Scenario: ${selectedResult.name}")
                Text("confidence: ${selectedResult.result.confidence}")
                Text("reasons: ${selectedResult.result.reasons}")
                Text("signals: ${selectedResult.result.matchedSignals}")
                Text("wouldBlock: ${selectedResult.result.shouldBlock}")
                Text("testOverlay: ${selectedResult.overlayMessage}")
                Text("snapshot: ${RuntimeProtectionState.lastDebugSnapshotText()}")
            }
        }
        Button(
            onClick = {
                RuntimeProtectionState.requestDebugSnapshot()
                selectedResult =
                    selectedResult.copy(
                        overlayMessage = "snapshot requested; open YouTube to capture next event",
                    )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Capture YouTube UI Snapshot")
        }
        Button(
            onClick = {
                RuntimeProtectionState.requestDebugOverlay()
                selectedResult =
                    selectedResult.copy(
                        overlayMessage = "overlay requested; open YouTube to consume next event",
                    )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Request Test Overlay")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun QaRow(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

private data class PlaygroundResult(
    val name: String,
    val result: DetectionResult,
    val overlayMessage: String,
)

private data class PlaygroundScenario(
    val name: String,
    val packageName: String?,
    val snapshot: AccessibilityTreeSnapshot,
)

private fun detectorPlaygroundScenarios(): List<PlaygroundScenario> =
    listOf(
        PlaygroundScenario(
            name = "Simulate YouTube Home",
            packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
            snapshot = homeSnapshot(),
        ),
        PlaygroundScenario(
            name = "Simulate normal YouTube video",
            packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
            snapshot = normalVideoSnapshot(),
        ),
        PlaygroundScenario(
            name = "Simulate Search",
            packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
            snapshot = searchSnapshot(),
        ),
        PlaygroundScenario(
            name = "Simulate Shorts HIGH confidence",
            packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
            snapshot = shortsSnapshot(),
        ),
        PlaygroundScenario(
            name = "Simulate non-YouTube app",
            packageName = "com.example.other",
            snapshot = shortsSnapshot(),
        ),
    )

private fun homeSnapshot(): AccessibilityTreeSnapshot =
    AccessibilityTreeSnapshot(
        nodes =
            listOf(
                node(width = 1080, height = 2200, right = 1080, bottom = 2200, depth = 0),
                node(contentDescriptionSignals = setOf("home"), left = 40, top = 2060),
                node(
                    viewIdResourceName = "com.google.android.youtube:id/bottom_bar_shorts",
                    contentDescriptionSignals = setOf("shorts"),
                    left = 300,
                    top = 2060,
                ),
                node(contentDescriptionSignals = setOf("subscriptions"), left = 560, top = 2060),
            ),
    )

private fun normalVideoSnapshot(): AccessibilityTreeSnapshot =
    AccessibilityTreeSnapshot(
        nodes =
            listOf(
                node(width = 1080, height = 2200, right = 1080, bottom = 2200, depth = 0),
                node(className = "android.view.View", width = 1080, height = 650, bottom = 650),
                node(contentDescriptionSignals = setOf("like"), left = 40, top = 900),
                node(contentDescriptionSignals = setOf("comments"), left = 460, top = 900),
                node(contentDescriptionSignals = setOf("share"), left = 620, top = 900),
            ),
    )

private fun searchSnapshot(): AccessibilityTreeSnapshot =
    AccessibilityTreeSnapshot(
        nodes =
            listOf(
                node(width = 1080, height = 2200, right = 1080, bottom = 2200, depth = 0),
                node(className = "android.widget.EditText", left = 80, top = 120, right = 1000),
                node(className = "androidx.recyclerview.widget.RecyclerView", isScrollable = true),
            ),
    )

private fun shortsSnapshot(): AccessibilityTreeSnapshot =
    AccessibilityTreeSnapshot(
        nodes =
            listOf(
                node(width = 1080, height = 2200, right = 1080, bottom = 2200, depth = 0),
                node(
                    className = "androidx.viewpager.widget.ViewPager",
                    viewIdResourceName = "com.google.android.youtube:id/reel_watch_sequence",
                    isScrollable = true,
                    width = 1080,
                    height = 2140,
                    right = 1080,
                    bottom = 2140,
                    depth = 2,
                ),
                node(contentDescriptionSignals = setOf("like"), left = 930, top = 760),
                node(contentDescriptionSignals = setOf("comments"), left = 930, top = 1110),
                node(contentDescriptionSignals = setOf("share"), left = 930, top = 1460),
            ),
    )

private fun node(
    className: String = "android.view.View",
    viewIdResourceName: String? = null,
    contentDescriptionSignals: Set<String> = emptySet(),
    isScrollable: Boolean = false,
    left: Int = 0,
    top: Int = 0,
    right: Int = left + 120,
    bottom: Int = top + 120,
    width: Int = right - left,
    height: Int = bottom - top,
    depth: Int = 1,
): AccessibilityNodeSignal =
    AccessibilityNodeSignal(
        className = className,
        viewIdResourceName = viewIdResourceName,
        contentDescriptionSignals = contentDescriptionSignals,
        isClickable = true,
        isScrollable = isScrollable,
        isVisibleToUser = true,
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        width = width,
        height = height,
        depth = depth,
    )
