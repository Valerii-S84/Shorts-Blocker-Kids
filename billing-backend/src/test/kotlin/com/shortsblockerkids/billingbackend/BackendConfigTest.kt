package com.shortsblockerkids.billingbackend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackendConfigTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun productionRequiresDurableStorageHttpsCredentialsAndPubSubAuth() {
        val exception =
            runCatching {
                BackendConfig.fromEnvironment(
                    mapOf(
                        "SBK_ENV" to "production",
                        "SBK_PUBLIC_BASE_URL" to "http://billing.example.com",
                    ),
                )
            }.exceptionOrNull() as ConfigurationException

        assertTrue(exception.errors.contains("SBK_DATABASE_URL is required in production"))
        assertTrue(exception.errors.contains("SBK_PUBLIC_BASE_URL must use https:// in production"))
        assertTrue(exception.errors.any { it.contains("GOOGLE_APPLICATION_CREDENTIALS") })
        assertTrue(exception.errors.any { it.contains("Authenticated Pub/Sub RTDN") })
    }

    @Test
    fun productionConfigAcceptsPostgresAndRuntimeCredentials() {
        val credentialsFile = temporaryFolder.newFile("service-account.json")

        val config =
            BackendConfig.fromEnvironment(
                mapOf(
                    "SBK_ENV" to "production",
                    "SBK_PUBLIC_BASE_URL" to "https://billing.example.com",
                    "SBK_DATABASE_URL" to "jdbc:postgresql://db:5432/shorts_blocker_kids",
                    "GOOGLE_APPLICATION_CREDENTIALS" to credentialsFile.absolutePath,
                    "SBK_RTDN_PUBSUB_AUDIENCE" to "https://billing.example.com/billing/play/rtdn",
                    "SBK_RTDN_PUBSUB_SERVICE_ACCOUNT_EMAIL" to "pubsub@example.iam.gserviceaccount.com",
                ),
            )

        assertEquals(StorageBackend.POSTGRES, config.storageBackend)
        assertEquals(true, config.requireHttps)
    }
}
