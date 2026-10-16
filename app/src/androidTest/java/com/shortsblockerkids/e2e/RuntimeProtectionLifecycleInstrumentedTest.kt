package com.shortsblockerkids.e2e

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.ComponentName
import android.os.SystemClock
import android.provider.Settings
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
import kotlinx.coroutines.withTimeout
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
    private val diagnosticPhases = ArrayDeque<String>()
    private var diagnosticDeadline: Long? = null

    @Before
    fun prepare() {
        diagnosticPhases.clear()
        diagnosticDeadline = null
        recordPhase("prepare")
        automation.serviceInfo =
            automation.serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        disableService()
        shell("input keyevent KEYCODE_HOME")
        assertTrueWithState("YouTube fixture must be installed", shell("pm path $FIXTURE_PACKAGE").contains("package:"))
        runBlocking {
            store.setProtectionEnabled(false)
            store.setTemporaryAllowUntil(null)
            CreatePinUseCase(store, Pbkdf2PinHasher())("4826", "4826")
            store.acceptAccessibilityDisclosure()
            store.setSelectedMode(ProtectionMode.BLOCK_SHORTS)
            store.setPlatformEnabled("youtube_shorts", true)
            store.setProtectionEnabled(true)
            store.completeProtectionActivation { ProtectionActivationOperation.Record(System.currentTimeMillis()) }
            assertTrueWithState(
                "YouTube protection must be enabled for the fixture",
                store
                    .readSettings()
                    .first()
                    .protectionConfiguration
                    .isPlatformEnabled("youtube_shorts"),
            )
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
    fun interruptRetainsStableBarrierWithoutLeavingProtectedSurface() {
        enableService()
        launchShorts()
        await("Stable protection barrier did not appear") {
            overlayVisible() && automation.rootInActiveWindow?.packageName?.toString() == FIXTURE_PACKAGE
        }
        assertEqualsWithState("Stable barrier outcome changed", "ShowOverlay", accessibilityDiagnostics.lastBlockingDecisionText())
        interruptService()
        SystemClock.sleep(1_000L)
        assertTrueWithState("Interrupt removed the stable protection barrier", overlayVisible())
        assertEqualsWithState(
            "Interrupt left the protected fixture",
            FIXTURE_PACKAGE,
            automation.rootInActiveWindow?.packageName?.toString(),
        )
        assertEqualsWithState(
            "Interrupt changed the stable barrier outcome",
            "ShowOverlay",
            accessibilityDiagnostics.lastBlockingDecisionText(),
        )
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
        val deadline = clock.elapsedRealtimeMillis + 60_000L
        diagnosticDeadline = deadline
        recordPhase("allowance-before-write")
        runBlocking {
            store.setTemporaryAllowDeadline(System.currentTimeMillis() + wallOffsetMillis, deadline, clock.bootCount)
            val snapshot = store.readSettings().first()
            assertEqualsWithState(
                "Persisted elapsed deadline changed",
                deadline,
                snapshot.protectionConfiguration.temporaryAllowElapsedRealtimeDeadlineMillis,
            )
            assertTrueWithState(
                "Persisted allowance is not active",
                snapshot.isTemporarilyAllowed(System.currentTimeMillis(), SystemClock.elapsedRealtime()),
            )
        }
        recordPhase("allowance-written")
        enableService()
        launchShorts()
        awaitAllowanceReady(deadline)
        assertFalseWithState("Wall deadline overrode elapsed allowance", overlayVisible())
        interruptService()
        if (reconnect) {
            recordPhase("reconnect-before")
            disableService()
            enableService()
            // Conservative Loading may show a barrier before the persisted allowance is read.
            launchShorts()
            recordPhase("reconnect-after")
        }
        recordPhase("quiet-readiness-before")
        awaitAllowanceReady(deadline)
        recordPhase("quiet-ready")
        assertTrueWithState("Test setup consumed allowance", SystemClock.elapsedRealtime() < deadline - 1_000L)
        // No settings write or fixture event is generated during this quiet expiry period.
        while (SystemClock.elapsedRealtime() < deadline - 100L) {
            assertFalseWithState("Allowance ended before elapsed deadline", overlayVisible())
            SystemClock.sleep(50L)
        }
        recordPhase("expiry-observation-before")
        await(
            "Expiry failed to recheck the quiet active window",
            timeoutMillis = 4_000L,
            failureDetails = { failureSnapshot() },
        ) {
            overlayVisible() || accessibilityDiagnostics.lastBlockingDecisionText() == "AutomaticPlaybackExitPending"
        }
        recordPhase("expiry-observed")
        assertTrueWithState("Protection resumed before elapsed expiry", SystemClock.elapsedRealtime() >= deadline)
    }

    private fun awaitAllowanceReady(deadline: Long) {
        await("Quiet expiry requires the service to apply the saved allowance in the active fixture") {
            accessibilityDiagnostics.readyTemporaryAllowDeadlineMillis() == deadline &&
                automation.rootInActiveWindow?.packageName?.toString() == FIXTURE_PACKAGE &&
                !overlayVisible()
        }
    }

    private fun interruptService() {
        recordPhase("interrupt-before")
        instrumentation.runOnMainSync { context.getSystemService(AccessibilityManager::class.java).interrupt() }
        recordPhase("interrupt-after")
    }

    private fun enableService() {
        recordPhase("bind-before-settings")
        accessibilityDiagnostics.recordReadyTemporaryAllowDeadline(null)
        shell("settings put secure enabled_accessibility_services $component")
        shell("settings put secure accessibility_enabled 1")
        recordPhase("bind-after-settings")
        await("Production accessibility service did not bind") {
            isServiceBound()
        }
        recordPhase("bind-complete")
    }

    private fun disableService() {
        recordPhase("unbind-before-settings")
        shell("settings delete secure enabled_accessibility_services")
        shell("settings put secure accessibility_enabled 0")
        recordPhase("unbind-after-settings")
        await("Production service did not unbind") {
            !isServiceBound() && !overlayVisible()
        }
        recordPhase("unbind-complete")
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
        recordPhase("fixture-launch-before")
        shell("am force-stop $FIXTURE_PACKAGE")
        shell("am start -W -n $FIXTURE_PACKAGE/com.shortsblockerkids.fixtureapps.FakeSocialActivity --es screen shorts")
        recordPhase("fixture-launch-after")
    }

    private fun overlayVisible(): Boolean = automation.windows.any { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }

    private fun await(
        message: String,
        timeoutMillis: Long = 8_000L,
        failureDetails: () -> String = { failureSnapshot() },
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(20L)
        }
        val succeeded = condition()
        assertTrue(if (succeeded) message else "$message ${failureDetails()}", succeeded)
    }

    @Suppress("DEPRECATION")
    private fun runtimeState(deadline: Long?): String {
        val clock = runCatching { AndroidPinLockoutClock(context).read() }.getOrNull()
        val protection =
            runCatching {
                runBlocking {
                    withTimeout(1_000L) { store.readSettings().first().protectionConfiguration }
                }
            }.getOrNull()
        val root = runCatching { automation.rootInActiveWindow }.getOrNull()
        val activePackage =
            try {
                root?.packageName?.toString()
            } finally {
                root?.recycle()
            }
        val configured =
            runCatching {
                Settings.Secure
                    .getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                    ?.split(':')
                    ?.contains(component) == true
            }.getOrNull()
        val enabled =
            runCatching {
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
            }.getOrNull()
        val bound = runCatching { isServiceBound() }.getOrNull()
        val overlay = runCatching { overlayVisible() }.getOrNull()
        return "elapsed=${SystemClock.elapsedRealtime()} deadline=$deadline boot=${clock?.bootCount} " +
            "storedElapsed=${protection?.temporaryAllowElapsedRealtimeDeadlineMillis} " +
            "storedBoot=${protection?.temporaryAllowBootCount} " +
            "storedWall=${protection?.temporaryAllowUntilMillis} wall=${System.currentTimeMillis()} " +
            "configured=$configured enabled=$enabled bound=$bound activePackage=$activePackage " +
            "decision=${accessibilityDiagnostics.lastBlockingDecisionText()} overlay=$overlay"
    }

    private fun recordPhase(phase: String) {
        if (diagnosticPhases.size == MAX_DIAGNOSTIC_PHASES) diagnosticPhases.removeFirst()
        diagnosticPhases.addLast(
            "$phase elapsed=${SystemClock.elapsedRealtime()} deadline=$diagnosticDeadline " +
                "decision=${accessibilityDiagnostics.lastBlockingDecisionText()}",
        )
    }

    private fun failureSnapshot(): String = "${runtimeState(diagnosticDeadline)} phases=[${diagnosticPhases.joinToString(" | ")}]"

    private fun failureMessage(
        message: String,
        passed: Boolean,
    ): String = if (passed) message else "$message ${failureSnapshot()}"

    private fun assertTrueWithState(
        message: String,
        actual: Boolean,
    ) {
        assertTrue(failureMessage(message, actual), actual)
    }

    private fun assertFalseWithState(
        message: String,
        actual: Boolean,
    ) {
        assertFalse(failureMessage(message, !actual), actual)
    }

    private fun <T> assertEqualsWithState(
        message: String,
        expected: T,
        actual: T,
    ) {
        assertEquals(failureMessage(message, expected == actual), expected, actual)
    }

    private fun shell(command: String): String =
        automation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        }

    private companion object {
        const val MAX_DIAGNOSTIC_PHASES = 24
        const val FIXTURE_PACKAGE = "com.shortsblockerkids.fixture.youtube"
    }
}
