package com.shortsblockerkids.application.protection

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
    ) : TemporaryAllowStore {
        var allowUntilMillis: Long? = allowUntilMillis
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
    }
}
