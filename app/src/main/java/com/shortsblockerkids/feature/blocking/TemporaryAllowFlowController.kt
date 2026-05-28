package com.shortsblockerkids.feature.blocking

class TemporaryAllowFlowController(
    private val storeTemporaryAllow: suspend (Int) -> Unit,
) {
    suspend fun selectDuration(minutes: Int): TemporaryAllowCompletion {
        require(minutes in ALLOWED_DURATIONS) {
            "Unsupported temporary allow duration: $minutes"
        }
        storeTemporaryAllow(minutes)
        return TemporaryAllowCompletion.ReturnToForegroundApp
    }

    fun cancel(): TemporaryAllowCompletion = TemporaryAllowCompletion.ReturnToForegroundApp

    companion object {
        val ALLOWED_DURATIONS = listOf(5, 10, 15)
    }
}

enum class TemporaryAllowCompletion {
    ReturnToForegroundApp,
}
