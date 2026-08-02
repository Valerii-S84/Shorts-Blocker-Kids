package com.shortsblockerkids.application.pin

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult

    data object NotConfigured : PinVerificationResult

    data class Failure(
        val remainingAttempts: Int,
    ) : PinVerificationResult

    data class Locked(
        val untilMillis: Long,
    ) : PinVerificationResult
}
