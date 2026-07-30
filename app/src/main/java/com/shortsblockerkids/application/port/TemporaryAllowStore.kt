package com.shortsblockerkids.application.port

interface TemporaryAllowStore {
    suspend fun setTemporaryAllowUntil(allowUntilMillis: Long?)

    suspend fun removeTemporaryAllowIf(shouldRemove: (Long) -> Boolean): Boolean
}
