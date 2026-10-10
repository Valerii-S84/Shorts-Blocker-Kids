package com.shortsblockerkids.presentation.app

import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.domain.pin.PinValidationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalSettingsSessionTest {
    private var writes = 0

    private fun session(
        verify: suspend (String) -> PinVerificationResult = {
            if (it == "4826") PinVerificationResult.Success else PinVerificationResult.Failure(4)
        },
    ) = ParentalSettingsSession(
        createPin = { _, _ -> PinValidationResult.Valid },
        verifyPin = verify,
        writePlatform = { _, _ -> writes += 1 },
    )

    private suspend fun assertDenied(session: ParentalSettingsSession) {
        try {
            session.setPlatformEnabled("youtube_shorts", false)
            throw AssertionError("Unauthorized mutation succeeded")
        } catch (_: IllegalStateException) {
            assertEquals(0, writes)
        }
    }

    @Test
    fun directMutationWithoutPinCannotWrite() =
        runBlocking {
            assertDenied(session())
        }

    @Test
    fun correctPinAllowsPlatformMutation() =
        runBlocking {
            val session = session()
            assertEquals(PinVerificationResult.Success, session.verifyPin("4826"))
            session.setPlatformEnabled("youtube_shorts", false)
            assertEquals(1, writes)
        }

    @Test
    fun wrongPinCannotWrite() =
        runBlocking {
            val session = session()
            assertTrue(session.verifyPin("0000") is PinVerificationResult.Failure)
            assertDenied(session)
        }

    @Test
    fun invalidOrUnconfiguredOrLockedVerificationCannotAuthorize() =
        runBlocking {
            listOf(
                PinVerificationResult.InvalidInput,
                PinVerificationResult.NotConfigured,
                PinVerificationResult.Locked(100L, 100L),
            ).forEach { result ->
                val session = session { result }
                session.verifyPin("4826")
                assertDenied(session)
            }
        }

    @Test
    fun backgroundRevokesPermissionUntilFreshPin() =
        runBlocking {
            val session = session()
            session.verifyPin("4826")
            session.lock()
            assertDenied(session)
            session.verifyPin("4826")
            session.setPlatformEnabled("youtube_shorts", false)
            assertEquals(1, writes)
        }

    @Test
    fun newActivitySessionCannotInheritAuthorization() =
        runBlocking {
            session().verifyPin("4826")
            assertDenied(session())
        }

    @Test
    fun pendingVerificationCannotUnlockAfterBackground() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val result = CompletableDeferred<PinVerificationResult>()
            val session =
                session {
                    started.complete(Unit)
                    result.await()
                }
            val attempt = async { session.verifyPin("4826") }
            started.await()
            session.lock()
            result.complete(PinVerificationResult.Success)
            assertEquals(PinVerificationResult.InvalidInput, attempt.await())
            assertDenied(session)
        }

    @Test
    fun cancelledVerificationCannotAuthorize() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val result = CompletableDeferred<PinVerificationResult>()
            val session =
                session {
                    started.complete(Unit)
                    result.await()
                }
            val attempt = async { session.verifyPin("4826") }
            started.await()
            attempt.cancelAndJoin()
            assertDenied(session)
        }

    @Test
    fun verificationStorageFailureCannotAuthorize() =
        runBlocking {
            val session = session { throw IllegalStateException("Storage failure") }
            try {
                session.verifyPin("4826")
            } catch (_: IllegalStateException) {
            }
            assertDenied(session)
        }

    @Test
    fun successfulPinCreationKeepsOnboardingAuthorized() =
        runBlocking {
            val session = session()
            assertEquals(PinValidationResult.Valid, session.createPin("4826", "4826"))
            session.setPlatformEnabled("youtube_shorts", false)
            assertEquals(1, writes)
        }

    @Test
    fun savedNavigationCannotGrantMutationPermission() =
        runBlocking {
            val navigation =
                ShortsBlockerKidsCoordinator(
                    AppScreen.Dashboard,
                    true,
                    false,
                    false,
                )
            assertTrue(navigation.isUnlocked)
            val session = session()
            assertDenied(session)
            navigation.onParentSessionLocked()
            assertFalse(navigation.isUnlocked)
            assertEquals(AppScreen.PinEntry, navigation.currentScreen)
        }
}
