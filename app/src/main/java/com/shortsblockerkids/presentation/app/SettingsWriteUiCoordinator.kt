package com.shortsblockerkids.presentation.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SettingsWriteUiCoordinator(
    private val onFailure: () -> Unit,
) {
    private val mutex = Mutex()

    suspend fun run(operation: suspend () -> Unit): Boolean =
        mutex.withLock {
            try {
                operation()
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Callers put success/navigation callbacks after the storage operation.
                // A failed mutation must not invoke them or be replayed automatically.
                onFailure()
                false
            }
        }
}
