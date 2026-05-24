package com.shortsblockerkids.e2e

import android.app.UiAutomation
import android.content.ComponentName
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shortsblockerkids.accessibility.AccessibilityServiceStatus
import com.shortsblockerkids.accessibility.RuntimeProtectionState
import com.shortsblockerkids.accessibility.ShortsBlockerAccessibilityService
import com.shortsblockerkids.core.model.ProtectionMode
import com.shortsblockerkids.core.storage.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

private typealias NodePredicate = (AccessibilityNodeInfo) -> Boolean

@RunWith(AndroidJUnit4::class)
class ShortVideoBlockerEmulatorE2eTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private val targetContext = instrumentation.targetContext
    private val settingsRepository = SettingsRepository(targetContext)

    @Before
    fun setUp() {
        resetProtectionEnvironment()
        ensureFixtureAppsInstalled()
        prepareProtectionState()
        enableAccessibilityService()
        pressHome()
        RuntimeProtectionState.clearDetectorResult()
    }

    @After
    fun tearDown() {
        runBlocking {
            settingsRepository.setProtectionEnabled(false)
            settingsRepository.setTemporaryAllowUntil(null)
        }
        disableAccessibilityService()
        waitUntil(timeoutMs = 5_000L) { !isOverlayVisible() }
        pressHome()
        RuntimeProtectionState.clearDetectorResult()
    }

    @Test
    fun youtubeFixtureBlocksOnlyShortsScreen() {
        assertNormalScreensDoNotBlock(
            platform = FixturePlatform.YOUTUBE,
            screens = listOf("home", "search", "normal_video"),
        )

        launchFixture(FixturePlatform.YOUTUBE, "shorts")

        assertOverlayVisible()
        assertLastDetectorPackage(FixturePlatform.YOUTUBE, "confidence=HIGH")
    }

    @Test
    fun tiktokFixtureBlocksOnlyForYouScreen() {
        assertNormalScreensDoNotBlock(
            platform = FixturePlatform.TIKTOK,
            screens = listOf("profile", "search", "settings"),
        )

        launchFixture(FixturePlatform.TIKTOK, "for_you")

        assertOverlayVisible()
        assertLastDetectorPackage(FixturePlatform.TIKTOK, "confidence=HIGH")
    }

    @Test
    fun instagramFixtureBlocksOnlyReelsScreen() {
        assertNormalScreensDoNotBlock(
            platform = FixturePlatform.INSTAGRAM,
            screens = listOf("feed", "profile", "story"),
        )

        launchFixture(FixturePlatform.INSTAGRAM, "reels")

        assertOverlayVisible()
        assertLastDetectorPackage(FixturePlatform.INSTAGRAM, "confidence=HIGH")
    }

    @Test
    fun facebookFixtureBlocksOnlyReelsScreen() {
        assertNormalScreensDoNotBlock(
            platform = FixturePlatform.FACEBOOK,
            screens = listOf("feed", "profile", "groups"),
        )

        launchFixture(FixturePlatform.FACEBOOK, "reels")

        assertOverlayVisible()
        assertLastDetectorPackage(FixturePlatform.FACEBOOK, "confidence=HIGH")
    }

    @Test
    fun repeatedShortVideoEventsKeepSingleStableOverlay() {
        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()

        repeat(6) {
            launchFixture(FixturePlatform.YOUTUBE, "shorts")
            assertOverlayVisible()
            assertEquals(1, overlayTitleCount())
        }
    }

    @Test
    fun normalToShortKeepsOverlayStableUntilExplicitExit() {
        launchFixture(FixturePlatform.YOUTUBE, "normal_video")
        assertNoOverlay()

        tapText("SHORTS")
        assertOverlayVisible()
        assertLastDetectorPackage(FixturePlatform.YOUTUBE, "confidence=HIGH")

        launchFixture(FixturePlatform.YOUTUBE, "normal_video")
        assertOverlayVisible()
        assertLastDetectorPackage(FixturePlatform.YOUTUBE, "confidence=HIGH")

        tapText(OVERLAY_EXIT)
        assertNoOverlay(timeoutMs = 4_000L)

        launchFixture(FixturePlatform.YOUTUBE, "normal_video")
        assertNoOverlay()

        tapText("SHORTS")
        assertOverlayVisible()
    }

    @Test
    fun pinFlowRejectsWrongPinAllowsCorrectPinAndTemporaryAllowExpires() {
        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()

        tapText(OVERLAY_ENTER_PIN)
        enterPin("0000")
        tapText("Unlock")
        waitForText("Wrong PIN")
        assertFalse(hasText("Allow short videos"))

        enterPin(PARENT_PIN)
        tapText("Unlock")
        waitForText("Allow short videos")
        tapText("5 minutes")
        assertNoOverlay()

        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertNoOverlay()

        runBlocking {
            settingsRepository.setTemporaryAllowUntil(System.currentTimeMillis() - 1L)
        }
        waitUntil(timeoutMs = 2_000L) {
            runBlocking {
                !settingsRepository
                    .readSettings()
                    .first()
                    .isTemporarilyAllowed(System.currentTimeMillis())
            }
        }
        SystemClock.sleep(800L)
        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()
    }

    @Test
    fun exitBackAndHomeInteractionsRecoverWithoutDuplicateOverlay() {
        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()

        tapText(OVERLAY_EXIT)
        assertNoOverlay(timeoutMs = 4_000L)

        launchFixture(FixturePlatform.YOUTUBE, "home")
        assertNoOverlay()
        pressBack()
        pressHome()
        assertNoOverlay()

        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()
        assertEquals(1, overlayTitleCount())
    }

    @Test
    fun globalProtectionToggleControlsBlocking() {
        runBlocking {
            settingsRepository.setProtectionEnabled(false)
            settingsRepository.setTemporaryAllowUntil(null)
        }

        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertNoOverlay()

        runBlocking {
            settingsRepository.setProtectionEnabled(true)
            settingsRepository.recordSuccessfulProtectionActivation()
        }
        launchFixture(FixturePlatform.YOUTUBE, "shorts")

        assertOverlayVisible()
    }

    @Test
    fun unsupportedPackageWithShortVideoLookalikeDoesNotBlock() {
        launchFixture(FixturePlatform.UNSUPPORTED, "reels")

        assertEquals(FixturePlatform.UNSUPPORTED.packageName, currentPackageName())
        assertNoOverlay()
        assertTrue(RuntimeProtectionState.lastDetectorResultText() in listOf(null, "none"))
    }

    @Test
    fun rapidSwitchingBetweenFakeAppsBlocksOnlyConfirmedShortVideoScreen() {
        listOf(
            FixturePlatform.YOUTUBE to "home",
            FixturePlatform.TIKTOK to "profile",
            FixturePlatform.INSTAGRAM to "feed",
            FixturePlatform.FACEBOOK to "groups",
            FixturePlatform.UNSUPPORTED to "reels",
        ).forEach { (platform, screen) ->
            launchFixture(platform, screen)
            assertNoOverlay()
            assertEquals(platform.packageName, currentPackageName())
        }

        launchFixture(FixturePlatform.YOUTUBE, "shorts")

        assertOverlayVisible()
        assertEquals(1, overlayTitleCount())
    }

    @Test
    fun youtubeStillBlocksAfterTikTokInstagramAndFacebookEvents() {
        launchAndExpectOverlay(FixturePlatform.TIKTOK, "for_you")
        dismissOverlayByDisablingProtection()
        launchAndExpectOverlay(FixturePlatform.INSTAGRAM, "reels")
        dismissOverlayByDisablingProtection()
        launchAndExpectOverlay(FixturePlatform.FACEBOOK, "reels")
        dismissOverlayByDisablingProtection()

        launchAndExpectOverlay(FixturePlatform.YOUTUBE, "shorts")
    }

    private fun assertNormalScreensDoNotBlock(
        platform: FixturePlatform,
        screens: List<String>,
    ) {
        screens.forEach { screen ->
            launchFixture(platform, screen)
            assertNoOverlay()
            assertEquals(platform.packageName, currentPackageName())
        }
    }

    private fun launchAndExpectOverlay(
        platform: FixturePlatform,
        screen: String,
    ) {
        launchFixture(platform, screen)
        assertOverlayVisible()
        assertLastDetectorPackage(platform, "confidence=HIGH")
    }

    private fun dismissOverlayByDisablingProtection() {
        runBlocking {
            settingsRepository.setProtectionEnabled(false)
        }
        assertNoOverlay(timeoutMs = 3_000L)
        pressHome()
        runBlocking {
            settingsRepository.setProtectionEnabled(true)
            settingsRepository.recordSuccessfulProtectionActivation()
        }
        SystemClock.sleep(700L)
    }

    private fun prepareProtectionState() {
        runBlocking {
            settingsRepository.savePin(PARENT_PIN)
            settingsRepository.acceptAccessibilityDisclosure()
            settingsRepository.setSelectedMode(ProtectionMode.BLOCK_SHORTS)
            settingsRepository.setTemporaryAllowUntil(null)
            settingsRepository.setProtectionEnabled(true)
            settingsRepository.recordSuccessfulProtectionActivation()
        }
        assertTrue(
            "Protection settings did not become active",
            waitUntil(timeoutMs = 3_000L) {
                runBlocking {
                    settingsRepository.readSettings().first().canProtect(System.currentTimeMillis())
                }
            },
        )
    }

    private fun resetProtectionEnvironment() {
        runBlocking {
            settingsRepository.setProtectionEnabled(false)
            settingsRepository.setTemporaryAllowUntil(null)
        }
        disableAccessibilityService()
        pressHome()
        waitUntil(timeoutMs = 5_000L) { !isOverlayVisible() }
        RuntimeProtectionState.clearDetectorResult()
    }

    private fun enableAccessibilityService() {
        val component =
            ComponentName(
                targetContext.packageName,
                ShortsBlockerAccessibilityService::class.java.name,
            ).flattenToString()
        shell("settings put secure enabled_accessibility_services $component")
        shell("settings put secure accessibility_enabled 1")

        assertTrue(
            "Accessibility service was not enabled for $component",
            waitUntil(timeoutMs = 5_000L) {
                AccessibilityServiceStatus.isEnabled(targetContext)
            },
        )
        assertTrue(
            "Accessibility service was not bound for $component",
            waitUntil(timeoutMs = 8_000L) {
                val accessibilityDump = shell("dumpsys accessibility")
                accessibilityDump.contains("Bound services:{Service") &&
                    accessibilityDump.contains(component)
            },
        )
    }

    private fun disableAccessibilityService() {
        shell("settings delete secure enabled_accessibility_services")
        shell("settings put secure accessibility_enabled 0")
        waitUntil(timeoutMs = 5_000L) {
            !shell("dumpsys accessibility")
                .contains(ShortsBlockerAccessibilityService::class.java.name)
        }
    }

    private fun ensureFixtureAppsInstalled() {
        FixturePlatform.values().forEach { platform ->
            val packagePath = shell("pm path ${platform.packageName}")
            assertTrue(
                "Missing fake fixture app for ${platform.packageName}. " +
                    "Run installFakeSocialApps before connectedDebugAndroidTest.",
                packagePath.contains("package:"),
            )
        }
    }

    private fun launchFixture(
        platform: FixturePlatform,
        screen: String,
    ) {
        val component = "${platform.packageName}/$FAKE_ACTIVITY"
        shell("am force-stop ${platform.packageName}")
        SystemClock.sleep(250L)
        shell("am start -W -n $component --es screen $screen")
        assertTrue(
            "Fixture ${platform.packageName} did not open",
            waitUntil(timeoutMs = 5_000L) {
                currentPackageName() == platform.packageName || isOverlayVisible()
            },
        )
        SystemClock.sleep(700L)
    }

    private fun tapText(text: String) {
        val node = waitForNode(timeoutMs = 5_000L) { it.hasExactTextOrDescription(text) }
        if (node != null) {
            assertTrue("Unable to click UI text: $text", clickNode(node))
            SystemClock.sleep(700L)
            return
        }
        when (text) {
            OVERLAY_ENTER_PIN -> tapOverlayEnterPin()
            OVERLAY_EXIT -> tapOverlayExit()
            else -> assertNotNull("Missing UI text: $text", node)
        }
        SystemClock.sleep(700L)
    }

    private fun enterPin(pin: String) {
        val field =
            waitForNode(timeoutMs = 5_000L) {
                it.className?.toString() == "android.widget.EditText" || it.isEditable
            }
        assertNotNull("Missing PIN input field", field)
        val args =
            Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    pin,
                )
            }
        assertTrue(
            "Unable to set PIN text",
            field!!.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args),
        )
        SystemClock.sleep(300L)
    }

    private fun waitForText(text: String) {
        assertTrue(
            "Expected text not found: $text",
            waitUntil(timeoutMs = 5_000L) { hasTextContaining(text) },
        )
    }

    private fun assertOverlayVisible() {
        assertTrue(
            "Blocking overlay was not visible",
            waitUntil(timeoutMs = 8_000L) { isOverlayVisible() },
        )
        assertEquals(1, overlayTitleCount())
    }

    private fun assertNoOverlay(timeoutMs: Long = 2_500L) {
        val disappeared = waitUntil(timeoutMs = timeoutMs) { !isOverlayVisible() }
        assertTrue("Blocking overlay was still visible", disappeared)
    }

    private fun assertLastDetectorPackage(
        platform: FixturePlatform,
        expectedFragment: String,
    ) {
        assertTrue(
            "AccessibilityService did not record ${platform.packageName}",
            waitUntil(timeoutMs = 3_000L) {
                RuntimeProtectionState
                    .lastDetectorResultText()
                    ?.contains("package=${platform.packageName}") == true
            },
        )
        assertTrue(
            RuntimeProtectionState.lastDetectorResultText().orEmpty(),
            RuntimeProtectionState
                .lastDetectorResultText()
                .orEmpty()
                .contains(expectedFragment),
        )
    }

    private fun isOverlayVisible(): Boolean =
        accessibilityOverlayWindowCount() > 0 ||
            hasText(OVERLAY_TITLE) ||
            shell("dumpsys accessibility").contains("TYPE_ACCESSIBILITY_OVERLAY")

    private fun overlayTitleCount(): Int =
        accessibilityOverlayWindowCount().takeIf { it > 0 }
            ?: findNodes { it.hasExactTextOrDescription(OVERLAY_TITLE) }.size

    private fun accessibilityOverlayWindowCount(): Int =
        uiAutomation.windows.count { window ->
            window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
        }

    private fun currentPackageName(): String? = uiAutomation.rootInActiveWindow?.packageName?.toString()

    private fun pressHome() {
        shell("input keyevent KEYCODE_HOME")
        SystemClock.sleep(700L)
    }

    private fun pressBack() {
        shell("input keyevent KEYCODE_BACK")
        SystemClock.sleep(700L)
    }

    private fun tapOverlayEnterPin() {
        shell("input tap 540 1580")
        SystemClock.sleep(900L)
    }

    private fun tapOverlayExit() {
        shell("input tap 540 1400")
        SystemClock.sleep(1_200L)
    }

    private fun hasText(text: String): Boolean = findNodes { it.hasExactTextOrDescription(text) }.isNotEmpty()

    private fun hasTextContaining(text: String): Boolean = findNodes { it.hasTextOrDescriptionContaining(text) }.isNotEmpty()

    private fun waitForNode(
        timeoutMs: Long,
        predicate: NodePredicate,
    ): AccessibilityNodeInfo? {
        var match: AccessibilityNodeInfo? = null
        waitUntil(timeoutMs = timeoutMs) {
            match = findNodes(predicate).firstOrNull()
            match != null
        }
        return match
    }

    private fun findNodes(predicate: NodePredicate): List<AccessibilityNodeInfo> {
        val roots =
            buildList {
                uiAutomation.rootInActiveWindow?.let(::add)
                uiAutomation.windows.mapNotNullTo(this) { it.root }
            }
        return roots.flatMap { root -> root.findDescendants(predicate) }
    }

    private fun AccessibilityNodeInfo.findDescendants(predicate: NodePredicate): List<AccessibilityNodeInfo> {
        val matches = mutableListOf<AccessibilityNodeInfo>()

        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null) {
                return
            }
            if (predicate(node)) {
                matches += node
            }
            for (index in 0 until node.childCount) {
                visit(node.getChild(index))
            }
        }
        visit(this)
        return matches
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun AccessibilityNodeInfo.hasExactTextOrDescription(expected: String): Boolean =
        text?.toString() == expected || contentDescription?.toString() == expected

    private fun AccessibilityNodeInfo.hasTextOrDescriptionContaining(expected: String): Boolean =
        text?.toString()?.contains(expected) == true ||
            contentDescription?.toString()?.contains(expected) == true

    private fun waitUntil(
        timeoutMs: Long,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) {
                return true
            }
            SystemClock.sleep(100L)
        }
        return condition()
    }

    private fun shell(command: String): String {
        val descriptor = uiAutomation.executeShellCommand(command)
        return try {
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { reader ->
                reader.readText()
            }
        } finally {
            descriptor.close()
        }
    }

    private enum class FixturePlatform(
        val packageName: String,
    ) {
        YOUTUBE("com.shortsblockerkids.fixture.youtube"),
        TIKTOK("com.shortsblockerkids.fixture.tiktok"),
        INSTAGRAM("com.shortsblockerkids.fixture.instagram"),
        FACEBOOK("com.shortsblockerkids.fixture.facebook"),
        UNSUPPORTED("com.example.shortvideo.fixture"),
    }

    private companion object {
        const val PARENT_PIN = "2580"
        const val OVERLAY_TITLE = "Short video blocked"
        const val OVERLAY_ENTER_PIN = "Enter PIN"
        const val OVERLAY_EXIT = "Exit to phone home"
        const val FAKE_ACTIVITY = "com.shortsblockerkids.fixtureapps.FakeSocialActivity"
    }
}
