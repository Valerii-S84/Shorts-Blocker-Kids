package com.shortsblockerkids.application.pin

import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.port.PinHashingPort
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.PinStateUpdate
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.pin.PinPolicy
import com.shortsblockerkids.domain.pin.PinRateLimiter

class VerifyPinUseCase(
    private val pinStateStore: PinStateStore,
    private val pinHasher: PinHashingPort,
    private val timeProvider: TimeProvider,
    private val pinRateLimiter: PinRateLimiter = PinRateLimiter(),
) {
    suspend operator fun invoke(pin: String): PinVerificationResult {
        val nowMillis = timeProvider.currentTimeMillis()
        return pinStateStore.verifyAndUpdateAtomically { credential, attemptState ->
            verificationUpdate(pin, credential, attemptState, nowMillis)
        }
    }

    private fun verificationUpdate(
        pin: String,
        credential: PinCredential?,
        attemptState: PinAttemptState,
        nowMillis: Long,
    ): PinStateUpdate {
        if (credential == null) {
            return PinStateUpdate(PinVerificationResult.NotConfigured)
        }
        activeLockout(attemptState, nowMillis)?.let { result ->
            return PinStateUpdate(result)
        }

        val matches =
            verifyCredential(pin, credential)
                ?: return PinStateUpdate(PinVerificationResult.NotConfigured)
        if (PinPolicy.isVerificationInputComplete(pin) && matches) {
            return PinStateUpdate(
                result = PinVerificationResult.Success,
                updatedAttemptState = PinAttemptState(),
            )
        }
        return failedAttemptUpdate(attemptState, nowMillis)
    }

    private fun verifyCredential(
        pin: String,
        credential: PinCredential,
    ): Boolean? =
        runCatching {
            val actualHash = pinHasher.hash(pin = pin, saltBase64 = credential.saltBase64)
            pinHasher.matches(
                expectedHashBase64 = credential.hashBase64,
                actualHashBase64 = actualHash,
            )
        }.getOrNull()

    private fun activeLockout(
        attemptState: PinAttemptState,
        nowMillis: Long,
    ): PinVerificationResult.Locked? {
        val lockoutUntil = attemptState.lockoutUntil ?: return null
        return lockoutUntil.takeIf { it > nowMillis }?.let { lockedResult(it, nowMillis) }
    }

    private fun failedAttemptUpdate(
        attemptState: PinAttemptState,
        nowMillis: Long,
    ): PinStateUpdate {
        val rateLimit = pinRateLimiter.recordFailure(attemptState.failedAttempts, nowMillis)
        val updatedAttemptState =
            PinAttemptState(
                failedAttempts = rateLimit.failedAttempts,
                lockoutUntil = rateLimit.lockoutUntil,
            )
        val result =
            rateLimit.lockoutUntil?.let { lockedResult(it, nowMillis) }
                ?: PinVerificationResult.Failure(
                    remainingAttempts =
                        pinRateLimiter.remainingAttemptsBeforeLockout(
                            rateLimit.failedAttempts,
                        ),
                )
        return PinStateUpdate(result, updatedAttemptState)
    }

    private fun lockedResult(
        untilMillis: Long,
        nowMillis: Long,
    ): PinVerificationResult.Locked =
        PinVerificationResult.Locked(
            untilMillis = untilMillis,
            remainingMillis = (untilMillis - nowMillis).coerceAtLeast(0L),
        )
}
