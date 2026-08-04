package com.shortsblockerkids.domain.pin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {
    @Test
    fun acceptsStrongPinsAtEveryAllowedLength() {
        listOf("4826", "48269", "482691").forEach { pin ->
            assertTrue(PinPolicy.validate(pin, pin) is PinValidationResult.Valid)
        }
    }

    @Test
    fun acceptsStrongPinsWithLeadingZeros() {
        listOf("0482", "00482", "004826").forEach { pin ->
            assertTrue(PinPolicy.validate(pin, pin) is PinValidationResult.Valid)
        }
    }

    @Test
    fun rejectsEveryWeakPin() {
        listOf("0000", "1111", "1234", "123456").forEach { pin ->
            assertInvalidCreation(pin, pin, PinValidationReason.WEAK_PIN)
        }
    }

    @Test
    fun rejectsShortLongNonDigitAndWhitespaceCreationPins() {
        listOf(
            "",
            "482",
            "4826910",
            "48a6",
            " 4826",
            "4826 ",
            "48-26",
        ).forEach { pin ->
            assertInvalidCreation(pin, pin, PinValidationReason.INVALID_LENGTH)
        }
    }

    @Test
    fun rejectsMismatchedConfirmation() {
        assertInvalidCreation(
            pin = "4826",
            confirmation = "4827",
            expectedReason = PinValidationReason.CONFIRMATION_MISMATCH,
        )
    }

    @Test
    fun verificationFormatAcceptsFourFiveSixDigitsIncludingWeakAndLeadingZeroValues() {
        listOf("4826", "48269", "482691", "004826", "0000").forEach { pin ->
            assertTrue("expected valid verification format: $pin", PinPolicy.isVerificationInputComplete(pin))
        }
    }

    @Test
    fun verificationFormatRejectsMalformedValues() {
        listOf("", "482", "4826910", "48a6", " 4826", "4826 ", "48-26").forEach { pin ->
            assertFalse("expected invalid verification format: $pin", PinPolicy.isVerificationInputComplete(pin))
        }
    }

    private fun assertInvalidCreation(
        pin: String,
        confirmation: String,
        expectedReason: PinValidationReason,
    ) {
        val result = PinPolicy.validate(pin, confirmation) as PinValidationResult.Invalid

        assertEquals(expectedReason, result.reason)
    }
}
