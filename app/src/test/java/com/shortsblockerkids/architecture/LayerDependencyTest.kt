package com.shortsblockerkids.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LayerDependencyTest {
    @Test
    fun domainImportsOnlyKotlinJavaAndDomainTypes() {
        assertImports("domain") { importedName ->
            importedName.startsWith("kotlin.") ||
                importedName.startsWith("java.") ||
                importedName.startsWith("com.shortsblockerkids.domain.")
        }
        assertNoDirectSystemTime("domain")
    }

    @Test
    fun applicationImportsOnlyApplicationDomainAndCoroutines() {
        assertImports("application") { importedName ->
            importedName.startsWith("kotlin.") ||
                importedName.startsWith("java.") ||
                importedName.startsWith("kotlinx.coroutines.") ||
                importedName.startsWith("com.shortsblockerkids.application.") ||
                importedName.startsWith("com.shortsblockerkids.domain.")
        }
        assertNoDirectSystemTime("application")
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
                    "System.currentTimeMillis(",
                    "pinHash",
                    "pinSalt",
                    "billingInstallationId",
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

    private fun assertImports(
        layer: String,
        isAllowed: (String) -> Boolean,
    ) {
        val violations =
            sources(layer).flatMap { source ->
                imports(source).filterNot(isAllowed).map { importedName ->
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
    ) {
        val violations =
            sources(layer).flatMap { source ->
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

    private val sourceRoot: File by lazy {
        generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { directory ->
            directory.parentFile
        }.map { directory ->
            File(directory, "app/src/main/java/com/shortsblockerkids")
        }.firstOrNull(File::isDirectory)
            ?: error("Production source root not found")
    }

    private companion object {
        val IMPORT_PATTERN = Regex("(?m)^import\\s+([^\\s]+)")
    }
}
