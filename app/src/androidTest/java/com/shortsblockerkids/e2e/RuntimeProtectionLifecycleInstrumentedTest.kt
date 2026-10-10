package com.shortsblockerkids.e2e

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.ComponentName
import android.os.SystemClock
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shortsblockerkids.accessibility.ShortsBlockerAccessibilityService
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.application.port.ProtectionActivationOperation
import com.shortsblockerkids.application.protection.isTemporarilyAllowed
import com.shortsblockerkids.domain.protection.ProtectionMode
import com.shortsblockerkids.infrastructure.security.Pbkdf2PinHasher
import com.shortsblockerkids.infrastructure.storage.DataStoreSettingsStore
import com.shortsblockerkids.infrastructure.time.AndroidPinLockoutClock
import com.shortsblockerkids.platform.accessibility.diagnostics.accessibilityDiagnostics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class RuntimeProtectionLifecycleInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    private val store = DataStoreSettingsStore(context)
    private val component = ComponentName(context, ShortsBlockerAccessibilityService::class.java).flattenToString()

    @Before
    fun prepare() {
        automation.serviceInfo =
            automation.serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        disableService()
        shell("input keyevent KEYCODE_HOME")
        assertTrue("YouTube fixture must be installed", shell("pm path $FIXTURE_PACKAGE").contains("package:"))
        runBlocking {
            store.setProtectionEnabled(false)
            store.setTemporaryAllowUntil(null)
            CreatePinUseCase(store, Pbkdf2PinHasher())("4826", "4826")
            store.acceptAccessibilityDisclosure()
            store.setSelectedMode(ProtectionMode.BLOCK_SHORTS)
            store.setProtectionEnabled(true)
            store.completeProtectionActivation { ProtectionActivationOperation.Record(System.currentTimeMillis()) }
        }
        accessibilityDiagnostics.clearDetectorResult()
    }

    @After
    fun cleanup() {
        runBlocking {
            store.setProtectionEnabled(false)
            store.setTemporaryAllowUntil(null)
        }
        disableService()
        shell("am force-stop $FIXTURE_PACKAGE")
        shell("input keyevent KEYCODE_HOME")
    }

    @Test
    fun interruptRetainsBarrierWhileAutomaticExitIsUnconfirmed() {
        enableService()
        launchShorts()
        await("Automatic exit did not start") {
            accessibilityDiagnostics.lastBlockingDecisionText() == "AutomaticPlaybackExitPending" && overlayVisible()
        }
        interruptService()
        SystemClock.sleep(100L)
        assertTrue("Interrupt removed barrier before two closure observations", overlayVisible())
        await("Unconfirmed exit callback did not survive interruption") {
            accessibilityDiagnostics.lastBlockingDecisionText() == "OverlayRetainedExitUnconfirmed"
        }
        assertTrue("Unconfirmed playback exit removed barrier", overlayVisible())
    }

    @Test
    fun elapsedDeadlineExpiresAfterInterruptDespiteFutureWallDeadline() {
        assertElapsedExpiry(wallOffsetMillis = 86_400_000L, reconnect = false)
    }

    @Test
    fun elapsedDeadlineSurvivesReconnectDespitePastWallDeadline() {
        assertElapsedExpiry(wallOffsetMillis = -86_400_000L, reconnect = true)
    }

    private fun assertElapsedExpiry(
        wallOffsetMillis: Long,
        reconnect: Boolean,
    ) {
        val clock = AndroidPinLockoutClock(context).read()
        val deadline = clock.elapsedRealtimeMillis + 15_000L
        runBlocking {
            store.setTemporaryAllowDeadline(System.currentTimeMillis() + wallOffsetMillis, deadline, clock.bootCount)
            val snapshot = store.readSettings().first()
            assertEquals(deadline, snapshot.protectionConfiguration.temporaryAllowElapsedRealtimeDeadlineMillis)
            assertTrue(snapshot.isTemporarilyAllowed(System.currentTimeMillis(), SystemClock.elapsedRealtime()))
        }
        enableService()
        launchShorts()
        SystemClock.sleep(700L)
        assertFalse("Wall deadline overrode elapsed allowance", overlayVisible())
        interruptService()
        if (reconnect) {
            disableService()
            enableService()
            // Conservative Loading may exit the old window before the persisted allowance is read.
            launchShorts()
            SystemClock.sleep(700L)
        }
        await("Quiet expiry requires an active fixture window") {
            automation.rootInActiveWindow?.packageName?.toString() == FIXTURE_PACKAGE && !overlayVisible()
        }
        assertTrue("Test setup consumed allowance", SystemClock.elapsedRealtime() < deadline - 1_000L)
        // No settings write or fixture event is generated during this quiet expiry period.
        while (SystemClock.elapsedRealtime() < deadline - 100L) {
            assertFalse("Allowance ended before elapsed deadline", overlayVisible())
            SystemClock.sleep(50L)
        }
        await("Expiry failed to recheck the quiet active window", timeoutMillis = 4_000L) {
            overlayVisible() || accessibilityDiagnostics.lastBlockingDecisionText() == "AutomaticPlaybackExitPending"
        }
        assertTrue("Protection resumed before elapsed expiry", SystemClock.elapsedRealtime() >= deadline)
    }

    private fun interruptService() {
        instrumentation.runOnMainSync { context.getSystemService(AccessibilityManager::class.java).interrupt() }
    }

    private fun enableService() {
        shell("settings put secure enabled_accessibility_services $component")
        shell("settings put secure accessibility_enabled 1")
        await("Production accessibility service did not bind") {
            isServiceBound()
        }
    }

    private fun disableService() {
        shell("settings delete secure enabled_accessibility_services")
        shell("settings put secure accessibility_enabled 0")
        await("Production service did not unbind") {
            !isServiceBound() && !overlayVisible()
        }
    }

    private fun isServiceBound(): Boolean =
        context
            .getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { serviceInfo ->
                val info = serviceInfo.resolveInfo.serviceInfo
                ComponentName(info.packageName, info.name).flattenToString() == component
            }

    private fun launchShorts() {
        shell("am force-stop $FIXTURE_PACKAGE")
        shell("am start -W -n $FIXTURE_PACKAGE/com.shortsblockerkids.fixtureapps.FakeSocialActivity --es screen shorts")
    }

    private fun overlayVisible(): Boolean = automation.windows.any { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }

    private fun await(
        message: String,
        timeoutMillis: Long = 8_000L,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(20L)
        }
        assertTrue(message, condition())
    }

    private fun shell(command: String): String =
        automation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        }

    private companion object {
        const val FIXTURE_PACKAGE = "com.shortsblockerkids.fixture.youtube"
    }
}
