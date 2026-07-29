package com.shortsblockerkids.application.pin

import com.shortsblockerkids.application.port.PinAccessPort

class CreatePinUseCase(
    private val pinAccessPort: PinAccessPort,
) {
    suspend operator fun invoke(pin: String) {
        pinAccessPort.createPin(pin)
    }
}
