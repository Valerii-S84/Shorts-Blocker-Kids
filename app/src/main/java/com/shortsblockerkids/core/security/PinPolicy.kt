package com.shortsblockerkids.core.security

object PinPolicy {
    private val weakPins = setOf("0000", "1111", "1234", "123456")
    private val allowedPattern = Regex("^\\d{4,6}$")

    fun validate(
        pin: String,
        confirmation: String,
    ): PinValidationResult {
        if (!allowedPattern.matches(pin)) {
            return PinValidationResult.Invalid("PIN must be 4-6 digits.")
        }

        if (pin in weakPins) {
            return PinValidationResult.Invalid("Choose a stronger PIN.")
        }

        if (pin != confirmation) {
            return PinValidationResult.Invalid("PIN confirmation does not match.")
        }

        return PinValidationResult.Valid
    }
}

sealed interface PinValidationResult {
    data object Valid : PinValidationResult

    data class Invalid(
        val message: String,
    ) : PinValidationResult
}
