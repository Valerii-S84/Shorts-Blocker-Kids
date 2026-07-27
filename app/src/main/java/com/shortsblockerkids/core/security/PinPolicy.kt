package com.shortsblockerkids.core.security

object PinPolicy {
    private val weakPins = setOf("0000", "1111", "1234", "123456")
    private val allowedPattern = Regex("^\\d{4,6}$")

    fun validate(
        pin: String,
        confirmation: String,
    ): PinValidationResult {
        if (!isVerificationInputComplete(pin)) {
            return PinValidationResult.Invalid(PinValidationReason.INVALID_LENGTH)
        }

        if (pin in weakPins) {
            return PinValidationResult.Invalid(PinValidationReason.WEAK_PIN)
        }

        if (pin != confirmation) {
            return PinValidationResult.Invalid(PinValidationReason.CONFIRMATION_MISMATCH)
        }

        return PinValidationResult.Valid
    }

    fun isVerificationInputComplete(pin: String): Boolean = allowedPattern.matches(pin)
}

enum class PinValidationReason {
    INVALID_LENGTH,
    WEAK_PIN,
    CONFIRMATION_MISMATCH,
}

sealed interface PinValidationResult {
    data object Valid : PinValidationResult

    data class Invalid(
        val reason: PinValidationReason,
    ) : PinValidationResult
}
