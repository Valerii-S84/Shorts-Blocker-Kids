package com.shortsblockerkids.billingbackend

import java.nio.file.Files
import java.nio.file.Path

data class BackendConfig(
    val environment: BackendEnvironment = BackendEnvironment.DEVELOPMENT,
    val bindAddress: String = "0.0.0.0",
    val port: Int = 8080,
    val publicBaseUrl: String? = null,
    val requireHttps: Boolean = false,
    val packageName: String = "com.shortsblockerkids",
    val productId: String = "shorts_blocker_kids_monthly",
    val storeFile: Path? = Path.of("billing-backend-data.json"),
    val database: DatabaseConfig? = null,
    val runMigrations: Boolean = true,
    val migrationsDir: Path = Path.of("billing-backend/migrations"),
    val rtdnSharedSecret: String? = null,
    val rtdnPubSubAudience: String? = null,
    val rtdnPubSubServiceAccountEmail: String? = null,
    val maxRequestBytes: Int = 32_768,
    val rateLimits: RateLimitConfig = RateLimitConfig(),
    val googleCredentialsFile: Path? = null,
) {
    val storageBackend: StorageBackend
        get() = if (database == null) StorageBackend.FILE else StorageBackend.POSTGRES

    companion object {
        fun fromEnvironment(env: Map<String, String>): BackendConfig {
            val environment = env.enumValue("SBK_ENV", BackendEnvironment.DEVELOPMENT)
            val config =
                BackendConfig(
                    environment = environment,
                    bindAddress = env.stringValue("SBK_BACKEND_BIND_ADDRESS", "0.0.0.0"),
                    port = env.intValue("SBK_BACKEND_PORT", 8080, 1..65_535),
                    publicBaseUrl = env.optionalString("SBK_PUBLIC_BASE_URL"),
                    requireHttps = env.booleanValue("SBK_REQUIRE_HTTPS", environment == BackendEnvironment.PRODUCTION),
                    packageName = env.stringValue("SBK_PACKAGE_NAME", "com.shortsblockerkids"),
                    productId = env.stringValue("SBK_PLAY_SUBSCRIPTION_PRODUCT_ID", "shorts_blocker_kids_monthly"),
                    storeFile = env.storeFile(),
                    database = env.databaseConfig(),
                    runMigrations = env.booleanValue("SBK_RUN_MIGRATIONS", true),
                    migrationsDir = Path.of(env.stringValue("SBK_MIGRATIONS_DIR", "billing-backend/migrations")),
                    rtdnSharedSecret = env.optionalString("SBK_RTDN_SHARED_SECRET"),
                    rtdnPubSubAudience = env.optionalString("SBK_RTDN_PUBSUB_AUDIENCE"),
                    rtdnPubSubServiceAccountEmail = env.optionalString("SBK_RTDN_PUBSUB_SERVICE_ACCOUNT_EMAIL"),
                    maxRequestBytes = env.intValue("SBK_MAX_REQUEST_BYTES", 32_768, 1..262_144),
                    rateLimits = RateLimitConfig.fromEnvironment(env),
                    googleCredentialsFile = env.optionalPath("GOOGLE_APPLICATION_CREDENTIALS"),
                )
            config.validate()
            return config
        }
    }
}

data class DatabaseConfig(
    val url: String,
    val user: String?,
    val password: String?,
)

data class RateLimitConfig(
    val verifyPerMinute: Int = 30,
    val statusPerMinute: Int = 120,
    val rtdnPerMinute: Int = 300,
) {
    companion object {
        fun fromEnvironment(env: Map<String, String>): RateLimitConfig =
            RateLimitConfig(
                verifyPerMinute = env.intValue("SBK_RATE_LIMIT_VERIFY_PER_MINUTE", 30, 1..10_000),
                statusPerMinute = env.intValue("SBK_RATE_LIMIT_STATUS_PER_MINUTE", 120, 1..10_000),
                rtdnPerMinute = env.intValue("SBK_RATE_LIMIT_RTDN_PER_MINUTE", 300, 1..50_000),
            )
    }
}

enum class BackendEnvironment {
    DEVELOPMENT,
    TEST,
    PRODUCTION,
}

enum class StorageBackend {
    FILE,
    POSTGRES,
}

class ConfigurationException(
    val errors: List<String>,
) : IllegalArgumentException(errors.joinToString("; "))

private fun BackendConfig.validate() {
    val errors = mutableListOf<String>()
    if (!packageName.matches(PACKAGE_NAME_REGEX)) {
        errors += "SBK_PACKAGE_NAME must be an Android package name"
    }
    if (!productId.matches(PRODUCT_ID_REGEX)) {
        errors += "SBK_PLAY_SUBSCRIPTION_PRODUCT_ID contains unsupported characters"
    }
    publicBaseUrl?.let {
        if (environment == BackendEnvironment.PRODUCTION && !it.startsWith("https://")) {
            errors += "SBK_PUBLIC_BASE_URL must use https:// in production"
        }
    }
    if (database == null && storeFile == null) {
        errors += "Either SBK_DATABASE_URL or SBK_BACKEND_STORE_FILE must be configured"
    }
    if (database != null && !database.url.startsWith("jdbc:postgresql://")) {
        errors += "SBK_DATABASE_URL must be a jdbc:postgresql:// URL"
    }
    if (environment == BackendEnvironment.PRODUCTION) {
        validateProduction(errors)
    }
    if (errors.isNotEmpty()) {
        throw ConfigurationException(errors)
    }
}

private fun BackendConfig.validateProduction(errors: MutableList<String>) {
    if (database == null) {
        errors += "SBK_DATABASE_URL is required in production"
    }
    if (googleCredentialsFile == null || !Files.isRegularFile(googleCredentialsFile)) {
        errors += "GOOGLE_APPLICATION_CREDENTIALS must point to a runtime service-account file in production"
    }
    if (publicBaseUrl.isNullOrBlank()) {
        errors += "SBK_PUBLIC_BASE_URL is required in production"
    }
    if (!requireHttps) {
        errors += "SBK_REQUIRE_HTTPS must be true in production"
    }
    if (rtdnPubSubAudience.isNullOrBlank() || rtdnPubSubServiceAccountEmail.isNullOrBlank()) {
        errors += "Authenticated Pub/Sub RTDN push config is required in production"
    }
}

private fun Map<String, String>.storeFile(): Path? {
    if (optionalString("SBK_DATABASE_URL") != null) {
        return optionalPath("SBK_BACKEND_STORE_FILE")
    }
    return optionalPath("SBK_BACKEND_STORE_FILE") ?: Path.of("billing-backend-data.json")
}

private fun Map<String, String>.databaseConfig(): DatabaseConfig? {
    val url = optionalString("SBK_DATABASE_URL") ?: return null
    return DatabaseConfig(
        url = url,
        user = optionalString("SBK_DATABASE_USER"),
        password = optionalString("SBK_DATABASE_PASSWORD"),
    )
}

private fun Map<String, String>.optionalPath(name: String): Path? = optionalString(name)?.let(Path::of)

private fun Map<String, String>.optionalString(name: String): String? = this[name]?.trim()?.takeIf { it.isNotEmpty() }

private fun Map<String, String>.stringValue(
    name: String,
    default: String,
): String = optionalString(name) ?: default

private fun Map<String, String>.booleanValue(
    name: String,
    default: Boolean,
): Boolean =
    optionalString(name)?.lowercase()?.let {
        when (it) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> throw ConfigurationException(listOf("$name must be true or false"))
        }
    } ?: default

private inline fun <reified T : Enum<T>> Map<String, String>.enumValue(
    name: String,
    default: T,
): T =
    optionalString(name)?.uppercase()?.let { raw ->
        enumValues<T>().firstOrNull { it.name == raw }
            ?: throw ConfigurationException(listOf("$name has unsupported value: $raw"))
    } ?: default

private fun Map<String, String>.intValue(
    name: String,
    default: Int,
    range: IntRange,
): Int {
    val value = optionalString(name)?.toIntOrNull() ?: default
    if (value !in range) {
        throw ConfigurationException(listOf("$name must be in range ${range.first}..${range.last}"))
    }
    return value
}

private val PACKAGE_NAME_REGEX = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
private val PRODUCT_ID_REGEX = Regex("[A-Za-z0-9_.-]{1,128}")
