package com.shortsblockerkids.application.pin

import com.shortsblockerkids.application.model.PinAttemptState
import com.shortsblockerkids.application.model.PinCredential
import com.shortsblockerkids.application.port.PinHashingPort
import com.shortsblockerkids.application.port.PinLockoutClock
import com.shortsblockerkids.application.port.PinLockoutTime
import com.shortsblockerkids.application.port.PinStateStore
import com.shortsblockerkids.application.port.PinStateUpdate
import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.domain.pin.PinPolicy
import com.shortsblockerkids.domain.pin.PinRateLimiter

class VerifyPinUseCase(
    private val pinStateStore: PinStateStore,
    private val pinHasher: PinHashingPort,
    private val timeProvider: TimeProvider,
    private val lockoutClock: PinLockoutClock,
    private val pinRateLimiter: PinRateLimiter = PinRateLimiter(),
) {
    suspend operator fun invoke(pin: String): PinVerificationResult =
        pinStateStore.verifyAndUpdateAtomically { credential, attemptState ->
            verificationUpdate(pin, credential, attemptState, timeProvider.currentTimeMillis(), lockoutClock.read())
        }

    private fun verificationUpdate(
        pin: String,
        credential: PinCredential?,
        attemptState: PinAttemptState,
        nowMillis: Long,
        lockoutTime: PinLockoutTime,
    ): PinStateUpdate {
        if (!PinPolicy.isVerificationInputComplete(pin)) {
            return PinStateUpdate(PinVerificationResult.InvalidInput)
        }
        if (credential == null) {
            return PinStateUpdate(PinVerificationResult.NotConfigured)
        }
        activeLockout(attemptState, nowMillis, lockoutTime)?.let { update ->
            return update
        }

        val matches =
            verifyCredential(pin, credential)
                ?: return PinStateUpdate(PinVerificationResult.NotConfigured)
        if (matches) {
            return PinStateUpdate(
                result = PinVerificationResult.Success,
                updatedAttemptState = PinAttemptState(),
            )
        }
        return failedAttemptUpdate(attemptState, nowMillis, lockoutTime)
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
        lockoutTime: PinLockoutTime,
    ): PinStateUpdate? {
        val elapsedDeadline = attemptState.lockoutElapsedRealtimeUntil
        if (attemptState.lockoutUntil == null && elapsedDeadline == null) {
            return null
        }
        if (elapsedDeadline == null || attemptState.lockoutBootCount != lockoutTime.bootCount) {
            // After a reboot or legacy upgrade, restart the bounded delay once without trusting wall time.
            val duration = pinRateLimiter.lockoutDurationMillis(attemptState.failedAttempts) ?: return null
            return lockoutUpdate(attemptState.failedAttempts, duration, nowMillis, lockoutTime)
        }
        val remainingMillis = elapsedDeadline - lockoutTime.elapsedRealtimeMillis
        return remainingMillis.takeIf { it > 0L }?.let {
            PinStateUpdate(lockedResult(nowMillis, it))
        }
    }

    private fun failedAttemptUpdate(
        attemptState: PinAttemptState,
        nowMillis: Long,
        lockoutTime: PinLockoutTime,
    ): PinStateUpdate {
        val rateLimit = pinRateLimiter.recordFailure(attemptState.failedAttempts, nowMillis)
        rateLimit.lockoutUntil?.let { until ->
            return lockoutUpdate(rateLimit.failedAttempts, until - nowMillis, nowMillis, lockoutTime)
        }
        return PinStateUpdate(
            result =
                PinVerificationResult.Failure(
                    pinRateLimiter.remainingAttemptsBeforeLockout(rateLimit.failedAttempts),
                ),
            updatedAttemptState = PinAttemptState(failedAttempts = rateLimit.failedAttempts),
        )
    }

    private fun lockoutUpdate(
        failedAttempts: Int,
        durationMillis: Long,
        nowMillis: Long,
        lockoutTime: PinLockoutTime,
    ): PinStateUpdate =
        PinStateUpdate(
            result = lockedResult(nowMillis, durationMillis),
            updatedAttemptState =
                PinAttemptState(
                    failedAttempts = failedAttempts,
                    lockoutUntil = nowMillis + durationMillis,
                    lockoutElapsedRealtimeUntil = lockoutTime.elapsedRealtimeMillis + durationMillis,
                    lockoutBootCount = lockoutTime.bootCount,
                ),
        )

    private fun lockedResult(
        nowMillis: Long,
        remainingMillis: Long,
    ): PinVerificationResult.Locked =
        PinVerificationResult.Locked(
            untilMillis = nowMillis + remainingMillis,
            remainingMillis = remainingMillis,
        )
}
