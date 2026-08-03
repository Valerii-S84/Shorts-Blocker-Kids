package com.shortsblockerkids.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LayerDependencyTest {
    @Test
    fun domainImportsOnlyKotlinJavaAndDomainTypes() {
        assertImports("domain") { _, importedName ->
            importedName.startsWith("kotlin.") ||
                importedName.startsWith("java.") ||
                importedName.startsWith("com.shortsblockerkids.domain.")
        }
        assertNoDirectSystemTime("domain")
    }

    @Test
    fun applicationImportsOnlyApplicationDomainAndCoroutines() {
        assertImports("application") { _, importedName ->
            importedName.startsWith("kotlin.") ||
                importedName.startsWith("java.") ||
                importedName.startsWith("kotlinx.coroutines.") ||
                importedName.startsWith("com.shortsblockerkids.application.") ||
                importedName.startsWith("com.shortsblockerkids.domain.")
        }
        assertNoDirectSystemTime("application")
    }

    @Test
    fun applicationBillingAvoidsAndroidGoogleBillingStorageAndPresentationDependencies() {
        assertNoImportsWithPrefixes(
            layer = "application/billing",
            forbiddenPrefixes =
                listOf(
                    "android.",
                    "androidx.",
                    "com.android.billingclient.",
                ),
        )
        assertNoSourceTokens(
            layer = "application/billing",
            forbiddenTokens =
                listOf(
                    "android.",
                    "androidx.",
                    "com.android.billingclient.",
                    "Context",
                    "Activity",
                    "Intent",
                    "Uri",
                    "BuildConfig",
                    "DataStore",
                    "Composable",
                    "R.",
                ),
        )
    }

    @Test
    fun presentationDoesNotImportOuterImplementationsOrFrameworkPayloads() {
        assertNoImportsWithPrefixes(
            layer = "presentation",
            forbiddenPrefixes =
                listOf(
                    "android.accessibilityservice.",
                    "android.view.accessibility.",
                    "androidx.datastore.",
                    "com.android.billingclient.",
                    "com.shortsblockerkids.core.storage.",
                    "com.shortsblockerkids.infrastructure.",
                    "com.shortsblockerkids.platform.",
                ),
        )
        assertNoSourceTokens(
            layer = "presentation",
            forbiddenTokens =
                listOf(
                    "pinHash",
                    "pinSalt",
                    "billingInstallationId",
                ),
        )
        assertNoSourceTokens(
            layer = "presentation",
            forbiddenTokens = listOf("System.currentTimeMillis("),
            excludedRelativePaths = setOf(PRESENTATION_PIN_ENTRY_SCREEN),
        )
    }

    @Test
    fun infrastructureBillingDoesNotImportPresentation() {
        assertNoImportsWithPrefixes(
            layer = "infrastructure/billing",
            forbiddenPrefixes = listOf("com.shortsblockerkids.presentation."),
        )
    }

    @Test
    fun presentationBillingAvoidsAndroidAndGoogleBillingPayloads() {
        assertNoImportsWithPrefixes(
            layer = "presentation/billing",
            forbiddenPrefixes =
                listOf(
                    "android.",
                    "com.android.billingclient.",
                    "com.shortsblockerkids.infrastructure.",
                ),
        )
    }

    @Test
    fun transitionalCoreStoragePackageIsGone() {
        val legacyStorage = File(sourceRoot, "core/storage")
        val remainingKotlinFiles =
            if (legacyStorage.isDirectory) {
                legacyStorage.walkTopDown().filter { file -> file.extension == "kt" }.toList()
            } else {
                emptyList()
            }

        assertTrue(
            "transitional core/storage Kotlin files remain: $remainingKotlinFiles",
            remainingKotlinFiles.isEmpty(),
        )
    }

    @Test
    fun legacyBillingPackagesAreGone() {
        val remainingKotlinFiles =
            listOf("core/billing", "feature/billing")
                .flatMap { relativePath ->
                    File(sourceRoot, relativePath)
                        .takeIf(File::isDirectory)
                        ?.walkTopDown()
                        ?.filter { file -> file.isFile && file.extension == "kt" }
                        ?.toList()
                        .orEmpty()
                }

        assertTrue(
            "legacy billing Kotlin files remain: $remainingKotlinFiles",
            remainingKotlinFiles.isEmpty(),
        )
    }

    @Test
    fun legacyFeatureKotlinSourcesAreGone() {
        val remainingKotlinFiles =
            SOURCE_SET_PATHS
                .map { sourceSetPath ->
                    File(repositoryRoot, "$sourceSetPath/com/shortsblockerkids/feature")
                }.filter(File::isDirectory)
                .flatMap { legacyFeature ->
                    legacyFeature
                        .walkTopDown()
                        .filter { file -> file.isFile && file.extension == "kt" }
                        .toList()
                }

        assertTrue(
            "legacy feature Kotlin files remain: $remainingKotlinFiles",
            remainingKotlinFiles.isEmpty(),
        )
    }

    @Test
    fun allSourceSetsAvoidRemovedArchitectureTypes() {
        val forbiddenImports =
            listOf(
                "com.shortsblockerkids.core.model.ProtectionMode",
                "com.shortsblockerkids.core.security.PinVerificationResult",
                "com.shortsblockerkids.core.storage.",
            )
        val violations =
            SOURCE_SET_PATHS
                .map { relativePath -> File(repositoryRoot, relativePath) }
                .filter(File::isDirectory)
                .flatMap { sourceSetRoot ->
                    sourceSetRoot
                        .walkTopDown()
                        .filter { file -> file.isFile && file.extension == "kt" }
                        .flatMap { source ->
                            imports(source)
                                .filter { importedName ->
                                    forbiddenImports.any(importedName::startsWith)
                                }.map { importedName ->
                                    "${source.relativeTo(repositoryRoot)} imports $importedName"
                                }
                        }
                }

        assertTrue("Removed architecture imports:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun assertImports(
        layer: String,
        isAllowed: (File, String) -> Boolean,
    ) {
        val violations =
            sources(layer).flatMap { source ->
                imports(source).filterNot { importedName -> isAllowed(source, importedName) }.map { importedName ->
                    "${source.relativeTo(sourceRoot)} imports $importedName"
                }
            }

        assertTrue("Forbidden imports:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun assertNoImportsWithPrefixes(
        layer: String,
        forbiddenPrefixes: List<String>,
    ) {
        val violations =
            sources(layer).flatMap { source ->
                imports(source)
                    .filter { importedName ->
                        forbiddenPrefixes.any(importedName::startsWith)
                    }.map { importedName ->
                        "${source.relativeTo(sourceRoot)} imports $importedName"
                    }
            }

        assertTrue("Forbidden imports:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun assertNoDirectSystemTime(layer: String) {
        assertNoSourceTokens(layer, listOf("System.currentTimeMillis("))
    }

    private fun assertNoSourceTokens(
        layer: String,
        forbiddenTokens: List<String>,
        excludedRelativePaths: Set<String> = emptySet(),
    ) {
        val violations =
            sources(layer)
                .filterNot { source ->
                    source.relativeTo(sourceRoot).invariantSeparatorsPath in excludedRelativePaths
                }.flatMap { source ->
                    val content = source.readText()
                    forbiddenTokens.filter(content::contains).map { token ->
                        "${source.relativeTo(sourceRoot)} contains $token"
                    }
                }

        assertTrue("Forbidden source dependencies:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun imports(source: File): List<String> =
        IMPORT_PATTERN.findAll(source.readText()).map { match -> match.groupValues[1] }.toList()

    private fun sources(layer: String): List<File> =
        File(sourceRoot, layer)
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .toList()

    private val repositoryRoot: File by lazy {
        generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { directory ->
            directory.parentFile
        }.firstOrNull { directory -> File(directory, "settings.gradle.kts").isFile }
            ?: error("Repository root not found")
    }

    private val sourceRoot: File by lazy {
        File(repositoryRoot, "app/src/main/java/com/shortsblockerkids")
    }

    private companion object {
        const val PRESENTATION_PIN_ENTRY_SCREEN = "presentation/pin/PinEntryScreen.kt"
        val IMPORT_PATTERN = Regex("(?m)^import\\s+([^\\s]+)")
        val SOURCE_SET_PATHS =
            listOf(
                "app/src/main/java",
                "app/src/debug/java",
                "app/src/release/java",
                "app/src/test/java",
                "app/src/androidTest/java",
            )
    }
}
