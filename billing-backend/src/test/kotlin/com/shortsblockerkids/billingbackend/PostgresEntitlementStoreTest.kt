package com.shortsblockerkids.billingbackend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.nio.file.Path
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID

class PostgresEntitlementStoreTest {
    @Test
    fun duplicateClaimSkipsUpdateAcrossStoreInstances() {
        val database = TransactionDouble()

        assertEquals(RtdnApplyResult.UPDATED, store(database).applyRtdn("message", "token", verified()))
        assertEquals(RtdnApplyResult.DUPLICATE, store(database).applyRtdn("message", "token", verified()))

        assertEquals(2, database.connectionCount)
        assertEquals(1, database.committedUpdates)
        assertEquals(
            listOf("begin", "claim", "update", "commit", "close", "begin", "claim", "commit", "close"),
            database.events,
        )
    }

    @Test
    fun updateFailureRollsBackClaimAndAllowsRetry() {
        val database = TransactionDouble()
        database.failUpdate = true

        assertThrows(SQLException::class.java) { store(database).applyRtdn("message", "token", verified()) }

        assertFalse(database.committedMarker)
        assertEquals(0, database.committedUpdates)
        assertEquals(listOf("begin", "claim", "update", "rollback", "close"), database.events)
        database.failUpdate = false
        assertEquals(RtdnApplyResult.UPDATED, store(database).applyRtdn("message", "token", verified()))
        assertTrue(database.committedMarker)
        assertEquals(1, database.committedUpdates)
    }

    @Test
    fun commitFailureRollsBackEntitlementAndMarker() {
        val database = TransactionDouble()
        database.failCommit = true

        assertThrows(SQLException::class.java) { store(database).applyRtdn("message", "token", verified()) }

        assertFalse(database.committedMarker)
        assertEquals(0, database.committedUpdates)
        assertEquals(listOf("begin", "claim", "update", "commit", "rollback", "close"), database.events)
        database.failCommit = false
        assertEquals(RtdnApplyResult.UPDATED, store(database).applyRtdn("message", "token", verified()))
        assertTrue(database.committedMarker)
        assertEquals(1, database.committedUpdates)
    }

    @Test
    fun absentInstallCommitsMarkerWithoutEntitlementUpdate() {
        val database = TransactionDouble()
        database.updateCount = 0

        assertEquals(RtdnApplyResult.NO_INSTALL_RECORD, store(database).applyRtdn("message", "token", verified()))

        assertTrue(database.committedMarker)
        assertEquals(0, database.committedUpdates)
        assertEquals(RtdnApplyResult.DUPLICATE, store(database).applyRtdn("message", "token", verified()))
    }

    @Test
    fun rejectedUpdateForExistingInstallIsStaleAndCommitsMarker() {
        val database = TransactionDouble()
        database.updateCount = 0
        database.tokenExists = true

        assertEquals(RtdnApplyResult.STALE, store(database).applyRtdn("message", "token", verified()))

        assertTrue(database.committedMarker)
        assertEquals(0, database.committedUpdates)
        assertEquals(listOf("begin", "claim", "update", "find-token", "commit", "close"), database.events)
        assertEquals(RtdnApplyResult.DUPLICATE, store(database).applyRtdn("message", "token", verified()))
    }

    @Test
    fun realPostgresUpsertRejectsOlderAndEqualVerification() {
        val store = realPostgresStore()
        val initial = record("pg-upsert-${UUID.randomUUID()}")
        store.upsert(initial)

        listOf(4_000L, 5_000L).forEach { timestamp ->
            store.upsert(initial.copy(state = SubscriptionEntitlementState.EXPIRED, lastVerifiedAtMillis = timestamp))
            assertEquals(initial, store.findByInstallId(initial.installId))
        }
        val newer = initial.copy(state = SubscriptionEntitlementState.EXPIRED, lastVerifiedAtMillis = 6_000L)
        store.upsert(newer)
        assertEquals(newer, store.findByInstallId(initial.installId))
    }

    @Test
    fun realPostgresTokenUpdateRejectsOlderAndEqualVerification() {
        val store = realPostgresStore()
        val initial = record("pg-token-${UUID.randomUUID()}")
        store.upsert(initial)

        listOf(4_000L, 5_000L).forEach { timestamp ->
            assertFalse(store.updateByPurchaseToken(initial.installId, verified().copy(verifiedAtMillis = timestamp)))
            assertEquals(initial, store.findByInstallId(initial.installId))
        }
        assertTrue(store.updateByPurchaseToken(initial.installId, verified()))
        assertEquals(SubscriptionEntitlementState.EXPIRED, store.findByInstallId(initial.installId)?.state)
    }

    @Test
    fun realPostgresRtdnDistinguishesStaleFromMissingAndPersistsMarker() {
        val store = realPostgresStore()
        val initial = record("pg-rtdn-${UUID.randomUUID()}")
        store.upsert(initial)

        listOf(4_000L, 5_000L).forEach { timestamp ->
            val messageId = "${initial.installId}-$timestamp"
            assertEquals(
                RtdnApplyResult.STALE,
                store.processRtdn(messageId, initial.installId) { verified().copy(verifiedAtMillis = timestamp) },
            )
            val reloaded = realPostgresStore()
            assertEquals(initial, reloaded.findByInstallId(initial.installId))
            assertTrue(reloaded.isRtdnProcessed(messageId))
            assertEquals(RtdnApplyResult.DUPLICATE, reloaded.processRtdn(messageId, initial.installId) { error("Duplicate verified") })
        }
        assertEquals(RtdnApplyResult.UPDATED, store.applyRtdn("${initial.installId}-newer", initial.installId, verified()))
        assertEquals(SubscriptionEntitlementState.EXPIRED, store.findByInstallId(initial.installId)?.state)
        val missingMessageId = "${initial.installId}-missing"
        assertEquals(RtdnApplyResult.NO_INSTALL_RECORD, store.applyRtdn(missingMessageId, "absent-token", verified()))
        assertTrue(realPostgresStore().isRtdnProcessed(missingMessageId))
    }

    private fun realPostgresStore(): PostgresEntitlementStore {
        val url = System.getenv("SBK_TEST_POSTGRES_URL")
        assumeTrue("Set SBK_TEST_POSTGRES_URL to an isolated regression database", !url.isNullOrBlank())
        val database = DatabaseConfig(requireNotNull(url), null, null)
        DatabaseMigrator(database, Path.of("migrations")).applyMigrations()
        return PostgresEntitlementStore(database)
    }

    private fun record(installId: String): EntitlementRecord =
        EntitlementRecord(
            installId = installId,
            packageName = "com.shortsblockerkids",
            productId = "monthly",
            purchaseTokenHash = EntitlementStore.hashPurchaseToken(installId),
            state = SubscriptionEntitlementState.ACTIVE,
            activeUntilMillis = 10_000L,
            acknowledged = true,
            lastVerifiedAtMillis = 5_000L,
            appVersion = null,
        )

    private fun store(database: TransactionDouble): PostgresEntitlementStore =
        PostgresEntitlementStore(
            database = DatabaseConfig("jdbc:postgresql://unused/test", null, null),
            nowMillis = { 9_000L },
            connectionFactory = database::connect,
        )

    private fun verified(): VerifiedSubscription =
        VerifiedSubscription(
            productId = "monthly",
            purchaseToken = "token",
            state = SubscriptionEntitlementState.EXPIRED,
            activeUntilMillis = null,
            acknowledged = true,
            verifiedAtMillis = 6_000L,
        )

    private class TransactionDouble {
        val events = mutableListOf<String>()
        var connectionCount = 0
        var committedMarker = false
        var committedUpdates = 0
        var failUpdate = false
        var failCommit = false
        var updateCount = 1
        var tokenExists = false

        fun connect(): Connection {
            connectionCount++
            return Transaction().connection()
        }

        private inner class Transaction {
            private var started = false
            private var pendingMarker = false
            private var pendingUpdates = 0

            fun connection(): Connection = proxy(Connection::class.java, ::dispatch)

            private fun dispatch(
                method: String,
                arguments: Array<out Any?>,
            ): Any? =
                when (method) {
                    "setAutoCommit" -> begin(arguments[0])
                    "prepareStatement" -> statement(arguments[0] as String)
                    "commit" -> commit()
                    "rollback" -> rollback()
                    "close" -> close()
                    else -> error("Unexpected connection method: $method")
                }

            private fun begin(autoCommit: Any?): Any? {
                assertEquals(false, autoCommit)
                started = true
                events += "begin"
                return null
            }

            private fun statement(sql: String): PreparedStatement {
                assertTrue(started)
                return Statement(sql).statement()
            }

            private fun claim(
                sql: String,
                parameters: Map<Int, Any?>,
            ): Int {
                assertTrue(sql.contains("ON CONFLICT (message_id) DO NOTHING"))
                assertEquals("message", parameters[1])
                assertEquals(9_000L, parameters[2])
                events += "claim"
                if (committedMarker) return 0
                pendingMarker = true
                return 1
            }

            private fun update(
                sql: String,
                parameters: Map<Int, Any?>,
            ): Int {
                assertTrue(sql.contains("UPDATE entitlements"))
                assertTrue(sql.contains("last_verified_at_millis < ?"))
                assertTrue(pendingMarker)
                assertEquals("EXPIRED", parameters[1])
                assertEquals(null, parameters[2])
                assertEquals(true, parameters[3])
                assertEquals(6_000L, parameters[4])
                assertEquals(9_000L, parameters[5])
                assertEquals(EntitlementStore.hashPurchaseToken("token"), parameters[6])
                assertEquals(6_000L, parameters[7])
                events += "update"
                if (failUpdate) throw SQLException("Injected update failure")
                pendingUpdates += updateCount
                return updateCount
            }

            private fun commit(): Any? {
                events += "commit"
                if (failCommit) throw SQLException("Injected commit failure")
                committedMarker = committedMarker || pendingMarker
                committedUpdates += pendingUpdates
                pendingMarker = false
                pendingUpdates = 0
                return null
            }

            private fun rollback(): Any? {
                events += "rollback"
                pendingMarker = false
                pendingUpdates = 0
                return null
            }

            private fun close(): Any? {
                events += "close"
                assertFalse(pendingMarker)
                assertEquals(0, pendingUpdates)
                return null
            }

            private inner class Statement(
                private val sql: String,
            ) {
                private val parameters = mutableMapOf<Int, Any?>()

                fun statement(): PreparedStatement = proxy(PreparedStatement::class.java, ::dispatch)

                private fun dispatch(
                    method: String,
                    values: Array<out Any?>,
                ): Any? =
                    when (method) {
                        "setString", "setLong", "setBoolean", "setNull" -> bind(method, values)
                        "executeUpdate" -> execute()
                        "executeQuery" -> query()
                        "setQueryTimeout" -> {
                            assertEquals(15, values[0])
                            null
                        }
                        "close" -> null
                        else -> error("Unexpected statement method: $method")
                    }

                private fun query(): ResultSet {
                    assertTrue(sql.contains("SELECT 1 FROM entitlements WHERE purchase_token_hash = ?"))
                    assertEquals(EntitlementStore.hashPurchaseToken("token"), parameters[1])
                    events += "find-token"
                    return proxy(ResultSet::class.java) { method, _ ->
                        when (method) {
                            "next" -> tokenExists
                            "close" -> null
                            else -> error("Unexpected result set method: $method")
                        }
                    }
                }

                private fun bind(
                    method: String,
                    values: Array<out Any?>,
                ): Any? {
                    parameters[values[0] as Int] = if (method == "setNull") null else values[1]
                    return null
                }

                private fun execute(): Int =
                    if (sql.contains("INSERT INTO processed_rtdn_messages")) {
                        claim(sql, parameters)
                    } else {
                        update(sql, parameters)
                    }
            }
        }

        private fun <T> proxy(
            type: Class<T>,
            invoke: (String, Array<out Any?>) -> Any?,
        ): T =
            type.cast(
                Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, arguments ->
                    invoke(method.name, arguments ?: emptyArray())
                },
            )
    }
}
