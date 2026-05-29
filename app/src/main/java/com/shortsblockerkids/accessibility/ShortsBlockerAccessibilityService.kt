package com.shortsblockerkids.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.core.storage.AppSettings
import com.shortsblockerkids.core.storage.SettingsRepository
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
        latestSettings =
            runBlocking(Dispatchers.IO) {
                activeSettingsFrom(
                    settingsRepository = settingsRepository,
                    settings = settingsRepository.readSettings().first(),
                    nowMillis = System.currentTimeMillis(),
                )
            }
        val blockingDecisionController = BlockingDecisionController()
        eventRouter =
            AccessibilityEventRouter(
                settingsProvider = { latestSettings },
                eventPolicy = AccessibilityEventPolicy(),
                treeScanner = AccessibilityTreeScanner(),
                detectionEngine = ShortVideoDetectionEngine.production(),
                blockingDecisionController = blockingDecisionController,
                blockOverlayController =
                    BlockOverlayController(
                        service = this,
                        onOverlayDismissed = blockingDecisionController::onOverlayDismissed,
                        onPinEntryRequested = {
                            blockingDecisionController.onPinEntryRequested()
                            schedulePinEntryRecheck()
                        },
                        onShortsCloseCompleted = {
                            if (::eventRouter.isInitialized) {
                                eventRouter.dismissBlockingState()
                            }
                        },
                    ),
                debugLogger = DebugAccessibilityLogger(),
                debugSnapshotStore =
                    DetectorDebugSnapshotStore(
                        directory = cacheDir.resolve("detector_snapshots"),
                    ),
            )
        serviceScope.launch {
            settingsRepository.readSettings().collect { settings ->
                val nowMillis = System.currentTimeMillis()
                val wasProtectionActive = latestSettings.canProtect(nowMillis)
                val activeSettings = activeSettingsFrom(settingsRepository, settings, nowMillis)
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

    private suspend fun activeSettingsFrom(
        settingsRepository: SettingsRepository,
        settings: AppSettings,
        nowMillis: Long,
    ): AppSettings =
        if (settings.hasExpiredTemporaryAllow(nowMillis)) {
            settingsRepository.clearExpiredTemporaryAllow(nowMillis)
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
