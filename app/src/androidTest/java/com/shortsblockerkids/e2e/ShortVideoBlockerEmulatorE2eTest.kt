package com.shortsblockerkids.e2e

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.LocaleManager
import android.app.UiAutomation
import android.content.ComponentName
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.shortsblockerkids.MainActivity
import com.shortsblockerkids.R
import com.shortsblockerkids.accessibility.ShortsBlockerAccessibilityService
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.port.ProtectionActivationOperation
import com.shortsblockerkids.application.protection.canProtect
import com.shortsblockerkids.application.protection.isTemporarilyAllowed
import com.shortsblockerkids.domain.pin.PinValidationResult
import com.shortsblockerkids.domain.protection.ProtectionMode
import com.shortsblockerkids.infrastructure.security.Pbkdf2PinHasher
import com.shortsblockerkids.infrastructure.storage.DataStoreSettingsStore
import com.shortsblockerkids.platform.accessibility.diagnostics.accessibilityDiagnostics
import com.shortsblockerkids.platform.accessibility.status.AccessibilityServiceStatus
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
    private val settingsStore = DataStoreSettingsStore(targetContext)
    private val createPinUseCase =
        CreatePinUseCase(
            pinStateStore = settingsStore,
            pinHasher = Pbkdf2PinHasher(),
        )
    private var originalApplicationLocales: LocaleList? = null
    private var lastOverlayCacheClearSucceeded: Boolean? = null

    @Before
    fun setUp() {
        uiAutomation.serviceInfo =
            uiAutomation.serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        originalApplicationLocales = currentApplicationLocales()
        resetProtectionEnvironment()
        clearApplicationLocale()
        ensureFixtureAppsInstalled()
        prepareProtectionState()
        enableAccessibilityService()
        pressHome()
        accessibilityDiagnostics.clearDetectorResult()
    }

    @After
    fun tearDown() {
        runBlocking {
            settingsStore.setProtectionEnabled(false)
            settingsStore.setTemporaryAllowUntil(null)
        }
        disableAccessibilityService()
        waitUntil(timeoutMs = 5_000L) { !isOverlayVisible() }
        restoreApplicationLocale()
        pressHome()
        accessibilityDiagnostics.clearDetectorResult()
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
            assertEquals(FixturePlatform.YOUTUBE.packageName, currentPackageName())
        }
    }

    @Test
    fun normalToShortAndBackReleasesPriorOverlayAndReblocks() {
        launchFixture(FixturePlatform.YOUTUBE, "normal_video")
        assertNoOverlay()

        tapText("SHORTS")
        assertOverlayVisible()
        assertLastDetectorPackage(FixturePlatform.YOUTUBE, "confidence=HIGH")

        launchFixture(FixturePlatform.YOUTUBE, "normal_video")
        assertNoOverlay(timeoutMs = 4_000L)

        tapText("SHORTS")
        assertOverlayVisible()

        tapText(resourceText(R.string.blocking_overlay_exit_shorts))
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

        tapText(resourceText(R.string.blocking_overlay_enter_pin))
        waitForText(
            text = resourceText(R.string.pin_entry_title),
            timeoutMs = PIN_ACTIVITY_TIMEOUT_MS,
        )
        enterPin("482")
        tapText(resourceText(R.string.pin_entry_submit))
        waitForText(resourceText(R.string.pin_entry_error_incomplete))
        assertFalse(hasText(resourceText(R.string.temporary_allow_title)))

        enterPin("0000")
        tapText(resourceText(R.string.pin_entry_submit))
        waitForText(quantityText(R.plurals.pin_entry_attempts_remaining, 4))
        assertFalse(hasText(resourceText(R.string.temporary_allow_title)))

        enterPin(PARENT_PIN)
        tapText(resourceText(R.string.pin_entry_submit))
        waitForText(resourceText(R.string.temporary_allow_title))
        tapText(quantityText(R.plurals.temporary_allow_duration_minutes, 15))
        assertNoOverlay()

        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayRemainsHidden()

        runBlocking {
            settingsStore.setTemporaryAllowUntil(System.currentTimeMillis() - 1L)
        }
        waitUntil(timeoutMs = 2_000L) {
            runBlocking {
                !settingsStore
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
    @SdkSuppress(minSdkVersion = 33)
    fun englishLocaleOverlayAndPinSmoke() {
        assertLocaleOverlayAndPinSmoke(
            languageTags = "en-US",
            expectedOverlayTitle = "Short video blocked",
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun germanLocaleOverlayAndPinSmoke() {
        assertLocaleOverlayAndPinSmoke(
            languageTags = "de-DE",
            expectedOverlayTitle = "Kurzvideo blockiert",
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun ukrainianLocaleOverlayAndPinSmoke() {
        assertLocaleOverlayAndPinSmoke(
            languageTags = "uk-UA",
            expectedOverlayTitle = "Коротке відео заблоковано",
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun unsupportedLocaleOverlayAndPinSmokeUsesEnglishFallback() {
        assertLocaleOverlayAndPinSmoke(
            languageTags = "fr-FR,en-US",
            expectedOverlayTitle = "Short video blocked",
        )
        assertEquals(
            englishResourceText(R.string.blocking_overlay_title),
            resourceText(R.string.blocking_overlay_title),
        )
    }

    @Test
    fun exitBackAndHomeInteractionsRecoverWithoutDuplicateOverlay() {
        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()

        tapText(resourceText(R.string.blocking_overlay_exit_shorts))
        assertNoOverlay(timeoutMs = 4_000L)

        launchFixture(FixturePlatform.YOUTUBE, "home")
        assertNoOverlay()
        pressBack()
        pressHome()
        assertNoOverlay()

        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()
        assertEquals(1, overlayTitleCount())

        pressHome()
        assertNoOverlay(timeoutMs = 4_000L)
        launchFixture(FixturePlatform.YOUTUBE, "normal_video")
        assertNoOverlay()
        tapText("SHORTS")
        assertOverlayVisible()
        assertEquals(1, overlayTitleCount())
    }

    @Test
    fun cancelingPinFromStableOverlayAllowsSameShortToBlockAgain() {
        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()
        tapText(resourceText(R.string.blocking_overlay_enter_pin))
        assertTrue(
            "PIN activity did not become active",
            waitUntil(timeoutMs = 5_000L) { currentPackageName() == targetContext.packageName },
        )
        waitForText(resourceText(R.string.pin_entry_title), timeoutMs = PIN_ACTIVITY_TIMEOUT_MS)
        pressBack()
        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()
        assertEquals(1, overlayTitleCount())
    }

    @Test
    fun globalProtectionToggleControlsBlocking() {
        runBlocking {
            settingsStore.setProtectionEnabled(false)
            settingsStore.setTemporaryAllowUntil(null)
        }

        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertNoOverlay()
        assertEquals(FixturePlatform.YOUTUBE.packageName, currentPackageName())

        accessibilityDiagnostics.clearDetectorResult()
        runBlocking {
            settingsStore.setProtectionEnabled(true)
            completeProtectionActivationForTest(System.currentTimeMillis())
        }

        var fallbackExecuted = false
        val protectionRestored =
            waitUntil(timeoutMs = 8_000L) {
                if (accessibilityDiagnostics.lastBlockingDecisionText() == "FallbackExecuted") {
                    fallbackExecuted = true
                }
                val currentPackage = currentPackageName()
                isOverlayVisible() ||
                    (fallbackExecuted && currentPackage != null && currentPackage != FixturePlatform.YOUTUBE.packageName)
            }
        assertTrue(
            "Protection did not resume after global toggle; " +
                "decision=${accessibilityDiagnostics.lastBlockingDecisionText()} package=${currentPackageName()}",
            protectionRestored,
        )
        if (isOverlayVisible()) assertEquals(1, overlayTitleCount())
    }

    @Test
    fun unsupportedPackageWithShortVideoLookalikeDoesNotBlock() {
        launchFixture(FixturePlatform.UNSUPPORTED, "reels")

        assertEquals(FixturePlatform.UNSUPPORTED.packageName, currentPackageName())
        assertNoOverlay()
        assertTrue(accessibilityDiagnostics.lastDetectorResultText() in listOf(null, "none"))
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

    private fun assertLocaleOverlayAndPinSmoke(
        languageTags: String,
        expectedOverlayTitle: String,
    ) {
        setApplicationLocale(languageTags)
        prewarmMainActivity()
        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayVisible()
        assertEquals(
            "$languageTags did not update the app resource context",
            expectedOverlayTitle,
            resourceText(R.string.blocking_overlay_title),
        )
        assertTrue(
            "$languageTags overlay did not use the expected localized title",
            hasText(expectedOverlayTitle),
        )
        assertEquals(FixturePlatform.YOUTUBE.packageName, currentPackageName())

        tapText(resourceText(R.string.blocking_overlay_enter_pin))
        waitForText(
            text = resourceText(R.string.pin_entry_title),
            timeoutMs = PIN_ACTIVITY_TIMEOUT_MS,
        )
        enterPin("0000")
        tapText(resourceText(R.string.pin_entry_submit))
        waitForText(quantityText(R.plurals.pin_entry_attempts_remaining, 4))

        enterPin(PARENT_PIN)
        tapText(resourceText(R.string.pin_entry_submit))
        waitForText(resourceText(R.string.temporary_allow_title))
        tapText(quantityText(R.plurals.temporary_allow_duration_minutes, 15))
        assertNoOverlay()

        launchFixture(FixturePlatform.YOUTUBE, "shorts")
        assertOverlayRemainsHidden()
    }

    private fun prewarmMainActivity() {
        val component = ComponentName(targetContext, MainActivity::class.java).flattenToString()
        shell("am start -W -n $component")
        waitForText(
            text = resourceText(R.string.pin_entry_title),
            timeoutMs = PIN_ACTIVITY_TIMEOUT_MS,
        )
        pressHome()
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
            settingsStore.setProtectionEnabled(false)
        }
        assertNoOverlay(timeoutMs = 3_000L)
        pressHome()
        runBlocking {
            settingsStore.setProtectionEnabled(true)
            completeProtectionActivationForTest(System.currentTimeMillis())
        }
        SystemClock.sleep(700L)
    }

    private fun prepareProtectionState() {
        runBlocking {
            assertEquals(
                PinValidationResult.Valid,
                createPinUseCase(PARENT_PIN, PARENT_PIN),
            )
            settingsStore.acceptAccessibilityDisclosure()
            settingsStore.setSelectedMode(ProtectionMode.BLOCK_SHORTS)
            settingsStore.setPlatformEnabled("youtube_shorts", true)
            settingsStore.setTemporaryAllowUntil(null)
            settingsStore.setProtectionEnabled(true)
            completeProtectionActivationForTest(System.currentTimeMillis())
        }
        assertTrue(
            "Protection settings did not become active",
            waitUntil(timeoutMs = 3_000L) {
                runBlocking {
                    settingsStore.readSettings().first().canProtect(System.currentTimeMillis())
                }
            },
        )
    }

    private fun resetProtectionEnvironment() {
        runBlocking {
            settingsStore.setProtectionEnabled(false)
            settingsStore.setTemporaryAllowUntil(null)
        }
        disableAccessibilityService()
        pressHome()
        waitUntil(timeoutMs = 5_000L) { !isOverlayVisible() }
        accessibilityDiagnostics.clearDetectorResult()
    }

    private fun setApplicationLocale(languageTags: String) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        disableAccessibilityService()
        applyApplicationLocales(
            locales = LocaleList.forLanguageTags(languageTags),
            action = "applied",
        )
        enableAccessibilityService()
    }

    private fun clearApplicationLocale() {
        applyApplicationLocales(
            locales = LocaleList.getEmptyLocaleList(),
            action = "cleared",
        )
    }

    private fun restoreApplicationLocale() {
        val locales = originalApplicationLocales ?: return
        applyApplicationLocales(
            locales = locales,
            action = "restored",
        )
    }

    private fun currentApplicationLocales(): LocaleList? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            targetContext
                .getSystemService(LocaleManager::class.java)
                .applicationLocales
        } else {
            null
        }

    private fun applyApplicationLocales(
        locales: LocaleList,
        action: String,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val localeManager = targetContext.getSystemService(LocaleManager::class.java)
        val expectedLanguageTags = locales.toLanguageTags()
        localeManager.applicationLocales = locales
        assertTrue(
            "App locale was not $action: $expectedLanguageTags",
            waitUntil(timeoutMs = 5_000L) {
                localeManager.applicationLocales.toLanguageTags() == expectedLanguageTags
            },
        )
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
        assertTrue(
            "Accessibility service did not fully unbind",
            waitUntil(timeoutMs = 8_000L) {
                !shell("dumpsys accessibility")
                    .contains(ShortsBlockerAccessibilityService::class.java.name)
            },
        )
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
        val clicked =
            waitUntil(timeoutMs = 8_000L) {
                val node =
                    findNodes {
                        it.hasExactTextOrDescription(text) && it.isVisibleToUser && it.isEnabled
                    }.firstOrNull()
                node != null && node.refresh() && clickNode(node)
            }
        if (!clicked) {
            throw AssertionError("Unable to click UI text: $text; targets=${clickTargetSnapshot(text)}")
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

    private fun waitForText(
        text: String,
        timeoutMs: Long = 8_000L,
    ) {
        val found = waitUntil(timeoutMs = timeoutMs) { hasTextContaining(text) }
        assertTrue(
            "Expected text not found: $text; visible=${visibleTextSnapshot()}",
            found,
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
        val failureDetails =
            if (disappeared) {
                ""
            } else {
                val overlayWindows = accessibilityOverlayWindowCount()
                val titlePresent = hasText(resourceText(R.string.blocking_overlay_title))
                val dumpsysOverlay = shell("dumpsys accessibility").contains("TYPE_ACCESSIBILITY_OVERLAY")
                " windows=$overlayWindows cacheCleared=$lastOverlayCacheClearSucceeded " +
                    "titlePresent=$titlePresent dumpsysOverlay=$dumpsysOverlay " +
                    "package=${currentPackageName()} decision=${accessibilityDiagnostics.lastBlockingDecisionText()}"
            }
        assertTrue("Blocking overlay was still visible$failureDetails", disappeared)
    }

    private fun assertOverlayRemainsHidden(timeoutMs: Long = 2_500L) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            assertFalse("Blocking overlay appeared during the quiet period", isOverlayVisible())
            SystemClock.sleep(100L)
        }
        assertFalse("Blocking overlay appeared during the quiet period", isOverlayVisible())
    }

    private fun assertLastDetectorPackage(
        platform: FixturePlatform,
        expectedFragment: String,
    ) {
        assertTrue(
            "AccessibilityService did not record ${platform.packageName}",
            waitUntil(timeoutMs = 3_000L) {
                accessibilityDiagnostics
                    .lastDetectorResultText()
                    ?.contains("package=${platform.packageName}") == true
            },
        )
        assertTrue(
            accessibilityDiagnostics.lastDetectorResultText().orEmpty(),
            accessibilityDiagnostics
                .lastDetectorResultText()
                .orEmpty()
                .contains(expectedFragment),
        )
    }

    private fun isOverlayVisible(): Boolean =
        accessibilityOverlayWindowCount() > 0 ||
            hasText(resourceText(R.string.blocking_overlay_title)) ||
            shell("dumpsys accessibility").contains("TYPE_ACCESSIBILITY_OVERLAY")

    private fun overlayTitleCount(): Int =
        accessibilityOverlayWindowCount().takeIf { it > 0 }
            ?: findNodes {
                it.hasExactTextOrDescription(resourceText(R.string.blocking_overlay_title))
            }.size

    private fun accessibilityOverlayWindowCount(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            lastOverlayCacheClearSucceeded = uiAutomation.clearCache()
        }
        return uiAutomation.windows.count { window ->
            window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
        }
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

    private fun hasText(text: String): Boolean = findNodes { it.hasExactTextOrDescription(text) }.isNotEmpty()

    private fun hasTextContaining(text: String): Boolean = findNodes { it.hasTextOrDescriptionContaining(text) }.isNotEmpty()

    private fun visibleTextSnapshot(): List<String> =
        findNodes { !it.text.isNullOrBlank() || !it.contentDescription.isNullOrBlank() }
            .mapNotNull { it.text?.toString() ?: it.contentDescription?.toString() }
            .distinct()

    private fun resourceText(resourceId: Int): String = targetContext.getString(resourceId)

    private fun quantityText(
        resourceId: Int,
        quantity: Int,
    ): String = targetContext.resources.getQuantityString(resourceId, quantity, quantity)

    private fun englishResourceText(resourceId: Int): String {
        val configuration =
            Configuration(targetContext.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags("en-US"))
            }
        return targetContext.createConfigurationContext(configuration).getString(resourceId)
    }

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

    private fun clickTargetSnapshot(text: String): String =
        findNodes { it.hasExactTextOrDescription(text) }.joinToString { target ->
            val bounds = Rect()
            target.getBoundsInScreen(bounds)
            "class=${target.className}, window=${target.window?.type}, bounds=$bounds, " +
                "visible=${target.isVisibleToUser}, enabled=${target.isEnabled}, clickable=${target.isClickable}"
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

    private suspend fun completeProtectionActivationForTest(nowMillis: Long) {
        settingsStore.completeProtectionActivation {
            ProtectionActivationOperation.Record(nowMillis)
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
        const val PARENT_PIN = "4826"
        const val FAKE_ACTIVITY = "com.shortsblockerkids.fixtureapps.FakeSocialActivity"
        const val PIN_ACTIVITY_TIMEOUT_MS = 30_000L
    }
}
