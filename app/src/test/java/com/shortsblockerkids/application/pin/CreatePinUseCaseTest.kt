package com.shortsblockerkids.application.pin

import com.shortsblockerkids.application.port.PinAccessPort
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CreatePinUseCaseTest {
    @Test
    fun passesExactPinToCreateOperationOnce() =
        runBlocking {
            val pinAccessPort = RecordingPinAccessPort()
            val useCase = CreatePinUseCase(pinAccessPort)

            useCase("004826")

            assertEquals(listOf("004826"), pinAccessPort.createdPins)
            assertEquals(emptyList<String>(), pinAccessPort.verifiedPins)
        }

    private class RecordingPinAccessPort : PinAccessPort {
        val createdPins = mutableListOf<String>()
        val verifiedPins = mutableListOf<String>()

        override suspend fun createPin(pin: String) {
            createdPins += pin
        }

        override suspend fun verifyPin(pin: String): PinVerificationResult {
            verifiedPins += pin
            return PinVerificationResult.NotConfigured
        }
    }
}
