package com.shortsblockerkids.infrastructure.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class Pbkdf2PinHasherTest {
    private val hasher = Pbkdf2PinHasher()

    @Test
    fun fixedPinAndSaltMatchKnownPbkdf2Vector() {
        val hash = hasher.hash(pin = "4826", saltBase64 = FIXED_SALT_BASE64)

        assertEquals(EXPECTED_HASH_BASE64, hash)
    }

    @Test
    fun generatedSaltIsSixteenBytesInStandardBase64() {
        val salts = List(4) { hasher.generateSalt() }

        salts.forEach { saltBase64 ->
            val decodedSalt = Base64.getDecoder().decode(saltBase64)
            assertEquals(16, decodedSalt.size)
            assertEquals(saltBase64, Base64.getEncoder().encodeToString(decodedSalt))
        }
        assertEquals(salts.size, salts.toSet().size)
    }

    @Test
    fun matchesExpectedHashRejectsDifferentPinAndDoesNotExposePlaintext() {
        val hash = hasher.hash(pin = "4826", saltBase64 = FIXED_SALT_BASE64)
        val differentHash = hasher.hash(pin = "4827", saltBase64 = FIXED_SALT_BASE64)

        assertTrue(hasher.matches(EXPECTED_HASH_BASE64, hash))
        assertFalse(hasher.matches(EXPECTED_HASH_BASE64, differentHash))
        assertFalse(hash.contains("4826"))
    }

    private companion object {
        const val FIXED_SALT_BASE64 = "AAECAwQFBgcICQoLDA0ODw=="
        const val EXPECTED_HASH_BASE64 = "JJfkH+VveNsEbC4vvoIyDBewEbKwjYEa29445Woz1lY="
    }
}
