package com.shortsblockerkids.core.security

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
