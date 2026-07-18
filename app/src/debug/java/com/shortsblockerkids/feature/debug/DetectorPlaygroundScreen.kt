package com.shortsblockerkids.feature.debug

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shortsblockerkids.BuildConfig
import com.shortsblockerkids.R
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
    val noScenarioName = stringResource(R.string.debug_no_scenario)
    val notRequestedMessage = stringResource(R.string.debug_not_requested)
    val snapshotRequestedMessage = stringResource(R.string.debug_snapshot_requested)
    val overlayRequestedMessage = stringResource(R.string.debug_overlay_requested)
    var selectedResult by remember(noScenarioName, notRequestedMessage) {
        mutableStateOf(
            PlaygroundResult(
                name = noScenarioName,
                result = DetectionResult.None,
                overlayMessage = notRequestedMessage,
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
            text = stringResource(R.string.debug_detector_playground_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QaRow(
                    stringResource(R.string.debug_detected_package),
                    RuntimeProtectionState.lastDetectorResultText()
                        ?: stringResource(R.string.debug_disabled),
                )
                QaRow(stringResource(R.string.debug_protection_mode), settings.selectedMode.name)
                QaRow(
                    stringResource(R.string.debug_blocking_decision),
                    RuntimeProtectionState.lastBlockingDecisionText()
                        ?: stringResource(R.string.debug_disabled),
                )
                QaRow(stringResource(R.string.debug_entitlement), entitlementState.name)
                QaRow(
                    stringResource(R.string.debug_subscription_state),
                    settings.billingEntitlementState.name,
                )
                QaRow(
                    stringResource(R.string.debug_backend),
                    if (BuildConfig.BILLING_BACKEND_BASE_URL.isBlank()) {
                        stringResource(R.string.debug_disabled)
                    } else {
                        stringResource(R.string.debug_enabled)
                    },
                )
                QaRow(stringResource(R.string.debug_billing_ui), billingUiState.statusMessage)
                QaRow(
                    stringResource(R.string.debug_protection_permission),
                    if (isAccessibilityServiceEnabled) {
                        stringResource(R.string.debug_enabled)
                    } else {
                        stringResource(R.string.debug_missing)
                    },
                )
            }
        }
        scenarios.forEach { scenario ->
            val scenarioName = stringResource(scenario.nameRes)
            Button(
                onClick = {
                    selectedResult =
                        PlaygroundResult(
                            name = scenarioName,
                            result = detector.detect(scenario.packageName, scenario.snapshot),
                            overlayMessage = selectedResult.overlayMessage,
                        )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(scenarioName)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.debug_scenario_format, selectedResult.name))
                Text(
                    stringResource(
                        R.string.debug_confidence_format,
                        selectedResult.result.confidence,
                    ),
                )
                Text(
                    stringResource(
                        R.string.debug_reasons_format,
                        selectedResult.result.reasons,
                    ),
                )
                Text(
                    stringResource(
                        R.string.debug_signals_format,
                        selectedResult.result.matchedSignals,
                    ),
                )
                Text(
                    stringResource(
                        R.string.debug_would_block_format,
                        selectedResult.result.shouldBlock,
                    ),
                )
                Text(
                    stringResource(
                        R.string.debug_test_overlay_format,
                        selectedResult.overlayMessage,
                    ),
                )
                Text(
                    stringResource(
                        R.string.debug_snapshot_format,
                        RuntimeProtectionState.lastDebugSnapshotText().toString(),
                    ),
                )
            }
        }
        Button(
            onClick = {
                RuntimeProtectionState.requestDebugSnapshot()
                selectedResult =
                    selectedResult.copy(
                        overlayMessage = snapshotRequestedMessage,
                    )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.debug_capture_youtube_snapshot))
        }
        Button(
            onClick = {
                RuntimeProtectionState.requestDebugOverlay()
                selectedResult =
                    selectedResult.copy(
                        overlayMessage = overlayRequestedMessage,
                    )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.debug_request_test_overlay))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.debug_back))
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
    @param:StringRes val nameRes: Int,
    val packageName: String?,
    val snapshot: AccessibilityTreeSnapshot,
)

private fun detectorPlaygroundScenarios(): List<PlaygroundScenario> =
    listOf(
        PlaygroundScenario(
            nameRes = R.string.debug_scenario_youtube_home,
            packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
            snapshot = homeSnapshot(),
        ),
        PlaygroundScenario(
            nameRes = R.string.debug_scenario_youtube_video,
            packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
            snapshot = normalVideoSnapshot(),
        ),
        PlaygroundScenario(
            nameRes = R.string.debug_scenario_search,
            packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
            snapshot = searchSnapshot(),
        ),
        PlaygroundScenario(
            nameRes = R.string.debug_scenario_shorts_high_confidence,
            packageName = YouTubeShortsDetector.YOUTUBE_PACKAGE,
            snapshot = shortsSnapshot(),
        ),
        PlaygroundScenario(
            nameRes = R.string.debug_scenario_non_youtube,
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
