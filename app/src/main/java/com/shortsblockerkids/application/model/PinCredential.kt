package com.shortsblockerkids.application.model

data class PinCredential(
    val hashBase64: String,
    val saltBase64: String,
    val hashVersion: Int,
)
