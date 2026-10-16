package com.shortsblockerkids.application.protection

import com.shortsblockerkids.application.port.PinLockoutClock
import com.shortsblockerkids.application.port.PinLockoutTime
import com.shortsblockerkids.application.port.TemporaryAllowStore
import com.shortsblockerkids.application.port.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearExpiredTemporaryAllowUseCaseTest {
    @Test
    fun futureElapsedDeadlineSurvivesForwardWallClockChange() =
        runBlocking {
            val store = InMemoryTemporaryAllowStore(2_001L, elapsedUntilMillis = 2_001L, bootCount = 1)
            assertFalse(deadlineUseCase(store, nowMillis = 86_400_000L, elapsedMillis = 2_000L)())
            assertEquals(2_001L, store.allowUntilMillis)
            assertEquals(2_001L, store.elapsedUntilMillis)
            assertEquals(1, store.bootCount)
            assertEquals(0, store.mutationCount)
        }

    @Test
    fun exactElapsedBoundaryExpiresDespiteWallClockRollback() =
        runBlocking {
            val store = InMemoryTemporaryAllowStore(86_400_000L, elapsedUntilMillis = 2_000L, bootCount = 1)
            assertTrue(deadlineUseCase(store, nowMillis = 1_000L, elapsedMillis = 2_000L)())
            assertDeadlineRemoved(store)
        }

    @Test
    fun rebootInvalidatesPersistedTemporaryAllowance() =
        runBlocking {
            val store = InMemoryTemporaryAllowStore(5_000L, elapsedUntilMillis = 5_000L, bootCount = 1)
            assertTrue(deadlineUseCase(store, nowMillis = 1_000L, elapsedMillis = 1_000L, bootCount = 2)())
            assertDeadlineRemoved(store)
        }

    @Test
    fun missingElapsedMetadataCannotExtendLegacyAllowance() =
        runBlocking {
            val store = InMemoryTemporaryAllowStore(5_000L, bootCount = 1)
            assertTrue(deadlineUseCase(store, nowMillis = 1_000L, elapsedMillis = 1_000L)())
            assertDeadlineRemoved(store)
        }

    @Test
    fun missingBootMetadataCannotAuthorizePersistedElapsedDeadline() =
        runBlocking {
            val store = InMemoryTemporaryAllowStore(5_000L, elapsedUntilMillis = 5_000L)
            assertTrue(deadlineUseCase(store, nowMillis = 1_000L, elapsedMillis = 1_000L)())
            assertDeadlineRemoved(store)
        }

    @Test
    fun missingAllowIsNotRemovedOrMutated() =
        runBlocking {
            val timeProvider = MutableTimeProvider(nowMillis = 2_000L)
            val temporaryAllowStore = InMemoryTemporaryAllowStore(allowUntilMillis = null)
            val useCase = createUseCase(timeProvider, temporaryAllowStore)

            assertFalse(useCase())

            assertNull(temporaryAllowStore.allowUntilMillis)
            assertEquals(0, temporaryAllowStore.mutationCount)
            assertEquals(1, timeProvider.readCount)
        }

    @Test
    fun activeAllowIsNotRemovedOrMutated() =
        runBlocking {
            val timeProvider = MutableTimeProvider(nowMillis = 2_000L)
            val temporaryAllowStore = InMemoryTemporaryAllowStore(allowUntilMillis = 2_001L)
            val useCase = createUseCase(timeProvider, temporaryAllowStore)

            assertFalse(useCase())

            assertEquals(2_001L, temporaryAllowStore.allowUntilMillis)
            assertEquals(0, temporaryAllowStore.mutationCount)
        }

    @Test
    fun allowAtExactExpiryBoundaryIsRemoved() =
        runBlocking {
            val timeProvider = MutableTimeProvider(nowMillis = 2_000L)
            val temporaryAllowStore = InMemoryTemporaryAllowStore(allowUntilMillis = 2_000L)
            val useCase = createUseCase(timeProvider, temporaryAllowStore)

            assertTrue(useCase())

            assertNull(temporaryAllowStore.allowUntilMillis)
            assertEquals(1, temporaryAllowStore.mutationCount)
        }

    @Test
    fun expiredAllowIsRemoved() =
        runBlocking {
            val timeProvider = MutableTimeProvider(nowMillis = 2_000L)
            val temporaryAllowStore = InMemoryTemporaryAllowStore(allowUntilMillis = 1_999L)
            val useCase = createUseCase(timeProvider, temporaryAllowStore)

            assertTrue(useCase())

            assertNull(temporaryAllowStore.allowUntilMillis)
            assertEquals(1, temporaryAllowStore.mutationCount)
        }

    @Test
    fun clockRollbackDoesNotRestoreRemovedAllow() =
        runBlocking {
            val timeProvider = MutableTimeProvider(nowMillis = 2_500L)
            val temporaryAllowStore = InMemoryTemporaryAllowStore(allowUntilMillis = 2_000L)
            val useCase = createUseCase(timeProvider, temporaryAllowStore)

            assertTrue(useCase())
            timeProvider.nowMillis = 1_500L
            assertFalse(useCase())

            assertNull(temporaryAllowStore.allowUntilMillis)
            assertEquals(1, temporaryAllowStore.mutationCount)
        }

    private fun deadlineUseCase(
        store: TemporaryAllowStore,
        nowMillis: Long,
        elapsedMillis: Long,
        bootCount: Int = 1,
    ): ClearExpiredTemporaryAllowUseCase =
        ClearExpiredTemporaryAllowUseCase(
            timeProvider = MutableTimeProvider(nowMillis),
            temporaryAllowStore = store,
            deadlineClock = PinLockoutClock { PinLockoutTime(elapsedMillis, bootCount) },
        )

    private fun assertDeadlineRemoved(store: InMemoryTemporaryAllowStore) {
        assertNull(store.allowUntilMillis)
        assertNull(store.elapsedUntilMillis)
        assertNull(store.bootCount)
        assertEquals(1, store.mutationCount)
    }

    private fun createUseCase(
        timeProvider: TimeProvider,
        temporaryAllowStore: TemporaryAllowStore,
    ): ClearExpiredTemporaryAllowUseCase =
        ClearExpiredTemporaryAllowUseCase(
            timeProvider = timeProvider,
            temporaryAllowStore = temporaryAllowStore,
        )

    private class MutableTimeProvider(
        var nowMillis: Long,
    ) : TimeProvider {
        var readCount: Int = 0
            private set

        override fun currentTimeMillis(): Long {
            readCount += 1
            return nowMillis
        }
    }

    private class InMemoryTemporaryAllowStore(
        allowUntilMillis: Long?,
        elapsedUntilMillis: Long? = null,
        bootCount: Int? = null,
    ) : TemporaryAllowStore {
        var allowUntilMillis: Long? = allowUntilMillis
            private set
        var elapsedUntilMillis: Long? = elapsedUntilMillis
            private set
        var bootCount: Int? = bootCount
            private set
        var mutationCount: Int = 0
            private set

        override suspend fun setTemporaryAllowUntil(allowUntilMillis: Long?) {
            this.allowUntilMillis = allowUntilMillis
            mutationCount += 1
        }

        override suspend fun removeTemporaryAllowIf(shouldRemove: (Long) -> Boolean): Boolean {
            val persistedAllowUntil = allowUntilMillis ?: return false
            if (!shouldRemove(persistedAllowUntil)) {
                return false
            }
            allowUntilMillis = null
            mutationCount += 1
            return true
        }

        override suspend fun removeTemporaryAllowDeadlineIf(shouldRemove: (Long?, Long?, Int?) -> Boolean): Boolean {
            if (allowUntilMillis == null && elapsedUntilMillis == null) return false
            if (!shouldRemove(allowUntilMillis, elapsedUntilMillis, bootCount)) return false
            allowUntilMillis = null
            elapsedUntilMillis = null
            bootCount = null
            mutationCount++
            return true
        }
    }
}
