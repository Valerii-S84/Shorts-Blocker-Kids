package com.shortsblockerkids.application.port

import com.shortsblockerkids.core.security.PinVerificationResult

interface PinAccessPort {
    suspend fun createPin(pin: String)

    suspend fun verifyPin(pin: String): PinVerificationResult
}
