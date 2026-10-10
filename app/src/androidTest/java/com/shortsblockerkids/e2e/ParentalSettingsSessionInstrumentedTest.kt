package com.shortsblockerkids.e2e

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.shortsblockerkids.MainActivity
import com.shortsblockerkids.R
import com.shortsblockerkids.application.pin.CreatePinUseCase
import com.shortsblockerkids.infrastructure.security.Pbkdf2PinHasher
import com.shortsblockerkids.infrastructure.storage.DataStoreSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParentalSettingsSessionInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val store = DataStoreSettingsStore(context)
    private lateinit var activity: MainActivity

    @Before
    fun prepare() {
        device.pressHome()
        runBlocking {
            store.setProtectionEnabled(false)
            store.setTemporaryAllowUntil(null)
            CreatePinUseCase(store, Pbkdf2PinHasher())("4826", "4826")
            store.acceptAccessibilityDisclosure()
            store.setPlatformEnabled("youtube_shorts", true)
        }
        activity = instrumentation.startActivitySync(launchIntent()) as MainActivity
        awaitPinEntry()
    }

    @After
    fun cleanup() {
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finish() }
        runBlocking { store.setTemporaryAllowUntil(null) }
        device.pressHome()
    }

    @Test
    fun directPlatformMutationWithoutAuthorizationCannotWrite() {
        assertMutationDenied()
    }

    @Test
    fun wrongPinAndCancelledEntryCannotWrite() {
        submitPin("0000")
        assertTrue("Incorrect PIN result missing", device.wait(Until.hasObject(By.text(
            activity.resources.getQuantityString(R.plurals.pin_entry_attempts_remaining, 4, 4),
        )), 5_000L))
        assertFalse(activity.parentalSettingsSession.isAuthorized)
        assertMutationDenied()
        device.pressBack()
        device.pressBack()
        await("PIN cancellation did not pause Activity") { !activity.hasWindowFocus() }
        assertMutationDenied()
    }

    @Test
    fun correctPinAllowsActualPlatformToggle() {
        unlock()
        // Exercise the actual Compose switch callback, then read persistent settings.
        device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4,
            device.displayWidth / 2, device.displayHeight / 3, 40)
        val platform = device.wait(Until.findObject(By.text(activity.getString(R.string.platform_youtube_shorts))), 5_000L)
        assertTrue("YouTube platform row missing", platform != null)
        val labelBounds = requireNotNull(platform).visibleBounds
        // Layout-only Compose rows need not exist in the accessibility hierarchy.
        val toggle = device.findObjects(By.checkable(true)).singleOrNull {
            val bounds = it.visibleBounds
            bounds.left >= labelBounds.right && bounds.top < labelBounds.bottom &&
                bounds.bottom > labelBounds.top
        }
        assertTrue("YouTube platform toggle missing", toggle != null)
        requireNotNull(toggle).click()
        await("UI platform toggle did not persist") { !platformEnabled() }
    }

    @Test
    fun backgroundForegroundRequiresNewPinAtMutationPath() {
        unlock()
        device.pressHome()
        await("Background did not revoke session") { !activity.parentalSettingsSession.isAuthorized }
        assertMutationDenied()
        context.startActivity(launchIntent().addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        awaitPinEntry()
        assertMutationDenied()
        unlock()
        mutatePlatform()
        assertFalse(platformEnabled())
    }

    @Test
    fun activityRecreationRestoresNavigationWithoutWriteAuthorization() {
        unlock()
        val previous = activity
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        try {
            instrumentation.runOnMainSync { previous.recreate() }
            activity = requireNotNull(monitor.waitForActivityWithTimeout(15_000L)) as MainActivity
        } finally {
            instrumentation.removeMonitor(monitor)
        }
        assertTrue(previous !== activity)
        awaitPinEntry()
        assertMutationDenied()
        unlock()
        mutatePlatform()
        assertFalse(platformEnabled())
    }

    private fun launchIntent() = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    @Test
    fun temporaryAllowStillRequiresPinAndCancellationDoesNotChangeSettings() {
        requestTemporaryAllow()
        assertMutationDenied()
        submitPin("4826")
        assertTrue(device.wait(Until.hasObject(By.text(activity.getString(R.string.temporary_allow_title))), 10_000L))
        requireNotNull(device.findObject(By.text(activity.getString(R.string.temporary_allow_cancel)))).click()
        await("Temporary allow cancellation did not close Activity") { !activity.hasWindowFocus() }
        assertEquals(null, temporaryAllowUntil())
        assertTrue(platformEnabled())
    }

    @Test
    fun correctPinStillAllowsTemporaryAllowance() {
        requestTemporaryAllow()
        submitPin("4826")
        assertTrue(device.wait(Until.hasObject(By.text(activity.getString(R.string.temporary_allow_title))), 10_000L))
        val duration = activity.resources.getQuantityString(R.plurals.temporary_allow_duration_minutes, 5, 5)
        requireNotNull(device.findObject(By.text(duration))).click()
        await("Temporary allowance did not persist") { temporaryAllowUntil() != null }
        assertTrue(platformEnabled())
    }

    private fun requestTemporaryAllow() {
        context.startActivity(launchIntent().addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_TEMPORARY_ALLOW_PIN, true))
        awaitPinEntry()
        instrumentation.waitForIdleSync()
    }

    private fun temporaryAllowUntil() = runBlocking {
        store.readSettings().first().protectionConfiguration.temporaryAllowUntilMillis
    }

    private fun awaitPinEntry() {
        assertTrue("Fresh PIN entry missing", device.wait(
            Until.hasObject(By.text(activity.getString(R.string.pin_entry_title))), 15_000L,
        ))
    }

    private fun submitPin(pin: String) {
        requireNotNull(device.wait(Until.findObject(By.clazz("android.widget.EditText")), 5_000L)).text = pin
        requireNotNull(device.findObject(By.text(activity.getString(R.string.pin_entry_submit)))).click()
        instrumentation.waitForIdleSync()
    }

    private fun unlock() {
        submitPin("4826")
        await("Correct PIN did not authorize session") { activity.parentalSettingsSession.isAuthorized }
        assertTrue("Dashboard not shown", device.wait(
            Until.hasObject(By.text(activity.getString(R.string.dashboard_title))), 10_000L,
        ))
    }

    private fun mutatePlatform() = runBlocking(Dispatchers.Main) {
        activity.parentalSettingsSession.setPlatformEnabled("youtube_shorts", false)
    }

    private fun assertMutationDenied() {
        try {
            mutatePlatform()
            throw AssertionError("Unauthorized platform mutation succeeded")
        } catch (_: IllegalStateException) {
            assertTrue("Unauthorized path changed protected settings", platformEnabled())
        }
    }

    private fun platformEnabled() = runBlocking {
        store.readSettings().first().protectionConfiguration.isPlatformEnabled("youtube_shorts")
    }

    private fun await(message: String, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 10_000L
        while (!predicate() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50L)
        assertTrue(message, predicate())
    }
}
