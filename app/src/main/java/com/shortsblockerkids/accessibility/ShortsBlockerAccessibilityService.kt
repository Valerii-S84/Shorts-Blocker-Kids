package com.shortsblockerkids.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.shortsblockerkids.core.storage.AppSettings
import com.shortsblockerkids.core.storage.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ShortsBlockerAccessibilityService : AccessibilityService() {
    private lateinit var eventRouter: AccessibilityEventRouter
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var latestSettings = AppSettings()

    override fun onServiceConnected() {
        super.onServiceConnected()
        val settingsRepository = SettingsRepository(this)
        val blockingDecisionController = BlockingDecisionController()
        eventRouter =
            AccessibilityEventRouter(
                settingsProvider = { latestSettings },
                eventPolicy = AccessibilityEventPolicy(),
                treeScanner = AccessibilityTreeScanner(),
                detectionEngine = ShortVideoDetectionEngine.youtubeOnly(),
                blockingDecisionController = blockingDecisionController,
                blockOverlayController =
                    BlockOverlayController(
                        service = this,
                        onOverlayDismissed = blockingDecisionController::onOverlayDismissed,
                        onPinEntryRequested = blockingDecisionController::onPinEntryRequested,
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
                val activeSettings =
                    if (settings.hasExpiredTemporaryAllow(nowMillis)) {
                        settingsRepository.clearExpiredTemporaryAllow(nowMillis)
                        settings.copy(temporaryAllowUntil = null)
                    } else {
                        settings
                    }
                latestSettings = activeSettings
                if (!activeSettings.canProtect(nowMillis)) {
                    eventRouter.dismissBlockingState()
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
    }

    override fun onDestroy() {
        if (::eventRouter.isInitialized) {
            eventRouter.shutdown()
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
