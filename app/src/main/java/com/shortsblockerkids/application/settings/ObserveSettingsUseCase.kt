package com.shortsblockerkids.application.settings

import com.shortsblockerkids.application.model.AppSettingsSnapshot
import com.shortsblockerkids.application.port.SettingsStatePort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

sealed interface SettingsReadState {
    data object Loading : SettingsReadState

    data class Ready(val snapshot: AppSettingsSnapshot) : SettingsReadState

    // This snapshot belongs only to this observer session. It is not a successful read,
    // must not unlock settings/PIN UI, and must never be persisted as a fallback.
    data class Unavailable(
        val lastSuccessfulSnapshot: AppSettingsSnapshot?,
        val retryScheduled: Boolean,
    ) : SettingsReadState
}

class ObserveSettingsUseCase(
    private val settingsStatePort: SettingsStatePort,
    private val initialReadTimeoutMillis: Long = 2_000L,
    private val retryDelaysMillis: List<Long> = listOf(250L, 1_000L),
    private val recoveryDelayMillis: Long = 30_000L,
) {
    init {
        require(initialReadTimeoutMillis > 0L)
        require(retryDelaysMillis.all { it > 0L })
        require(recoveryDelayMillis > 0L)
    }

    operator fun invoke(): Flow<SettingsReadState> = channelFlow {
        send(SettingsReadState.Loading)
        var lastSuccessfulSnapshot: AppSettingsSnapshot? = null
        while (true) {
            for (attempt in 0..retryDelaysMillis.size) {
                try {
                    supervisorScope {
                        val settings = settingsStatePort.readSettings().produceIn(this)
                        try {
                            // Only the initial read is timed out. A healthy DataStore can remain
                            // idle indefinitely between changes without being a storage failure.
                            val initial = withTimeout(initialReadTimeoutMillis) { settings.receive() }
                            lastSuccessfulSnapshot = initial
                            send(SettingsReadState.Ready(initial))
                            for (snapshot in settings) {
                                lastSuccessfulSnapshot = snapshot
                                send(SettingsReadState.Ready(snapshot))
                            }
                        } finally {
                            settings.cancel()
                        }
                    }
                    return@channelFlow
                } catch (_: TimeoutCancellationException) {
                    // This attempt exceeded its initial-read deadline; retry is bounded below.
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Do not expose storage details or manufacture a successful default snapshot.
                }
                val retryScheduled = attempt < retryDelaysMillis.size
                send(SettingsReadState.Unavailable(lastSuccessfulSnapshot, retryScheduled))
                if (retryScheduled) delay(retryDelaysMillis[attempt])
            }
            // Short retries are exhausted. Keep the failure visible and allow explicit UI
            // retry while the service waits for a slow, cancellable recovery cycle.
            delay(recoveryDelayMillis)
        }
    }
}
