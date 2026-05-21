package com.shortsblockerkids.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PhoneHomeExitControllerTest {
    @Test
    fun successfulExitCallsPhoneHomeAndDismissesOverlay() {
        val action = RecordingPhoneHomeAction(result = true)
        var completionCount = 0
        val controller =
            PhoneHomeExitController(
                phoneHomeAction = action,
                onExitCompleted = { completionCount += 1 },
            )

        val exited = controller.exitToPhoneHome()

        assertTrue(exited)
        assertEquals(1, action.callCount)
        assertEquals(1, completionCount)
    }

    @Test
    fun failedExitKeepsOverlayVisibleAndAllowsRetry() {
        val action = RecordingPhoneHomeAction(result = false)
        var completionCount = 0
        val controller =
            PhoneHomeExitController(
                phoneHomeAction = action,
                onExitCompleted = { completionCount += 1 },
            )

        val failed = controller.exitToPhoneHome()
        action.result = true
        val retried = controller.exitToPhoneHome()

        assertFalse(failed)
        assertTrue(retried)
        assertEquals(2, action.callCount)
        assertEquals(1, completionCount)
    }

    @Test
    fun exitControllerHasNoYouTubeHomeFallbackCallbacks() {
        val action = RecordingPhoneHomeAction(result = true)
        val controller =
            PhoneHomeExitController(
                phoneHomeAction = action,
                onExitCompleted = {},
            )

        controller.exitToPhoneHome()

        assertEquals(1, action.callCount)
    }

    @Test
    fun blockOverlayExitDoesNotUseYouTubeHomeNavigationFallbacks() {
        val source = blockOverlayControllerSource()

        assertFalse(source.contains("bottom_bar_home"))
        assertFalse(source.contains("AccessibilityNodeInfo.ACTION_CLICK"))
        assertFalse(source.contains("Intent.ACTION_VIEW"))
        assertFalse(source.contains("https://www.youtube.com/"))
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
}
