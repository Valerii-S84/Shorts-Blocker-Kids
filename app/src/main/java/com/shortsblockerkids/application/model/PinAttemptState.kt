package com.shortsblockerkids.application.model

data class PinAttemptState(
    val failedAttempts: Int = 0,
    val lockoutUntil: Long? = null,
)
