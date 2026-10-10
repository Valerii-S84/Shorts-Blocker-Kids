package com.shortsblockerkids.presentation.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shortsblockerkids.application.pin.PinVerificationResult
import com.shortsblockerkids.domain.pin.PinValidationResult

/** Activity-owned authorization; saved navigation state never grants write access. */
class ParentalSettingsSession(
    private val createPin: suspend (String, String) -> PinValidationResult,
    private val verifyPin: suspend (String) -> PinVerificationResult,
    private val writePlatform: suspend (String, Boolean) -> Unit,
) {
    var isAuthorized by mutableStateOf(false)
        private set
    private var generation = 0L

    fun lock() {
        generation += 1
        isAuthorized = false
    }

    suspend fun createPin(pin: String, confirmation: String): PinValidationResult {
        val attemptGeneration = generation
        val result = createPin.invoke(pin, confirmation)
        if (result == PinValidationResult.Valid && attemptGeneration == generation) {
            isAuthorized = true
        }
        return result
    }

    suspend fun verifyPin(pin: String): PinVerificationResult {
        lock()
        val attemptGeneration = generation
        val result = verifyPin.invoke(pin)
        if (attemptGeneration != generation) return PinVerificationResult.InvalidInput
        isAuthorized = result == PinVerificationResult.Success
        return result
    }

    suspend fun setPlatformEnabled(platformId: String, enabled: Boolean) {
        runAuthorized { writePlatform(platformId, enabled) }
    }

    suspend fun <T> runAuthorized(operation: suspend () -> T): T {
        check(isAuthorized) { "Parental PIN authorization required" }
        return operation()
    }
}
