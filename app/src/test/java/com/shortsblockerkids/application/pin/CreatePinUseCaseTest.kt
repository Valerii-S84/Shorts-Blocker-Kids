package com.shortsblockerkids.application.pin

import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.port.PinHashingPort
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.PinStateUpdate
import com.shortsblockerkids.domain.pin.PinValidationReason
import com.shortsblockerkids.domain.pin.PinValidationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CreatePinUseCaseTest {
    @Test
    fun invalidPinReturnsTypedRejectionWithoutHashingOrWriting() {
        assertRejected(
            pin = "482",
            confirmation = "482",
            expectedReason = PinValidationReason.INVALID_LENGTH,
        )
    }

    @Test
    fun weakPinReturnsTypedRejectionWithoutHashingOrWriting() {
        assertRejected(
            pin = "0000",
            confirmation = "0000",
            expectedReason = PinValidationReason.WEAK_PIN,
        )
    }

    @Test
    fun mismatchedConfirmationReturnsTypedRejectionWithoutHashingOrWriting() {
        assertRejected(
            pin = "4826",
            confirmation = "4827",
            expectedReason = PinValidationReason.CONFIRMATION_MISMATCH,
        )
    }

    @Test
    fun strongLeadingZeroPinCreatesCredentialResetsAttemptsAndWritesOnce() =
        runBlocking {
            val stateStore = RecordingPinStateStore()
            val pinHasher = RecordingPinHasher()
            val useCase = CreatePinUseCase(stateStore, pinHasher)

            val result = useCase(pin = "004826", confirmation = "004826")

            assertSame(PinValidationResult.Valid, result)
            assertEquals(1, pinHasher.saltGenerationCount)
            assertEquals(listOf(HashInput(pin = "004826", saltBase64 = "fixed-salt")), pinHasher.hashInputs)
            assertEquals(
                listOf(
                    SavedPinState(
                        credential =
                            PinCredential(
                                hashBase64 = "fixed-hash",
                                saltBase64 = "fixed-salt",
                                hashVersion = 1,
                            ),
                        attemptState = PinAttemptState(),
                    ),
                ),
                stateStore.savedStates,
            )
        }

    private fun assertRejected(
        pin: String,
        confirmation: String,
        expectedReason: PinValidationReason,
    ) = runBlocking {
        val stateStore = RecordingPinStateStore()
        val pinHasher = RecordingPinHasher()
        val useCase = CreatePinUseCase(stateStore, pinHasher)

        val result = useCase(pin, confirmation) as PinValidationResult.Invalid

        assertEquals(expectedReason, result.reason)
        assertEquals(0, pinHasher.saltGenerationCount)
        assertEquals(emptyList<HashInput>(), pinHasher.hashInputs)
        assertEquals(emptyList<SavedPinState>(), stateStore.savedStates)
    }

    private data class HashInput(
        val pin: String,
        val saltBase64: String,
    )

    private data class SavedPinState(
        val credential: PinCredential,
        val attemptState: PinAttemptState,
    )

    private class RecordingPinHasher : PinHashingPort {
        var saltGenerationCount: Int = 0
            private set
        val hashInputs = mutableListOf<HashInput>()

        override fun generateSalt(): String {
            saltGenerationCount += 1
            return "fixed-salt"
        }

        override fun hash(
            pin: String,
            saltBase64: String,
        ): String {
            hashInputs += HashInput(pin, saltBase64)
            return "fixed-hash"
        }

        override fun matches(
            expectedHashBase64: String,
            actualHashBase64: String,
        ): Boolean = error("Creation must not compare hashes")
    }

    private class RecordingPinStateStore : PinStateStore {
        val savedStates = mutableListOf<SavedPinState>()

        override suspend fun savePinState(
            credential: PinCredential,
            attemptState: PinAttemptState,
        ) {
            savedStates += SavedPinState(credential, attemptState)
        }

        override suspend fun verifyAndUpdateAtomically(
            verification: (PinCredential?, PinAttemptState) -> PinStateUpdate,
        ): PinVerificationResult = error("Creation must not verify")
    }
}
