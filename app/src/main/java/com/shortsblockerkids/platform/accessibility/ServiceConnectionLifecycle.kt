package com.shortsblockerkids.platform.accessibility

/** Owns the cleanup of one service connection, releasing it before replacement. */
internal class ServiceConnectionLifecycle {
    private var cleanup: (() -> Unit)? = null

    fun replace(start: () -> (() -> Unit)) {
        close()
        cleanup = start()
    }

    fun close() {
        val previousCleanup = cleanup
        cleanup = null
        previousCleanup?.invoke()
    }
}
