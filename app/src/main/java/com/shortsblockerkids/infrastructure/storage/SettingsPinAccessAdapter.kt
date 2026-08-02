package com.shortsblockerkids.infrastructure.storage

import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.application.port.PinAccessPort
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.PinStateUpdate
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.core.security.PinHasher
import com.shortsblockerkids.core.security.PinRateLimiter
import com.shortsblockerkids.infrastructure.time.SystemTimeProvider

internal class SettingsPinAccessAdapter internal constructor(
    private val pinStateStore: PinStateStore,
    private val pinHasher: PinHasher = PinHasher(),
    private val pinRateLimiter: PinRateLimiter = PinRateLimiter(),
    private val timeProvider: TimeProvider = SystemTimeProvider(),
) : PinAccessPort {
    override suspend fun createPin(pin: String) {
        val salt = pinHasher.generateSalt()
        pinStateStore.savePinState(
            credential =
                PinCredential(
                    hashBase64 = pinHasher.hash(pin = pin, saltBase64 = salt),
                    saltBase64 = salt,
                    hashVersion = PinHasher.CURRENT_VERSION,
                ),
            attemptState = PinAttemptState(),
        )
    }

    override suspend fun verifyPin(pin: String): PinVerificationResult {
        val nowMillis = timeProvider.currentTimeMillis()
        return pinStateStore.verifyAndUpdateAtomically { credential, attemptState ->
            verificationUpdate(
                pin = pin,
                credential = credential,
                attemptState = attemptState,
                nowMillis = nowMillis,
            )
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

        val lockoutUntil = attemptState.lockoutUntil
        if (lockoutUntil != null && lockoutUntil > nowMillis) {
            return PinStateUpdate(PinVerificationResult.Locked(lockoutUntil))
        }

        val matches =
            runCatching {
                val actualHash =
                    pinHasher.hash(
                        pin = pin,
                        saltBase64 = credential.saltBase64,
                    )
                pinHasher.matches(
                    expectedHashBase64 = credential.hashBase64,
                    actualHashBase64 = actualHash,
                )
            }.getOrElse {
                return PinStateUpdate(PinVerificationResult.NotConfigured)
            }
        if (matches) {
            return PinStateUpdate(
                result = PinVerificationResult.Success,
                updatedAttemptState = PinAttemptState(),
            )
        }

        val rateLimit = pinRateLimiter.recordFailure(attemptState.failedAttempts, nowMillis)
        val updatedAttemptState =
            PinAttemptState(
                failedAttempts = rateLimit.failedAttempts,
                lockoutUntil = rateLimit.lockoutUntil,
            )
        val result =
            if (rateLimit.lockoutUntil != null) {
                PinVerificationResult.Locked(rateLimit.lockoutUntil)
            } else {
                PinVerificationResult.Failure(
                    remainingAttempts =
                        pinRateLimiter.remainingAttemptsBeforeLockout(
                            rateLimit.failedAttempts,
                        ),
                )
            }
        return PinStateUpdate(
            result = result,
            updatedAttemptState = updatedAttemptState,
        )
    }
}
