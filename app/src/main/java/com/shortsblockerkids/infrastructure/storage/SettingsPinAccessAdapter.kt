package com.shortsblockerkids.infrastructure.storage

import com.shortsblockerkids.application.port.PinAccessPort
import com.shortsblockerkids.core.security.PinVerificationResult
import com.shortsblockerkids.core.storage.SettingsRepository

class SettingsPinAccessAdapter(
    private val settingsRepository: SettingsRepository,
) : PinAccessPort {
    override suspend fun createPin(pin: String) {
        settingsRepository.savePin(pin)
    }

    override suspend fun verifyPin(pin: String): PinVerificationResult = settingsRepository.verifyPin(pin)
}
