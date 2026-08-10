package com.shortsblockerkids.application.protection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProtectionActivationBoundaryTest {
    @Test
    fun presentationDoesNotOwnActivationPolicy() {
        val source = source(PRESENTATION_APP)

        assertFalse(source.contains("ProtectionActivationPolicy"))
        assertFalse(source.contains("shouldStartFreeTest"))
    }

    @Test
    fun composeShellDelegatesActivationDecisionsToCoveredCoordinator() {
        val source = source(PRESENTATION_APP)

        assertTrue(source.contains("ProtectionActivationUiCoordinator"))
        assertFalse(source.contains("ProtectionActivationIntent"))
        assertFalse(source.contains("ProtectionActivationResult"))
    }

    @Test
    fun coverageExclusionsKeepActivationCoordinatorInTheDenominator() {
        val buildScript = source(APP_BUILD_SCRIPT)

        assertTrue(buildScript.contains("**/com/shortsblockerkids/MainActivity*.*"))
        assertTrue(buildScript.contains("**/presentation/app/MainActivityKt*.*"))
        assertFalse(buildScript.contains("**/MainActivity*.*"))
        assertFalse(buildScript.contains("**/presentation/app/**"))
        assertFalse(buildScript.contains("ProtectionActivationUiCoordinator"))
    }

    @Test
    fun applicationUseCaseIsTheOnlyProductionActivationStoreCaller() {
        val activationStoreCall = Regex("\\.completeProtectionActivation\\s*[({]")
        val callSites =
            productionKotlinFiles()
                .filter { file ->
                    activationStoreCall.containsMatchIn(file.readText())
                }.map { file -> file.relativeTo(repositoryRoot()).invariantSeparatorsPath }
                .toList()

        assertEquals(listOf(ACTIVATION_USE_CASE), callSites)
    }

    private fun source(relativePath: String): String = File(repositoryRoot(), relativePath).readText()

    private fun productionKotlinFiles(): Sequence<File> =
        File(repositoryRoot(), SOURCE_ROOT)
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }

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
        const val PRESENTATION_APP = "$SOURCE_ROOT/presentation/app/ShortsBlockerKidsApp.kt"
        const val APP_BUILD_SCRIPT = "app/build.gradle.kts"
        const val ACTIVATION_USE_CASE =
            "$SOURCE_ROOT/application/protection/RecordSuccessfulProtectionActivationUseCase.kt"
    }
}
