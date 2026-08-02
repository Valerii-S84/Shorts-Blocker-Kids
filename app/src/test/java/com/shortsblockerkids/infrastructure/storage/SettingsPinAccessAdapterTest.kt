package com.shortsblockerkids.infrastructure.storage

import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.PinStateUpdate
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.core.security.PinHasher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SettingsPinAccessAdapterTest {
    @Test
    fun activeLockoutDoesNotMutateAttemptState() =
        runBlocking {
            val initialState =
                PinAttemptState(
                    failedAttempts = 5,
                    lockoutUntil = 31_000L,
                )
            val stateStore =
                RecordingPinStateStore(
                    credential = credential(),
                    attemptState = initialState,
                )

            val result = adapter(stateStore, nowMillis = 2_000L).verifyPin("4826")

            assertEquals(PinVerificationResult.Locked(untilMillis = 31_000L), result)
            assertEquals(initialState, stateStore.attemptState)
            assertEquals(0, stateStore.appliedUpdateCount)
        }

    @Test
    fun successAtExpiryBoundaryClearsAttemptState() =
        runBlocking {
            val stateStore =
                RecordingPinStateStore(
                    credential = credential(),
                    attemptState =
                        PinAttemptState(
                            failedAttempts = 5,
                            lockoutUntil = 2_000L,
                        ),
                )

            val result = adapter(stateStore, nowMillis = 2_000L).verifyPin("4826")

            assertSame(PinVerificationResult.Success, result)
            assertEquals(PinAttemptState(), stateStore.attemptState)
        }

    @Test
    fun corruptCredentialReturnsNotConfiguredWithoutMutation() =
        runBlocking {
            val initialState = PinAttemptState(failedAttempts = 3)
            val stateStore =
                RecordingPinStateStore(
                    credential =
                        PinCredential(
                            hashBase64 = "not-base64",
                            saltBase64 = "not-base64",
                            hashVersion = PinHasher.CURRENT_VERSION,
                        ),
                    attemptState = initialState,
                )

            val result = adapter(stateStore, nowMillis = 1_000L).verifyPin("4826")

            assertSame(PinVerificationResult.NotConfigured, result)
            assertEquals(initialState, stateStore.attemptState)
            assertEquals(0, stateStore.appliedUpdateCount)
        }

    @Test
    fun fifthMismatchPersistsThirtySecondLockout() =
        runBlocking {
            val stateStore =
                RecordingPinStateStore(
                    credential = credential(),
                    attemptState = PinAttemptState(failedAttempts = 4),
                )

            val result = adapter(stateStore, nowMillis = 1_000L).verifyPin("1111")

            assertEquals(PinVerificationResult.Locked(untilMillis = 31_000L), result)
            assertEquals(
                PinAttemptState(
                    failedAttempts = 5,
                    lockoutUntil = 31_000L,
                ),
                stateStore.attemptState,
            )
        }

    private fun adapter(
        stateStore: PinStateStore,
        nowMillis: Long,
    ): SettingsPinAccessAdapter =
        SettingsPinAccessAdapter(
            pinStateStore = stateStore,
            timeProvider = TimeProvider { nowMillis },
        )

    private fun credential(pin: String = "4826"): PinCredential {
        val hasher = PinHasher()
        val salt = hasher.generateSalt()
        return PinCredential(
            hashBase64 = hasher.hash(pin = pin, saltBase64 = salt),
            saltBase64 = salt,
            hashVersion = PinHasher.CURRENT_VERSION,
        )
    }

    private class RecordingPinStateStore(
        var credential: PinCredential?,
        var attemptState: PinAttemptState,
    ) : PinStateStore {
        var appliedUpdateCount: Int = 0
            private set

        override suspend fun savePinState(
            credential: PinCredential,
            attemptState: PinAttemptState,
        ) {
            this.credential = credential
            this.attemptState = attemptState
        }

        override suspend fun verifyAndUpdateAtomically(
            verification: (PinCredential?, PinAttemptState) -> PinStateUpdate,
        ): PinVerificationResult {
            val update = verification(credential, attemptState)
            update.updatedAttemptState?.let { updatedState ->
                attemptState = updatedState
                appliedUpdateCount += 1
            }
            return update.result
        }
    }
}
