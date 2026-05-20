package com.shortsblockerkids.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun hashesPinWithSaltAndMatchesExpectedHash() {
        val hasher = PinHasher()
        val salt = hasher.generateSalt()
        val hash = hasher.hash(pin = "4826", saltBase64 = salt)
        val sameHash = hasher.hash(pin = "4826", saltBase64 = salt)
        val differentHash = hasher.hash(pin = "4827", saltBase64 = salt)

        assertTrue(hasher.matches(hash, sameHash))
        assertFalse(hasher.matches(hash, differentHash))
        assertFalse(hash.contains("4826"))
    }
}
