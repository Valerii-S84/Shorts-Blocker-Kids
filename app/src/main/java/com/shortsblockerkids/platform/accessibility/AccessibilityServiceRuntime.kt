package com.shortsblockerkids.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.MainActivity
import com.shortsblockerkids.accessibility.BlockingDecisionController
import com.shortsblockerkids.application.model.AccessibilityProtectionState
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.ProtectionSettingsPort
import com.shortsblockerkids.application.port.SettingsStatePort
import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.application.settings.ObserveSettingsUseCase
import com.shortsblockerkids.application.settings.SettingsReadState
import com.shortsblockerkids.application.port.MonotonicTimeProvider
import com.shortsblockerkids.application.protection.canProtect
import com.shortsblockerkids.domain.detection.SupportedPlatform
import com.shortsblockerkids.platform.accessibility.detection.ProductionDetectorRegistry
import com.shortsblockerkids.platform.accessibility.diagnostics.DebugAccessibilityLogger
import com.shortsblockerkids.platform.accessibility.diagnostics.DetectorDebugSnapshotStore
import com.shortsblockerkids.platform.accessibility.diagnostics.configureAccessibilityDiagnostics
import com.shortsblockerkids.platform.accessibility.overlay.BlockOverlayController
import com.shortsblockerkids.platform.accessibility.overlay.TemporaryAllowNavigator
import com.shortsblockerkids.platform.accessibility.routing.AccessibilityEventPolicy
import com.shortsblockerkids.platform.accessibility.routing.AccessibilityEventRouter
import com.shortsblockerkids.platform.accessibility.scanning.AccessibilityTreeScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AccessibilityServiceRuntime(
    private val service: AccessibilityService,
    private val settingsStatePort: SettingsStatePort,
    private val temporaryAllowStore: TemporaryAllowStore,
    private val timeProvider: TimeProvider,
    private val intervalClock: MonotonicTimeProvider,
) {
    private lateinit var eventRouter: AccessibilityEventRouter
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var latestSettings: AppSettingsSnapshot? = null
    private var settingsJob: Job? = null
    private var pinEntryRecheckJob: Job? = null
    private var scanRecheckJob: Job? = null
    private val temporaryAllowDeadlineScheduler =
        TemporaryAllowDeadlineScheduler(
            scope = serviceScope,
            timeProvider = timeProvider,
            onDeadline = ::routeCurrentWindow,
        )

    fun onServiceConnected() {
        val protectionSettingsPort =
            ProtectionSettingsPort { nowMillis ->
                latestSettings?.toAccessibilityProtectionState(nowMillis)
                    ?: AccessibilityProtectionState(false, emptySet())
            }
        val diagnostics =
            configureAccessibilityDiagnostics(
                debugLogger = DebugAccessibilityLogger(),
                debugSnapshotStore =
                    DetectorDebugSnapshotStore(
                        directory = service.cacheDir.resolve("detector_snapshots"),
                    ),
            )
        val temporaryAllowNavigator =
            TemporaryAllowNavigator {
                val intent =
                    Intent(service, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP,
                        )
                        putExtra(MainActivity.EXTRA_OPEN_TEMPORARY_ALLOW_PIN, true)
                    }
                service.startActivity(intent)
            }
        val blockingDecisionController = BlockingDecisionController(monotonicTimeProvider = intervalClock)
        eventRouter =
            AccessibilityEventRouter(
                protectionSettingsPort = protectionSettingsPort,
                timeProvider = timeProvider,
                eventPolicy = AccessibilityEventPolicy(
                    monotonicTimeProvider = intervalClock,
                    requestRescan = ::scheduleScanRecheck,
                ),
                treeScanner = AccessibilityTreeScanner(),
                detectionEngine = ProductionDetectorRegistry.create(),
                blockingDecisionController = blockingDecisionController,
                blockOverlayController =
                    BlockOverlayController(
                        service = service,
                        temporaryAllowNavigator = temporaryAllowNavigator,
                        onOverlayDismissed = blockingDecisionController::onOverlayDismissed,
                        onPinEntryRequested = {
                            val nowMillis = timeProvider.currentTimeMillis()
                            blockingDecisionController.onPinEntryRequested(nowMillis)
                            schedulePinEntryRecheck()
                        },
                        onShortsCloseCompleted = {
                            if (::eventRouter.isInitialized) {
                                eventRouter.dismissBlockingState()
                            }
                        },
                    ),
                diagnostics = diagnostics,
            )
        settingsJob?.cancel()
        settingsJob = serviceScope.launch {
            ObserveSettingsUseCase(settingsStatePort)().collect { state ->
                when (state) {
                    is SettingsReadState.Ready -> {
                        latestSettings = state.snapshot
                        temporaryAllowDeadlineScheduler.schedule(
                            state.snapshot.protectionConfiguration.temporaryAllowUntilMillis,
                        )
                        if (!state.snapshot.canProtect(timeProvider.currentTimeMillis())) {
                            eventRouter.dismissBlockingState()
                        } else {
                            routeCurrentWindow()
                        }
                    }
                    is SettingsReadState.Unavailable -> {
                        // Only a successfully read snapshot from this service session survives failure.
                        diagnostics.recordBlockingDecision(
                            if (latestSettings == null) "SettingsUnavailableProtectionDegraded"
                            else "SettingsUnavailableUsingLastSnapshot",
                        )
                    }
                    SettingsReadState.Loading -> Unit
                }
            }
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val safeEvent = event ?: return
        if (!::eventRouter.isInitialized || latestSettings == null) {
            return
        }

        eventRouter.route(safeEvent) { service.rootInActiveWindow }
    }

    fun onInterrupt() {
        if (::eventRouter.isInitialized) {
            eventRouter.shutdown()
        }
        pinEntryRecheckJob?.cancel()
        scanRecheckJob?.cancel()
        temporaryAllowDeadlineScheduler.cancel()
    }

    fun onDestroy() {
        if (::eventRouter.isInitialized) {
            eventRouter.shutdown()
        }
        pinEntryRecheckJob?.cancel()
        scanRecheckJob?.cancel()
        temporaryAllowDeadlineScheduler.cancel()
        serviceScope.cancel()
    }

    private fun AppSettingsSnapshot.toAccessibilityProtectionState(nowMillis: Long): AccessibilityProtectionState =
        AccessibilityProtectionState(
            isProtectionActive = canProtect(nowMillis),
            enabledPlatformIds =
                SupportedPlatform.PROTECTED_PLATFORMS
                    .filter { platform ->
                        protectionConfiguration.isPlatformEnabled(platform.id)
                    }.mapTo(mutableSetOf()) { platform -> platform.id },
        )

    private fun scheduleScanRecheck(delayMillis: Long) {
        scanRecheckJob?.cancel()
        scanRecheckJob =
            serviceScope.launch {
                delay(delayMillis)
                routeCurrentWindow()
            }
    }

    private fun schedulePinEntryRecheck() {
        pinEntryRecheckJob?.cancel()
        pinEntryRecheckJob =
            serviceScope.launch {
                delay(BlockingDecisionController.DEFAULT_PIN_ENTRY_LAUNCH_GRACE_MS + PIN_ENTRY_RECHECK_DELAY_MS)
                routeCurrentWindow()
            }
    }

    @Suppress("DEPRECATION")
    private fun routeCurrentWindow() {
        if (!::eventRouter.isInitialized || latestSettings == null) {
            return
        }
        val packageName = service.rootInActiveWindow?.packageName ?: return
        val event =
            AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply {
                this.packageName = packageName
            }
        try {
            eventRouter.route(event) { service.rootInActiveWindow }
        } finally {
            event.recycle()
        }
    }

    private companion object {
        const val PIN_ENTRY_RECHECK_DELAY_MS = 100L
    }
}
