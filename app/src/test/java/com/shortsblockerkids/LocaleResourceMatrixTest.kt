package com.shortsblockerkids

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(Parameterized::class)
class LocaleResourceMatrixTest(
    private val scenario: String,
    private val localeTag: String,
    private val expectedCatalogPath: String,
    private val expectedBlockingTitle: String,
) {
    @Test
    fun localeResolvesExpectedCatalogAndCopy() {
        val resolvedResource = resolveString(localeTag, BLOCKING_TITLE_RESOURCE)

        assertEquals("$scenario catalog", expectedCatalogPath, resolvedResource.catalogPath)
        assertEquals("$scenario copy", expectedBlockingTitle, resolvedResource.value)
    }

    private fun resolveString(
        localeTag: String,
        resourceName: String,
    ): ResolvedResource {
        val language = Locale.forLanguageTag(localeTag).language
        val localizedCatalogPath = "app/src/main/res/values-$language/strings.xml"
        val catalogPath =
            localizedCatalogPath
                .takeIf { repoFileOrNull(it) != null }
                ?: DEFAULT_CATALOG_PATH
        val catalog = requireNotNull(repoFileOrNull(catalogPath))

        return ResolvedResource(
            catalogPath = catalogPath,
            value = parseStringResource(catalog, resourceName),
        )
    }

    private fun parseStringResource(
        catalog: File,
        resourceName: String,
    ): String {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply {
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                }.newDocumentBuilder()
                .parse(catalog)
        val resources = document.documentElement.childNodes

        for (index in 0 until resources.length) {
            val resource = resources.item(index) as? Element ?: continue
            if (resource.tagName == "string" && resource.getAttribute("name") == resourceName) {
                return resource.textContent.trim()
            }
        }
        error("Missing string/$resourceName in ${catalog.path}")
    }

    private fun repoFileOrNull(relativePath: String): File? {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var root: File? = File(userDir)
        while (root != null) {
            val candidate = File(root, relativePath)
            if (candidate.isFile) {
                return candidate
            }
            root = root.parentFile
        }
        return null
    }

    private data class ResolvedResource(
        val catalogPath: String,
        val value: String,
    )

    private companion object {
        const val DEFAULT_CATALOG_PATH = "app/src/main/res/values/strings.xml"
        const val GERMAN_CATALOG_PATH = "app/src/main/res/values-de/strings.xml"
        const val UKRAINIAN_CATALOG_PATH = "app/src/main/res/values-uk/strings.xml"
        const val BLOCKING_TITLE_RESOURCE = "blocking_overlay_title"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}: {1}")
        fun localeMatrix(): List<Array<Any>> =
            listOf(
                arrayOf("English", "en-US", DEFAULT_CATALOG_PATH, "Short video blocked"),
                arrayOf("German", "de-DE", GERMAN_CATALOG_PATH, "Kurzvideo blockiert"),
                arrayOf("Ukrainian", "uk-UA", UKRAINIAN_CATALOG_PATH, "Коротке відео заблоковано"),
                arrayOf("English fallback", "fr-FR", DEFAULT_CATALOG_PATH, "Short video blocked"),
            )
    }
}
