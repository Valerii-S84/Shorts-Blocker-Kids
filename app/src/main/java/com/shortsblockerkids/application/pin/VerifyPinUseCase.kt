package com.shortsblockerkids.application.pin

import com.shortsblockerkids.application.port.PinAccessPort
import com.shortsblockerkids.core.security.PinVerificationResult

class VerifyPinUseCase(
    private val pinAccessPort: PinAccessPort,
) {
    suspend operator fun invoke(pin: String): PinVerificationResult = pinAccessPort.verifyPin(pin)
}
