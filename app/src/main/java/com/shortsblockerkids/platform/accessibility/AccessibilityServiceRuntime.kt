package com.shortsblockerkids.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.MainActivity
import com.shortsblockerkids.accessibility.BlockingDecisionController
import com.shortsblockerkids.application.model.AccessibilityProtectionState
import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.ProtectionSettingsPort
import com.shortsblockerkids.application.protection.ClearExpiredTemporaryAllowUseCase
import com.shortsblockerkids.application.protection.canProtect
import com.shortsblockerkids.core.storage.SettingsRepository
import com.shortsblockerkids.domain.detection.SupportedPlatform
import com.shortsblockerkids.infrastructure.time.SystemTimeProvider
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AccessibilityServiceRuntime(
    private val service: AccessibilityService,
) {
    private lateinit var eventRouter: AccessibilityEventRouter
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var latestSettings = AppSettingsSnapshot()
    private var pinEntryRecheckJob: Job? = null

    fun onServiceConnected() {
        val settingsRepository = SettingsRepository(service)
        val timeProvider = SystemTimeProvider()
        val clearExpiredTemporaryAllowUseCase =
            ClearExpiredTemporaryAllowUseCase(
                timeProvider = timeProvider,
                temporaryAllowStore = settingsRepository,
            )
        latestSettings =
            runBlocking(Dispatchers.IO) {
                activeSettingsFrom(
                    clearExpiredTemporaryAllowUseCase = clearExpiredTemporaryAllowUseCase,
                    settings = settingsRepository.readSettings().first(),
                )
            }
        val protectionSettingsPort =
            ProtectionSettingsPort { nowMillis ->
                latestSettings.toAccessibilityProtectionState(nowMillis)
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
        val blockingDecisionController = BlockingDecisionController()
        eventRouter =
            AccessibilityEventRouter(
                protectionSettingsPort = protectionSettingsPort,
                timeProvider = timeProvider,
                eventPolicy = AccessibilityEventPolicy(),
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
        serviceScope.launch {
            settingsRepository.readSettings().collect { settings ->
                val nowMillis = timeProvider.currentTimeMillis()
                val wasProtectionActive = latestSettings.canProtect(nowMillis)
                val activeSettings =
                    activeSettingsFrom(
                        clearExpiredTemporaryAllowUseCase = clearExpiredTemporaryAllowUseCase,
                        settings = settings,
                    )
                latestSettings = activeSettings
                if (!activeSettings.canProtect(nowMillis)) {
                    eventRouter.dismissBlockingState()
                } else if (!wasProtectionActive) {
                    routeCurrentWindow()
                }
            }
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val safeEvent = event ?: return
        if (!::eventRouter.isInitialized) {
            return
        }

        eventRouter.route(safeEvent) { service.rootInActiveWindow }
    }

    fun onInterrupt() {
        if (::eventRouter.isInitialized) {
            eventRouter.shutdown()
        }
        pinEntryRecheckJob?.cancel()
    }

    fun onDestroy() {
        if (::eventRouter.isInitialized) {
            eventRouter.shutdown()
        }
        pinEntryRecheckJob?.cancel()
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

    private suspend fun activeSettingsFrom(
        clearExpiredTemporaryAllowUseCase: ClearExpiredTemporaryAllowUseCase,
        settings: AppSettingsSnapshot,
    ): AppSettingsSnapshot =
        if (clearExpiredTemporaryAllowUseCase()) {
            settings.copy(
                protectionConfiguration =
                    settings.protectionConfiguration.copy(
                        temporaryAllowUntilMillis = null,
                    ),
            )
        } else {
            settings
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
        if (!::eventRouter.isInitialized) {
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
