package com.shortsblockerkids.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinHasher {
    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hash(
        pin: String,
        saltBase64: String,
    ): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val bytes =
                SecretKeyFactory
                    .getInstance(ALGORITHM)
                    .generateSecret(spec)
                    .encoded
            Base64.getEncoder().encodeToString(bytes)
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(
        expectedHashBase64: String,
        actualHashBase64: String,
    ): Boolean {
        val expected = Base64.getDecoder().decode(expectedHashBase64)
        val actual = Base64.getDecoder().decode(actualHashBase64)
        return MessageDigest.isEqual(expected, actual)
    }

    companion object {
        const val CURRENT_VERSION = 1
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16
    }
}
