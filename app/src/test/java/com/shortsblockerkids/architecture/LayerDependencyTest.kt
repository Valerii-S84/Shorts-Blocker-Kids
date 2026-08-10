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
    fun presentationAcrossProductionSourceSetsDoesNotImportOuterImplementationsOrFrameworkPayloads() {
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
        )
    }

    @Test
    fun platformDoesNotImportOuterImplementationLayers() {
        assertNoImportsWithPrefixes(
            layer = "platform",
            forbiddenPrefixes =
                listOf(
                    "com.shortsblockerkids.presentation.",
                    "com.shortsblockerkids.composition.",
                    "com.shortsblockerkids.infrastructure.",
                ),
        )
    }

    @Test
    fun compositionDoesNotImportInfrastructureOrPlatform() {
        assertNoImportsWithPrefixes(
            layer = "composition",
            forbiddenPrefixes =
                listOf(
                    "com.shortsblockerkids.infrastructure.",
                    "com.shortsblockerkids.platform.",
                ),
        )
    }

    @Test
    fun infrastructureDoesNotImportPresentationOrCompositionOrPlatform() {
        assertNoImportsWithPrefixes(
            layer = "infrastructure",
            forbiddenPrefixes =
                listOf(
                    "com.shortsblockerkids.presentation.",
                    "com.shortsblockerkids.composition.",
                    "com.shortsblockerkids.platform.",
                ),
        )
    }

    @Test
    fun pinPolicyAndRateLimiterStayOutOfPresentationAndInfrastructure() {
        val forbiddenTokens =
            listOf(
                "com.shortsblockerkids.domain.pin.PinPolicy",
                "com.shortsblockerkids.domain.pin.PinRateLimiter",
                "com.shortsblockerkids.domain.pin.*",
            )
        val violations =
            SOURCE_SET_PATHS.flatMap { sourceSetPath ->
                listOf("presentation", "infrastructure").flatMap { layer ->
                    File(
                        repositoryRoot,
                        "$sourceSetPath/com/shortsblockerkids/$layer",
                    ).takeIf(File::isDirectory)
                        ?.walkTopDown()
                        ?.filter { source -> source.isFile && source.extension == "kt" }
                        ?.flatMap { source ->
                            forbiddenTokens
                                .filter(source.readText()::contains)
                                .map { token ->
                                    "${source.relativeTo(repositoryRoot)} contains $token"
                                }
                        }?.toList()
                        .orEmpty()
                }
            }

        assertTrue(
            "PIN policy boundary violations:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun legacyPinBoundarySourcesAreGone() {
        val remainingKotlinFiles =
            SOURCE_SET_PATHS.flatMap { sourceSetPath ->
                val packageRoot = File(repositoryRoot, "$sourceSetPath/com/shortsblockerkids")
                listOf(
                    File(packageRoot, "core/security"),
                    File(packageRoot, "application/port/PinAccessPort.kt"),
                    File(packageRoot, "infrastructure/storage/SettingsPinAccessAdapter.kt"),
                ).flatMap { candidate ->
                    when {
                        candidate.isDirectory ->
                            candidate
                                .walkTopDown()
                                .filter { file -> file.isFile && file.extension == "kt" }
                                .toList()

                        candidate.isFile -> listOf(candidate)
                        else -> emptyList()
                    }
                }
            }

        assertTrue(
            "legacy PIN boundary sources remain: $remainingKotlinFiles",
            remainingKotlinFiles.isEmpty(),
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
                "com.shortsblockerkids.core.security.",
                "com.shortsblockerkids.core.storage.",
                "com.shortsblockerkids.application.port.PinAccessPort",
                "com.shortsblockerkids.infrastructure.storage.SettingsPinAccessAdapter",
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
        sourceSets: List<String> = PRODUCTION_SOURCE_SET_PATHS,
        isAllowed: (File, String) -> Boolean,
    ) {
        val violations =
            sources(layer = layer, sourceSets = sourceSets).flatMap { source ->
                imports(source).filterNot { importedName -> isAllowed(source, importedName) }.map { importedName ->
                    "${sourceRelativeToRepository(source)} imports $importedName"
                }
            }

        assertTrue("Forbidden imports:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun assertNoImportsWithPrefixes(
        layer: String,
        sourceSets: List<String> = PRODUCTION_SOURCE_SET_PATHS,
        forbiddenPrefixes: List<String>,
    ) {
        val violations =
            sources(layer = layer, sourceSets = sourceSets).flatMap { source ->
                imports(source)
                    .filter { importedName ->
                        forbiddenPrefixes.any(importedName::startsWith)
                    }.map { importedName ->
                        "${sourceRelativeToRepository(source)} imports $importedName"
                    }
            }

        assertTrue("Forbidden imports:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun assertNoDirectSystemTime(layer: String) {
        assertNoSourceTokens(
            layer = layer,
            forbiddenTokens = listOf("System.currentTimeMillis("),
        )
    }

    private fun assertNoSourceTokens(
        layer: String,
        sourceSets: List<String> = PRODUCTION_SOURCE_SET_PATHS,
        forbiddenTokens: List<String>,
        excludedRelativePaths: Set<String> = emptySet(),
    ) {
        val violations =
            sources(layer, sourceSets)
                .filterNot { source ->
                    sourceRelativeToRepository(source) in excludedRelativePaths
                }.flatMap { source ->
                    val content = source.readText()
                    forbiddenTokens.filter(content::contains).map { token ->
                        "${sourceRelativeToRepository(source)} contains $token"
                    }
                }

        assertTrue("Forbidden source dependencies:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun imports(source: File): List<String> =
        IMPORT_PATTERN.findAll(source.readText()).map { match -> match.groupValues[1] }.toList()

    private fun sources(
        layer: String,
        sourceSets: List<String> = PRODUCTION_SOURCE_SET_PATHS,
    ): List<File> =
        sourceSets
            .map { sourceSetPath -> File(repositoryRoot, "$sourceSetPath/com/shortsblockerkids/$layer") }
            .filter(File::isDirectory)
            .flatMap { layerRoot ->
                layerRoot.walkTopDown().filter { file -> file.isFile && file.extension == "kt" }.toList()
            }

    private fun sourceRelativeToRepository(source: File): String = source.relativeTo(repositoryRoot).invariantSeparatorsPath

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
        val IMPORT_PATTERN = Regex("(?m)^import\\s+([^\\s]+)")
        val PRODUCTION_SOURCE_SET_PATHS =
            listOf(
                "app/src/main/java",
                "app/src/debug/java",
                "app/src/release/java",
            )
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
