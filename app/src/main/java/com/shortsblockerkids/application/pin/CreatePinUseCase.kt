package com.shortsblockerkids.application.pin

import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.port.PinHashingPort
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.domain.pin.PinPolicy
import com.shortsblockerkids.domain.pin.PinValidationResult

class CreatePinUseCase(
    private val pinStateStore: PinStateStore,
    private val pinHasher: PinHashingPort,
) {
    suspend operator fun invoke(
        pin: String,
        confirmation: String,
    ): PinValidationResult {
        val validation = PinPolicy.validate(pin, confirmation)
        if (validation is PinValidationResult.Invalid) {
            return validation
        }

        val salt = pinHasher.generateSalt()
        pinStateStore.savePinState(
            credential =
                PinCredential(
                    hashBase64 = pinHasher.hash(pin = pin, saltBase64 = salt),
                    saltBase64 = salt,
                    hashVersion = PinHashingPort.CURRENT_VERSION,
                ),
            attemptState = PinAttemptState(),
        )
        return PinValidationResult.Valid
    }
}
