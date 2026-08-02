package com.shortsblockerkids.application.pin

import com.shortsblockerkids.application.port.PinAccessPort
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VerifyPinUseCaseTest {
    @Test
    fun returnsSuccessWithoutChangingIt() {
        assertReturnsExactResult(PinVerificationResult.Success)
    }

    @Test
    fun returnsNotConfiguredWithoutChangingIt() {
        assertReturnsExactResult(PinVerificationResult.NotConfigured)
    }

    @Test
    fun returnsFailureWithoutChangingIt() {
        assertReturnsExactResult(PinVerificationResult.Failure(remainingAttempts = 3))
    }

    @Test
    fun returnsLockedWithoutChangingIt() {
        assertReturnsExactResult(PinVerificationResult.Locked(untilMillis = 12_345L))
    }

    private fun assertReturnsExactResult(expectedResult: PinVerificationResult) =
        runBlocking {
            val pinAccessPort = RecordingPinAccessPort(expectedResult)
            val useCase = VerifyPinUseCase(pinAccessPort)

            val actualResult = useCase("004826")

            assertSame(expectedResult, actualResult)
            assertEquals(listOf("004826"), pinAccessPort.verifiedPins)
            assertEquals(emptyList<String>(), pinAccessPort.createdPins)
        }

    private class RecordingPinAccessPort(
        private val verificationResult: PinVerificationResult,
    ) : PinAccessPort {
        val createdPins = mutableListOf<String>()
        val verifiedPins = mutableListOf<String>()

        override suspend fun createPin(pin: String) {
            createdPins += pin
        }

        override suspend fun verifyPin(pin: String): PinVerificationResult {
            verifiedPins += pin
            return verificationResult
        }
    }
}
