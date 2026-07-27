package com.shortsblockerkids.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {
    @Test
    fun rejectsWeakPins() {
        val weakPins = listOf("0000", "1111", "1234", "123456")

        weakPins.forEach { pin ->
            val result = PinPolicy.validate(pin, pin) as PinValidationResult.Invalid

            assertEquals(PinValidationReason.WEAK_PIN, result.reason)
        }
    }

    @Test
    fun rejectsInvalidLengthWithReason() {
        val result = PinPolicy.validate("482", "482") as PinValidationResult.Invalid

        assertEquals(PinValidationReason.INVALID_LENGTH, result.reason)
    }

    @Test
    fun acceptsFourToSixDigitStrongPins() {
        assertTrue(PinPolicy.validate("4826", "4826") is PinValidationResult.Valid)
        assertTrue(PinPolicy.validate("482691", "482691") is PinValidationResult.Valid)
    }

    @Test
    fun rejectsMismatchedConfirmation() {
        val result = PinPolicy.validate("4826", "4827") as PinValidationResult.Invalid

        assertEquals(PinValidationReason.CONFIRMATION_MISMATCH, result.reason)
    }

    @Test
    fun verificationInputIsCompleteOnlyForFourToSixDigits() {
        assertTrue(PinPolicy.isVerificationInputComplete("4826"))
        assertTrue(PinPolicy.isVerificationInputComplete("482691"))

        assertFalse(PinPolicy.isVerificationInputComplete(""))
        assertFalse(PinPolicy.isVerificationInputComplete("482"))
        assertFalse(PinPolicy.isVerificationInputComplete("4826910"))
        assertFalse(PinPolicy.isVerificationInputComplete("48a6"))
    }

    @Test
    fun verificationInputRejectsWhitespaceAndSeparators() {
        assertFalse(PinPolicy.isVerificationInputComplete(" 4826"))
        assertFalse(PinPolicy.isVerificationInputComplete("4826 "))
        assertFalse(PinPolicy.isVerificationInputComplete("48-26"))
    }
}
