package com.shortsblockerkids.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.MainActivity
import com.shortsblockerkids.application.model.AccessibilityProtectionState
import com.shortsblockerkids.application.port.ProtectionSettingsPort
import com.shortsblockerkids.application.protection.ClearExpiredTemporaryAllowUseCase
import com.shortsblockerkids.core.storage.AppSettings
import com.shortsblockerkids.core.storage.SettingsRepository
import com.shortsblockerkids.domain.detection.SupportedPlatform
import com.shortsblockerkids.infrastructure.time.SystemTimeProvider
import com.shortsblockerkids.platform.accessibility.detection.ProductionDetectorRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ShortsBlockerAccessibilityService : AccessibilityService() {
    private lateinit var eventRouter: AccessibilityEventRouter
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var latestSettings = AppSettings()
    private var pinEntryRecheckJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val settingsRepository = SettingsRepository(this)
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
                        directory = cacheDir.resolve("detector_snapshots"),
                    ),
            )
        val temporaryAllowNavigator =
            TemporaryAllowNavigator {
                val intent =
                    Intent(this, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP,
                        )
                        putExtra(MainActivity.EXTRA_OPEN_TEMPORARY_ALLOW_PIN, true)
                    }
                startActivity(intent)
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
                        service = this,
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val safeEvent = event ?: return
        if (!::eventRouter.isInitialized) {
            return
        }

        eventRouter.route(safeEvent) { rootInActiveWindow }
    }

    override fun onInterrupt() {
        if (::eventRouter.isInitialized) {
            eventRouter.shutdown()
        }
        pinEntryRecheckJob?.cancel()
    }

    override fun onDestroy() {
        if (::eventRouter.isInitialized) {
            eventRouter.shutdown()
        }
        pinEntryRecheckJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun AppSettings.toAccessibilityProtectionState(nowMillis: Long): AccessibilityProtectionState =
        AccessibilityProtectionState(
            isProtectionActive = canProtect(nowMillis),
            enabledPlatformIds =
                SupportedPlatform.PROTECTED_PLATFORMS
                    .filter { platform -> isPlatformEnabled(platform.id) }
                    .mapTo(mutableSetOf()) { platform -> platform.id },
        )

    private suspend fun activeSettingsFrom(
        clearExpiredTemporaryAllowUseCase: ClearExpiredTemporaryAllowUseCase,
        settings: AppSettings,
    ): AppSettings =
        if (clearExpiredTemporaryAllowUseCase()) {
            settings.copy(temporaryAllowUntil = null)
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
        val packageName = rootInActiveWindow?.packageName ?: return
        val event =
            AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply {
                this.packageName = packageName
            }
        try {
            eventRouter.route(event) { rootInActiveWindow }
        } finally {
            event.recycle()
        }
    }

    private companion object {
        const val PIN_ENTRY_RECHECK_DELAY_MS = 100L
    }
}
