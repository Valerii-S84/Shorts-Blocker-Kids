package com.shortsblockerkids.application.port

fun interface ProtectionActivationStore {
    suspend fun recordSuccessfulProtectionActivation(nowMillis: Long)
}
