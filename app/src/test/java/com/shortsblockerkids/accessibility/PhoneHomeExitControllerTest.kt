package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PhoneHomeExitControllerTest {
    @Test
    fun successfulExitCallsPhoneHomeAndSchedulesOverlayDismiss() {
        val action = RecordingPhoneHomeAction(result = true)
        val scheduler = RecordingDelayedActionScheduler()
        var completionCount = 0
        val controller =
            PhoneHomeExitController(
                phoneHomeAction = action,
                delayedActionScheduler = scheduler,
                onExitCompleted = { completionCount += 1 },
            )

        val exited = controller.exitToPhoneHome()

        assertTrue(exited)
        assertEquals(1, action.callCount)
        assertEquals(PhoneHomeExitController.EXIT_DISMISS_DELAY_MS, scheduler.delayMillis)
        assertEquals(0, completionCount)

        scheduler.runScheduledAction()

        assertEquals(1, completionCount)
    }

    @Test
    fun failedExitKeepsOverlayVisibleAndAllowsRetry() {
        val action = RecordingPhoneHomeAction(result = false)
        val scheduler = RecordingDelayedActionScheduler()
        var completionCount = 0
        val controller =
            PhoneHomeExitController(
                phoneHomeAction = action,
                delayedActionScheduler = scheduler,
                onExitCompleted = { completionCount += 1 },
            )

        val failed = controller.exitToPhoneHome()
        assertFalse(failed)
        assertEquals(1, action.callCount)
        assertEquals(null, scheduler.delayMillis)
        assertEquals(0, completionCount)

        action.result = true
        val retried = controller.exitToPhoneHome()
        scheduler.runScheduledAction()

        assertTrue(retried)
        assertEquals(2, action.callCount)
        assertEquals(PhoneHomeExitController.EXIT_DISMISS_DELAY_MS, scheduler.delayMillis)
        assertEquals(1, completionCount)
    }

    @Test
    fun successfulExitDismissesOverlayWithoutSecondTap() {
        val action = RecordingPhoneHomeAction(result = true)
        val scheduler = RecordingDelayedActionScheduler()
        var completionCount = 0
        val controller =
            PhoneHomeExitController(
                phoneHomeAction = action,
                delayedActionScheduler = scheduler,
                onExitCompleted = { completionCount += 1 },
            )

        controller.exitToPhoneHome()
        scheduler.runScheduledAction()

        assertEquals(1, action.callCount)
        assertEquals(1, completionCount)
    }

    @Test
    fun blockOverlayExitUsesHomeActionWithoutYouTubeNavigationFallbacks() {
        val source = blockOverlayControllerSource()

        assertTrue(source.contains("GLOBAL_ACTION_HOME"))
        assertFalse(source.contains("bottom_bar_home"))
        assertFalse(source.contains("AccessibilityNodeInfo.ACTION_CLICK"))
        assertFalse(source.contains("Intent.ACTION_VIEW"))
        assertFalse(source.contains("https://www.youtube.com/"))
        assertFalse(source.contains("GLOBAL_ACTION_BACK"))
    }

    private fun blockOverlayControllerSource(): String {
        val path =
            listOf(
                "src/main/java/com/shortsblockerkids/accessibility/BlockOverlayController.kt",
                "app/src/main/java/com/shortsblockerkids/accessibility/BlockOverlayController.kt",
            ).map(::File)
                .first(File::isFile)
        return path.readText()
    }

    private class RecordingPhoneHomeAction(
        var result: Boolean,
    ) : PhoneHomeAction {
        var callCount = 0
            private set

        override fun exitToPhoneHome(): Boolean {
            callCount += 1
            return result
        }
    }

    private class RecordingDelayedActionScheduler : DelayedActionScheduler {
        var delayMillis: Long? = null
            private set
        private var scheduledAction: (() -> Unit)? = null

        override fun schedule(
            delayMillis: Long,
            action: () -> Unit,
        ) {
            this.delayMillis = delayMillis
            scheduledAction = action
        }

        fun runScheduledAction() {
            scheduledAction?.invoke()
        }
    }
}
