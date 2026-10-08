package com.shortsblockerkids.application.port

interface TemporaryAllowStore {
    suspend fun setTemporaryAllowUntil(allowUntilMillis: Long?)

    suspend fun setTemporaryAllowDeadline(
        allowUntilMillis: Long,
        elapsedRealtimeDeadlineMillis: Long,
        bootCount: Int,
    ) = setTemporaryAllowUntil(allowUntilMillis)

    suspend fun removeTemporaryAllowDeadlineIf(shouldRemove: (Long?, Long?, Int?) -> Boolean): Boolean =
        removeTemporaryAllowIf { shouldRemove(it, null, null) }

    suspend fun removeTemporaryAllowIf(shouldRemove: (Long) -> Boolean): Boolean
}
