package com.shortsblockerkids.application.pin

import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.port.PinHashingPort
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.PinStateUpdate
import com.shortsblockerkids.application.port.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VerifyPinUseCaseTest {
    @Test
    fun matchingPinReturnsSuccessAndResetsAttemptState() =
        runBlocking {
            val fixture = fixture(attemptState = PinAttemptState(failedAttempts = 3))

            val result = fixture.useCase("4826")

            assertSame(PinVerificationResult.Success, result)
            assertEquals(PinAttemptState(), fixture.stateStore.attemptState)
            fixture.assertSingleBoundaryCall()
        }

    @Test
    fun mismatchedPinReturnsFailureAndRecordsAttempt() =
        runBlocking {
            val fixture = fixture()

            val result = fixture.useCase("1111")

            assertEquals(PinVerificationResult.Failure(remainingAttempts = 4), result)
            assertEquals(PinAttemptState(failedAttempts = 1), fixture.stateStore.attemptState)
            fixture.assertSingleBoundaryCall()
        }

    @Test
    fun malformedInputsReturnInvalidInputWithoutHashingOrMutatingAttempts() =
        runBlocking {
            listOf("", "482", "48269x", "4826901").forEach { malformedPin ->
                val initialState = PinAttemptState(failedAttempts = 4)
                val fixture =
                    fixture(
                        storedCredential = credential(pin = malformedPin),
                        attemptState = initialState,
                    )

                val result = fixture.useCase(malformedPin)

                assertSame(PinVerificationResult.InvalidInput, result)
                assertEquals(initialState, fixture.stateStore.attemptState)
                assertEquals(null, fixture.stateStore.lastUpdate?.updatedAttemptState)
                assertEquals(emptyList<HashInput>(), fixture.pinHasher.hashInputs)
                assertEquals(emptyList<MatchInput>(), fixture.pinHasher.matchInputs)
                fixture.assertSingleBoundaryCall()
            }
        }

    @Test
    fun malformedInputDuringActiveLockoutReturnsInvalidInputAndPreservesLockout() =
        runBlocking {
            val initialState = PinAttemptState(failedAttempts = 5, lockoutUntil = 31_000L)
            val fixture = fixture(attemptState = initialState, nowMillis = 2_000L)

            val result = fixture.useCase("482")

            assertSame(PinVerificationResult.InvalidInput, result)
            assertEquals(initialState, fixture.stateStore.attemptState)
            assertEquals(null, fixture.stateStore.lastUpdate?.updatedAttemptState)
            assertEquals(emptyList<HashInput>(), fixture.pinHasher.hashInputs)
            assertEquals(emptyList<MatchInput>(), fixture.pinHasher.matchInputs)
            fixture.assertSingleBoundaryCall()
        }

    @Test
    fun weakPersistedPinRemainsVerifiable() =
        runBlocking {
            val fixture =
                fixture(
                    storedCredential = credential(pin = "0000"),
                    attemptState = PinAttemptState(failedAttempts = 2),
                )

            val result = fixture.useCase("0000")

            assertSame(PinVerificationResult.Success, result)
            assertEquals(PinAttemptState(), fixture.stateStore.attemptState)
            fixture.assertSingleBoundaryCall()
        }

    @Test
    fun attemptsOneThroughFourReturnExactRemainingCounts() =
        runBlocking {
            (1..4).forEach { failedAttempt ->
                val fixture = fixture(attemptState = PinAttemptState(failedAttempts = failedAttempt - 1))

                val result = fixture.useCase("1111")

                assertEquals(PinVerificationResult.Failure(5 - failedAttempt), result)
                assertEquals(PinAttemptState(failedAttempts = failedAttempt), fixture.stateStore.attemptState)
                fixture.assertSingleBoundaryCall()
            }
        }

    @Test
    fun attemptsFiveSixSevenAndEightReturnEscalatingLockouts() =
        runBlocking {
            val cases =
                listOf(
                    LockoutCase(previousAttempts = 4, failedAttempts = 5, durationMillis = 30_000L),
                    LockoutCase(previousAttempts = 5, failedAttempts = 6, durationMillis = 60_000L),
                    LockoutCase(previousAttempts = 6, failedAttempts = 7, durationMillis = 300_000L),
                    LockoutCase(previousAttempts = 7, failedAttempts = 8, durationMillis = 300_000L),
                )

            cases.forEach { case ->
                val fixture = fixture(attemptState = PinAttemptState(failedAttempts = case.previousAttempts))

                val result = fixture.useCase("1111")

                assertEquals(
                    PinVerificationResult.Locked(
                        untilMillis = NOW_MILLIS + case.durationMillis,
                        remainingMillis = case.durationMillis,
                    ),
                    result,
                )
                assertEquals(
                    PinAttemptState(
                        failedAttempts = case.failedAttempts,
                        lockoutUntil = NOW_MILLIS + case.durationMillis,
                    ),
                    fixture.stateStore.attemptState,
                )
                fixture.assertSingleBoundaryCall()
            }
        }

    @Test
    fun activeLockoutReturnsRemainingTimeWithoutIncrementOrHashing() =
        runBlocking {
            val initialState = PinAttemptState(failedAttempts = 5, lockoutUntil = 31_000L)
            val fixture = fixture(attemptState = initialState, nowMillis = 2_000L)

            val result = fixture.useCase("4826")

            assertEquals(
                PinVerificationResult.Locked(untilMillis = 31_000L, remainingMillis = 29_000L),
                result,
            )
            assertEquals(initialState, fixture.stateStore.attemptState)
            assertEquals(null, fixture.stateStore.lastUpdate?.updatedAttemptState)
            assertEquals(emptyList<HashInput>(), fixture.pinHasher.hashInputs)
            fixture.assertSingleBoundaryCall()
        }

    @Test
    fun successAtExpiryBoundaryResetsAttemptsAndLockout() =
        runBlocking {
            val fixture =
                fixture(
                    attemptState = PinAttemptState(failedAttempts = 5, lockoutUntil = NOW_MILLIS),
                )

            val result = fixture.useCase("4826")

            assertSame(PinVerificationResult.Success, result)
            assertEquals(PinAttemptState(), fixture.stateStore.attemptState)
            fixture.assertSingleBoundaryCall()
        }

    @Test
    fun missingCredentialReturnsNotConfiguredWithoutMutatingAttempts() =
        runBlocking {
            val initialState = PinAttemptState(failedAttempts = 3)
            val fixture = fixture(storedCredential = null, attemptState = initialState)

            val result = fixture.useCase("4826")

            assertSame(PinVerificationResult.NotConfigured, result)
            assertEquals(initialState, fixture.stateStore.attemptState)
            assertEquals(null, fixture.stateStore.lastUpdate?.updatedAttemptState)
            assertEquals(emptyList<HashInput>(), fixture.pinHasher.hashInputs)
            fixture.assertSingleBoundaryCall()
        }

    @Test
    fun corruptCredentialReturnsNotConfiguredWithoutMutatingAttempts() =
        runBlocking {
            val initialState = PinAttemptState(failedAttempts = 3)
            val pinHasher = RecordingPinHasher(throwOnHash = true)
            val fixture =
                fixture(
                    storedCredential =
                        PinCredential(
                            hashBase64 = "not-base64",
                            saltBase64 = "not-base64",
                            hashVersion = 1,
                        ),
                    attemptState = initialState,
                    pinHasher = pinHasher,
                )

            val result = fixture.useCase("4826")

            assertSame(PinVerificationResult.NotConfigured, result)
            assertEquals(initialState, fixture.stateStore.attemptState)
            assertEquals(null, fixture.stateStore.lastUpdate?.updatedAttemptState)
            fixture.assertSingleBoundaryCall()
        }

    @Test
    fun arbitraryHashVersionKeepsCurrentNonDispatchVerificationBehavior() =
        runBlocking {
            val fixture = fixture(storedCredential = credential(pin = "4826", hashVersion = 99))

            val result = fixture.useCase("4826")

            assertSame(PinVerificationResult.Success, result)
            assertEquals(listOf(HashInput(pin = "4826", saltBase64 = "salt")), fixture.pinHasher.hashInputs)
            fixture.assertSingleBoundaryCall()
        }

    @Test
    fun verificationReadsTimeOnceAndPerformsOneAtomicCall() =
        runBlocking {
            val fixture = fixture(nowMillis = 12_345L)

            fixture.useCase("1111")

            fixture.assertSingleBoundaryCall()
        }

    private fun fixture(
        storedCredential: PinCredential? = credential(),
        attemptState: PinAttemptState = PinAttemptState(),
        nowMillis: Long = NOW_MILLIS,
        pinHasher: RecordingPinHasher = RecordingPinHasher(),
    ): Fixture {
        val stateStore = RecordingPinStateStore(storedCredential, attemptState)
        val timeProvider = RecordingTimeProvider(nowMillis)
        return Fixture(
            useCase = VerifyPinUseCase(stateStore, pinHasher, timeProvider),
            stateStore = stateStore,
            pinHasher = pinHasher,
            timeProvider = timeProvider,
        )
    }

    private fun credential(
        pin: String = "4826",
        hashVersion: Int = 1,
    ): PinCredential =
        PinCredential(
            hashBase64 = "hash:$pin",
            saltBase64 = "salt",
            hashVersion = hashVersion,
        )

    private data class LockoutCase(
        val previousAttempts: Int,
        val failedAttempts: Int,
        val durationMillis: Long,
    )

    private data class HashInput(
        val pin: String,
        val saltBase64: String,
    )

    private data class MatchInput(
        val expectedHashBase64: String,
        val actualHashBase64: String,
    )

    private data class Fixture(
        val useCase: VerifyPinUseCase,
        val stateStore: RecordingPinStateStore,
        val pinHasher: RecordingPinHasher,
        val timeProvider: RecordingTimeProvider,
    ) {
        fun assertSingleBoundaryCall() {
            assertEquals(1, timeProvider.callCount)
            assertEquals(1, stateStore.atomicCallCount)
        }
    }

    private class RecordingTimeProvider(
        private val nowMillis: Long,
    ) : TimeProvider {
        var callCount: Int = 0
            private set

        override fun currentTimeMillis(): Long {
            callCount += 1
            return nowMillis
        }
    }

    private class RecordingPinHasher(
        private val throwOnHash: Boolean = false,
    ) : PinHashingPort {
        val hashInputs = mutableListOf<HashInput>()
        val matchInputs = mutableListOf<MatchInput>()

        override fun generateSalt(): String = error("Verification must not generate a salt")

        override fun hash(
            pin: String,
            saltBase64: String,
        ): String {
            hashInputs += HashInput(pin, saltBase64)
            if (throwOnHash) {
                throw IllegalArgumentException("Corrupt credential metadata")
            }
            return "hash:$pin"
        }

        override fun matches(
            expectedHashBase64: String,
            actualHashBase64: String,
        ): Boolean {
            matchInputs += MatchInput(expectedHashBase64, actualHashBase64)
            return expectedHashBase64 == actualHashBase64
        }
    }

    private class RecordingPinStateStore(
        private val credential: PinCredential?,
        var attemptState: PinAttemptState,
    ) : PinStateStore {
        var atomicCallCount: Int = 0
            private set
        var lastUpdate: PinStateUpdate? = null
            private set

        override suspend fun savePinState(
            credential: PinCredential,
            attemptState: PinAttemptState,
        ) = error("Verification must not save a new credential")

        override suspend fun verifyAndUpdateAtomically(
            verification: (PinCredential?, PinAttemptState) -> PinStateUpdate,
        ): PinVerificationResult {
            atomicCallCount += 1
            val update = verification(credential, attemptState)
            lastUpdate = update
            update.updatedAttemptState?.let { updatedState ->
                attemptState = updatedState
            }
            return update.result
        }
    }

    private companion object {
        const val NOW_MILLIS = 1_000L
    }
}
