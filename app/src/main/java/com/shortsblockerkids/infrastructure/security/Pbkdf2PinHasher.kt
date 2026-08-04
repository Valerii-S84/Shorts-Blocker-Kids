package com.shortsblockerkids.infrastructure.security

import com.shortsblockerkids.application.port.PinHashingPort
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class Pbkdf2PinHasher : PinHashingPort {
    private val secureRandom = SecureRandom()

    override fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    override fun hash(
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

    override fun matches(
        expectedHashBase64: String,
        actualHashBase64: String,
    ): Boolean {
        val expected = Base64.getDecoder().decode(expectedHashBase64)
        val actual = Base64.getDecoder().decode(actualHashBase64)
        return MessageDigest.isEqual(expected, actual)
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ITERATIONS = 120_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_BYTES = 16
    }
}
