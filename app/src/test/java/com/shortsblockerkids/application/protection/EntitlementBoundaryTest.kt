package com.shortsblockerkids.application.protection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EntitlementBoundaryTest {
    @Test
    fun consumerSnapshotDoesNotExposeStorageOnlyMetadata() {
        val snapshotSource = source(APP_SETTINGS_SNAPSHOT)

        STORAGE_ONLY_NAMES.forEach { name ->
            assertFalse("consumer snapshot exposes $name", snapshotSource.contains(name))
        }
    }

    @Test
    fun applicationAndPlatformConsumersDoNotReceiveStorageRecord() {
        CONSUMER_SOURCES.forEach { relativePath ->
            val source = source(relativePath)

            STORAGE_RECORD_NAMES.forEach { storageRecordName ->
                assertFalse(
                    "$relativePath imports $storageRecordName",
                    source.contains(storageRecordName),
                )
            }
        }
    }

    @Test
    fun entitlementTypesUseApprovedPackagesAndNeutralResolverInput() {
        val root = repositoryRoot()
        val resolver = source(LOCAL_ENTITLEMENT_RESOLVER)

        OLD_TYPE_PATHS.forEach { relativePath ->
            assertFalse("old entitlement type remains: $relativePath", File(root, relativePath).exists())
        }
        NEW_TYPE_PATHS.forEach { relativePath ->
            assertTrue("missing entitlement type: $relativePath", File(root, relativePath).isFile)
        }
        assertTrue(resolver.contains("data class LocalEntitlementInput"))
        assertFalse(resolver.contains("AppSettings"))
        assertFalse(resolver.contains("core.billing"))
        assertFalse(resolver.contains("core.storage"))
    }

    private fun source(relativePath: String): String = File(repositoryRoot(), relativePath).readText()

    private fun repositoryRoot(): File {
        var candidate: File? = File(requireNotNull(System.getProperty("user.dir")))
        while (candidate != null) {
            if (File(candidate, "settings.gradle.kts").isFile) {
                return candidate
            }
            candidate = candidate.parentFile
        }
        error("Repository root not found")
    }

    private companion object {
        const val SOURCE_ROOT = "app/src/main/java/com/shortsblockerkids"
        const val APP_SETTINGS_SNAPSHOT = "$SOURCE_ROOT/application/model/AppSettingsSnapshot.kt"
        const val LOCAL_ENTITLEMENT_RESOLVER =
            "$SOURCE_ROOT/application/protection/LocalEntitlementResolver.kt"

        val STORAGE_ONLY_NAMES =
            listOf(
                "pinHash",
                "pinSalt",
                "pinHashVersion",
                "failedPinAttempts",
                "pinLockoutUntil",
                "billingInstallationId",
            )
        val STORAGE_RECORD_NAMES =
            listOf(
                "com.shortsblockerkids.core.storage.AppSettings",
                "com.shortsblockerkids.infrastructure.storage.StoredAppSettings",
            )
        val CONSUMER_SOURCES =
            listOf(
                "$SOURCE_ROOT/MainActivity.kt",
                "$SOURCE_ROOT/presentation/app/ShortsBlockerKidsApp.kt",
                "$SOURCE_ROOT/presentation/dashboard/DashboardScreen.kt",
                "$SOURCE_ROOT/presentation/onboarding/ProtectedAppsScreen.kt",
                "$SOURCE_ROOT/platform/accessibility/AccessibilityServiceRuntime.kt",
            )
        val OLD_TYPE_PATHS =
            listOf(
                "$SOURCE_ROOT/core/entitlement/FreeTestPolicy.kt",
                "$SOURCE_ROOT/core/entitlement/LocalEntitlementResolver.kt",
                "$SOURCE_ROOT/core/model/EntitlementState.kt",
                "$SOURCE_ROOT/core/billing/BillingEntitlementState.kt",
                "$SOURCE_ROOT/core/billing/BillingEntitlementSnapshot.kt",
                "$SOURCE_ROOT/core/billing/BillingVerificationPolicy.kt",
            )
        val NEW_TYPE_PATHS =
            listOf(
                APP_SETTINGS_SNAPSHOT,
                LOCAL_ENTITLEMENT_RESOLVER,
                "$SOURCE_ROOT/domain/entitlement/FreeTestPolicy.kt",
                "$SOURCE_ROOT/domain/entitlement/BillingEntitlementState.kt",
                "$SOURCE_ROOT/domain/entitlement/BillingEntitlementSnapshot.kt",
                "$SOURCE_ROOT/domain/entitlement/BillingVerificationPolicy.kt",
                "$SOURCE_ROOT/application/model/EntitlementState.kt",
            )
    }
}
