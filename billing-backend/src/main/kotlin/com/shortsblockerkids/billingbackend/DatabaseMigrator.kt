package com.shortsblockerkids.billingbackend

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

object EntitlementStorageFactory {
    fun create(config: BackendConfig): EntitlementStorage {
        val database = config.database ?: return EntitlementStore(requireNotNull(config.storeFile))
        if (config.runMigrations) {
            DatabaseMigrator(database, config.migrationsDir).applyMigrations()
        }
        return PostgresEntitlementStore(database)
    }
}

class DatabaseMigrator(
    private val database: DatabaseConfig,
    private val migrationsDir: Path,
) {
    fun applyMigrations() {
        openDatabaseConnection(database).use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { it.execute(SCHEMA_MIGRATIONS_SQL) }
                val applied = appliedVersions(connection)
                migrationFiles().forEach { file ->
                    val version = file.fileName.toString().substringBefore("_")
                    if (version !in applied) {
                        applyMigration(connection, file, version)
                    }
                }
                connection.commit()
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            }
        }
    }

    private fun migrationFiles(): List<Path> {
        if (!Files.isDirectory(migrationsDir)) {
            throw ConfigurationException(listOf("Migration directory not found: $migrationsDir"))
        }
        return Files
            .list(migrationsDir)
            .use { paths ->
                paths
                    .filter { it.fileName.toString().matches(MIGRATION_FILE_REGEX) }
                    .sorted()
                    .toList()
            }
    }

    private fun appliedVersions(connection: Connection): Set<String> =
        connection
            .prepareStatement("SELECT version FROM schema_migrations")
            .use { statement ->
                statement.executeQuery().use { rows ->
                    buildSet {
                        while (rows.next()) {
                            add(rows.getString("version"))
                        }
                    }
                }
            }

    private fun applyMigration(
        connection: Connection,
        file: Path,
        version: String,
    ) {
        connection.createStatement().use { statement ->
            splitSql(Files.readString(file)).forEach(statement::execute)
        }
        connection
            .prepareStatement("INSERT INTO schema_migrations(version, applied_at_millis) VALUES (?, ?)")
            .use { statement ->
                statement.setString(1, version)
                statement.setLong(2, System.currentTimeMillis())
                statement.executeUpdate()
            }
    }

    private fun splitSql(sql: String): List<String> =
        sql
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private companion object {
        val MIGRATION_FILE_REGEX = Regex("\\d{3}_.+\\.sql")

        const val SCHEMA_MIGRATIONS_SQL =
            """
            CREATE TABLE IF NOT EXISTS schema_migrations (
                version TEXT PRIMARY KEY,
                applied_at_millis BIGINT NOT NULL
            )
            """
    }
}

class PostgresEntitlementStore(
    private val database: DatabaseConfig,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : EntitlementStorage {
    override fun upsert(record: EntitlementRecord) {
        openDatabaseConnection(database).use { connection ->
            connection.prepareStatement(UPSERT_ENTITLEMENT_SQL).use { statement ->
                statement.bindRecord(record, nowMillis())
                statement.executeUpdate()
            }
        }
    }

    override fun findByInstallId(installId: String): EntitlementRecord? = findOne("install_id = ?", installId)

    override fun findByPurchaseTokenHash(purchaseTokenHash: String): EntitlementRecord? =
        findOne("purchase_token_hash = ?", purchaseTokenHash)

    override fun markRtdnProcessed(messageId: String): Boolean =
        openDatabaseConnection(database).use { connection ->
            connection
                .prepareStatement(
                    """
                    INSERT INTO processed_rtdn_messages(message_id, processed_at_millis)
                    VALUES (?, ?)
                    ON CONFLICT (message_id) DO NOTHING
                    """,
                ).use { statement ->
                    statement.setString(1, messageId)
                    statement.setLong(2, nowMillis())
                    statement.executeUpdate() == 1
                }
        }

    override fun isRtdnProcessed(messageId: String): Boolean =
        openDatabaseConnection(database).use { connection ->
            connection
                .prepareStatement("SELECT 1 FROM processed_rtdn_messages WHERE message_id = ?")
                .use { statement ->
                    statement.setString(1, messageId)
                    statement.executeQuery().use(ResultSet::next)
                }
        }

    override fun updateByPurchaseToken(
        purchaseToken: String,
        verified: VerifiedSubscription,
    ): Boolean =
        openDatabaseConnection(database).use { connection ->
            connection.prepareStatement(UPDATE_BY_TOKEN_SQL).use { statement ->
                statement.setString(1, verified.state.name)
                statement.setNullableLong(2, verified.activeUntilMillis)
                statement.setBoolean(3, verified.acknowledged)
                statement.setLong(4, verified.verifiedAtMillis)
                statement.setLong(5, nowMillis())
                statement.setString(6, EntitlementStore.hashPurchaseToken(purchaseToken))
                statement.executeUpdate() > 0
            }
        }

    private fun findOne(
        whereSql: String,
        value: String,
    ): EntitlementRecord? =
        openDatabaseConnection(database).use { connection ->
            connection
                .prepareStatement(
                    """
                    SELECT install_id, package_name, product_id, purchase_token_hash, state,
                        active_until_millis, acknowledged, last_verified_at_millis, app_version
                    FROM entitlements
                    WHERE $whereSql
                    """,
                ).use { statement ->
                    statement.setString(1, value)
                    statement.executeQuery().use { rows ->
                        if (rows.next()) rows.toEntitlementRecord() else null
                    }
                }
        }

    private companion object {
        const val UPSERT_ENTITLEMENT_SQL =
            """
            INSERT INTO entitlements(
                install_id, package_name, product_id, purchase_token_hash, state,
                active_until_millis, acknowledged, last_verified_at_millis, app_version,
                created_at_millis, updated_at_millis
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (install_id) DO UPDATE SET
                package_name = EXCLUDED.package_name,
                product_id = EXCLUDED.product_id,
                purchase_token_hash = EXCLUDED.purchase_token_hash,
                state = EXCLUDED.state,
                active_until_millis = EXCLUDED.active_until_millis,
                acknowledged = EXCLUDED.acknowledged,
                last_verified_at_millis = EXCLUDED.last_verified_at_millis,
                app_version = EXCLUDED.app_version,
                updated_at_millis = EXCLUDED.updated_at_millis
            """

        const val UPDATE_BY_TOKEN_SQL =
            """
            UPDATE entitlements
            SET state = ?,
                active_until_millis = ?,
                acknowledged = ?,
                last_verified_at_millis = ?,
                updated_at_millis = ?
            WHERE purchase_token_hash = ?
            """
    }
}

private fun openDatabaseConnection(database: DatabaseConfig): Connection =
    if (database.user == null && database.password == null) {
        DriverManager.getConnection(database.url)
    } else {
        DriverManager.getConnection(database.url, database.user, database.password)
    }

private fun java.sql.PreparedStatement.bindRecord(
    record: EntitlementRecord,
    nowMillis: Long,
) {
    setString(1, record.installId)
    setString(2, record.packageName)
    setString(3, record.productId)
    setString(4, record.purchaseTokenHash)
    setString(5, record.state.name)
    setNullableLong(6, record.activeUntilMillis)
    setBoolean(7, record.acknowledged)
    setLong(8, record.lastVerifiedAtMillis)
    setString(9, record.appVersion)
    setLong(10, nowMillis)
    setLong(11, nowMillis)
}

private fun java.sql.PreparedStatement.setNullableLong(
    index: Int,
    value: Long?,
) {
    if (value == null) {
        setNull(index, java.sql.Types.BIGINT)
    } else {
        setLong(index, value)
    }
}

private fun ResultSet.toEntitlementRecord(): EntitlementRecord =
    EntitlementRecord(
        installId = getString("install_id"),
        packageName = getString("package_name"),
        productId = getString("product_id"),
        purchaseTokenHash = getString("purchase_token_hash"),
        state = SubscriptionEntitlementState.valueOf(getString("state")),
        activeUntilMillis = getNullableLong("active_until_millis"),
        acknowledged = getBoolean("acknowledged"),
        lastVerifiedAtMillis = getLong("last_verified_at_millis"),
        appVersion = getString("app_version"),
    )

private fun ResultSet.getNullableLong(column: String): Long? {
    val value = getLong(column)
    return if (wasNull()) null else value
}
