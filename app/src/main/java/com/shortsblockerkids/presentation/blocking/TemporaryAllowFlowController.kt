package com.shortsblockerkids.presentation.blocking

import com.shortsblockerkids.application.protection.SetTemporaryAllowUseCase
import com.shortsblockerkids.domain.protection.TemporaryAllowDuration

class TemporaryAllowFlowController(
    private val setTemporaryAllowUseCase: SetTemporaryAllowUseCase,
) {
    suspend fun selectDuration(duration: TemporaryAllowDuration): TemporaryAllowCompletion {
        setTemporaryAllowUseCase(duration)
        return TemporaryAllowCompletion.ReturnToForegroundApp
    }

    fun cancel(): TemporaryAllowCompletion = TemporaryAllowCompletion.ReturnToForegroundApp
}

enum class TemporaryAllowCompletion {
    ReturnToForegroundApp,
}
