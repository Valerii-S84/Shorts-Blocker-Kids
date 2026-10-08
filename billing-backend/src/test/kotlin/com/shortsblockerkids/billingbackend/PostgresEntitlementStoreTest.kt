package com.shortsblockerkids.billingbackend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

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
                assertTrue(pendingMarker)
                assertEquals("EXPIRED", parameters[1])
                assertEquals(null, parameters[2])
                assertEquals(true, parameters[3])
                assertEquals(6_000L, parameters[4])
                assertEquals(9_000L, parameters[5])
                assertEquals(EntitlementStore.hashPurchaseToken("token"), parameters[6])
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
                        "close" -> null
                        else -> error("Unexpected statement method: $method")
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
