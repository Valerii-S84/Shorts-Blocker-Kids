package com.shortsblockerkids.application.port

import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.core.security.PinVerificationResult

interface PinStateStore {
    suspend fun savePinState(
        credential: PinCredential,
        attemptState: PinAttemptState,
    )

    suspend fun verifyAndUpdateAtomically(verification: (PinCredential?, PinAttemptState) -> PinStateUpdate): PinVerificationResult
}

data class PinStateUpdate(
    val result: PinVerificationResult,
    val updatedAttemptState: PinAttemptState? = null,
)
