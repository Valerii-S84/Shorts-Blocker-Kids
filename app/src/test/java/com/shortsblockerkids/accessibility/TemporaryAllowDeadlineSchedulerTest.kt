package com.shortsblockerkids.accessibility

import com.shortsblockerkids.application.port.TimeProvider
import com.shortsblockerkids.platform.accessibility.TemporaryAllowDeadlineScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class TemporaryAllowDeadlineSchedulerTest {
    @Test
    fun deadlineRechecksWindowWithoutAnotherEventOrSettingsEmission() = runBlocking {
        val fixture = Fixture(this)
        fixture.scheduler.schedule(2_000L)
        yield()
        assertEquals(listOf(1_000L), fixture.delays)
        assertEquals(0, fixture.rechecks)

        fixture.nowMillis = 2_000L
        fixture.wake()
        yield()

        assertEquals(1, fixture.rechecks)
    }

    @Test
    fun extendingAllowanceCancelsOldDeadlineAndUsesNewDeadline() = runBlocking {
        val fixture = Fixture(this)
        fixture.scheduler.schedule(2_000L)
        yield()
        val oldWake = fixture.pending.last()
        fixture.scheduler.schedule(3_000L)
        yield()

        fixture.nowMillis = 2_000L
        oldWake.complete(Unit)
        yield()
        assertEquals(0, fixture.rechecks)
        assertEquals(listOf(1_000L, 2_000L), fixture.delays)

        fixture.nowMillis = 3_000L
        fixture.wake()
        yield()
        assertEquals(1, fixture.rechecks)
    }

    @Test
    fun shorteningAllowanceSchedulesEarlierDeadline() = runBlocking {
        val fixture = Fixture(this)
        fixture.scheduler.schedule(3_000L)
        yield()
        fixture.scheduler.schedule(1_500L)
        yield()
        assertEquals(listOf(2_000L, 500L), fixture.delays)

        fixture.nowMillis = 1_500L
        fixture.wake()
        yield()
        assertEquals(1, fixture.rechecks)
    }

    @Test
    fun removedAllowanceCancelsPendingRecheck() = runBlocking {
        val fixture = Fixture(this)
        fixture.scheduler.schedule(2_000L)
        yield()
        fixture.scheduler.schedule(null)
        fixture.nowMillis = 2_000L
        fixture.wake()
        yield()

        assertEquals(0, fixture.rechecks)
        assertEquals(listOf(1_000L), fixture.delays)
    }

    @Test
    fun backwardClockChangeRearmsUntilPersistedDeadline() = runBlocking {
        val fixture = Fixture(this)
        fixture.scheduler.schedule(2_000L)
        yield()
        fixture.nowMillis = 500L
        fixture.wake()
        yield()

        assertEquals(0, fixture.rechecks)
        assertEquals(listOf(1_000L, 1_500L), fixture.delays)

        fixture.nowMillis = 2_000L
        fixture.wake()
        yield()
        assertEquals(1, fixture.rechecks)
    }

    @Test
    fun lifecycleCancellationPreventsDeadlineCallback() = runBlocking {
        val fixture = Fixture(this)
        fixture.scheduler.schedule(2_000L)
        yield()
        fixture.scheduler.cancel()
        fixture.nowMillis = 2_000L
        fixture.wake()
        yield()

        assertEquals(0, fixture.rechecks)
    }

    @Test
    fun alreadyExpiredDeadlineRechecksImmediatelyWithoutDelay() = runBlocking {
        val fixture = Fixture(this)
        fixture.scheduler.schedule(1_000L)
        yield()

        assertEquals(1, fixture.rechecks)
        assertEquals(emptyList<Long>(), fixture.delays)
    }

    private class Fixture(scope: CoroutineScope) {
        var nowMillis = 1_000L
        var rechecks = 0
        val delays = mutableListOf<Long>()
        val pending = mutableListOf<CompletableDeferred<Unit>>()
        val scheduler =
            TemporaryAllowDeadlineScheduler(
                scope = scope,
                timeProvider = TimeProvider { nowMillis },
                onDeadline = { rechecks++ },
                awaitDelay = { millis ->
                    delays += millis
                    val wake = CompletableDeferred<Unit>()
                    pending += wake
                    wake.await()
                },
            )

        fun wake() {
            pending.last().complete(Unit)
        }
    }
}
