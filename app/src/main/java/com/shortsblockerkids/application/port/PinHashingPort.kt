package com.shortsblockerkids.application.port

interface PinHashingPort {
    fun generateSalt(): String

    fun hash(
        pin: String,
        saltBase64: String,
    ): String

    fun matches(
        expectedHashBase64: String,
        actualHashBase64: String,
    ): Boolean

    companion object {
        const val CURRENT_VERSION = 1
    }
}
