package com.shortsblockerkids.feature.blocking

class TemporaryAllowFlowController(
    private val storeTemporaryAllow: suspend (Int) -> Unit,
) {
    suspend fun selectDuration(minutes: Int): TemporaryAllowCompletion {
        storeTemporaryAllow(minutes)
        return TemporaryAllowCompletion.ReturnToForegroundApp
    }

    fun cancel(): TemporaryAllowCompletion = TemporaryAllowCompletion.ReturnToForegroundApp
}

enum class TemporaryAllowCompletion {
    ReturnToForegroundApp,
}
