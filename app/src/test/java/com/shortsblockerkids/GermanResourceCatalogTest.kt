package com.shortsblockerkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class GermanResourceCatalogTest {
    @Test
    fun germanCatalogMatchesDefaultResourceNames() {
        assertEquals(EXPECTED_STRING_COUNT, defaultCatalog.stringNames.size)
        assertEquals(EXPECTED_PLURAL_COUNT, defaultCatalog.pluralNames.size)

        val missingResources = defaultCatalog.resourceKeys - germanCatalog.resourceKeys
        val extraResources = germanCatalog.resourceKeys - defaultCatalog.resourceKeys

        assertTrue("Missing German resources: ${missingResources.sorted()}", missingResources.isEmpty())
        assertTrue("Extra German resources: ${extraResources.sorted()}", extraResources.isEmpty())
        assertEquals(EXPECTED_STRING_COUNT, germanCatalog.stringNames.size)
        assertEquals(EXPECTED_PLURAL_COUNT, germanCatalog.pluralNames.size)
    }

    @Test
    fun germanCatalogPreservesPlaceholderPositionsTypesAndOrder() {
        val defaultPlaceholders = defaultCatalog.placeholderSignatures()
        val germanPlaceholders = germanCatalog.placeholderSignatures()
        val mismatches =
            (defaultPlaceholders.keys + germanPlaceholders.keys)
                .filter { defaultPlaceholders[it] != germanPlaceholders[it] }

        assertTrue(
            buildString {
                append("German placeholder mismatches:")
                mismatches.sorted().forEach { resourcePath ->
                    append(
                        "\n$resourcePath: default=${defaultPlaceholders[resourcePath]}, " +
                            "German=${germanPlaceholders[resourcePath]}",
                    )
                }
            },
            mismatches.isEmpty(),
        )
    }

    @Test
    fun germanPluralsContainRequiredQuantities() {
        val mismatches =
            germanCatalog.pluralQuantities
                .filterValues { it != REQUIRED_GERMAN_QUANTITIES }

        assertEquals(defaultCatalog.pluralNames, germanCatalog.pluralNames)
        assertTrue("German plural quantity mismatches: $mismatches", mismatches.isEmpty())
    }

    @Test
    fun germanCatalogPreservesTranslatableAttributes() {
        assertEquals(defaultCatalog.translatableAttributes, germanCatalog.translatableAttributes)
    }

    private val defaultCatalog by lazy { ResourceCatalog.parse(repoFile(DEFAULT_CATALOG)) }
    private val germanCatalog by lazy { ResourceCatalog.parse(repoFile(GERMAN_CATALOG)) }

    private fun repoFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var root: File? = File(userDir)
        while (root != null) {
            val candidate = File(root, relativePath)
            if (candidate.isFile) {
                return candidate
            }
            root = root.parentFile
        }
        error("Missing repository file: $relativePath")
    }

    private data class ResourceCatalog(
        val stringNames: Set<String>,
        val pluralNames: Set<String>,
        val valuesByPath: Map<String, String>,
        val pluralQuantities: Map<String, Set<String>>,
        val translatableAttributes: Map<String, String>,
    ) {
        val resourceKeys: Set<String>
            get() =
                buildSet {
                    stringNames.forEach { add("string/$it") }
                    pluralNames.forEach { add("plurals/$it") }
                }

        fun placeholderSignatures(): Map<String, List<Placeholder>> =
            valuesByPath.mapValues { (_, value) ->
                PLACEHOLDER_REGEX
                    .findAll(value)
                    .map { match ->
                        Placeholder(
                            position = match.groupValues[1].takeIf(String::isNotEmpty)?.toInt(),
                            type = match.groupValues[2],
                        )
                    }.toList()
            }

        companion object {
            fun parse(file: File): ResourceCatalog {
                val document =
                    DocumentBuilderFactory
                        .newInstance()
                        .apply {
                            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                        }.newDocumentBuilder()
                        .parse(file)
                val stringNames = mutableSetOf<String>()
                val pluralNames = mutableSetOf<String>()
                val valuesByPath = mutableMapOf<String, String>()
                val pluralQuantities = mutableMapOf<String, Set<String>>()
                val translatableAttributes = mutableMapOf<String, String>()
                val resources = document.documentElement.childNodes

                for (index in 0 until resources.length) {
                    val resource = resources.item(index) as? Element ?: continue
                    val name = resource.getAttribute("name")
                    when (resource.tagName) {
                        "string" -> {
                            check(stringNames.add(name)) { "Duplicate string resource: $name" }
                            valuesByPath["string/$name"] = resource.textContent.trim()
                            recordTranslatableAttribute(resource, "string/$name", translatableAttributes)
                        }

                        "plurals" -> {
                            check(pluralNames.add(name)) { "Duplicate plurals resource: $name" }
                            val quantities = pluralItems(resource, name, valuesByPath)
                            pluralQuantities[name] = quantities
                            recordTranslatableAttribute(resource, "plurals/$name", translatableAttributes)
                        }
                    }
                }

                return ResourceCatalog(
                    stringNames = stringNames,
                    pluralNames = pluralNames,
                    valuesByPath = valuesByPath,
                    pluralQuantities = pluralQuantities,
                    translatableAttributes = translatableAttributes,
                )
            }

            private fun pluralItems(
                plural: Element,
                pluralName: String,
                valuesByPath: MutableMap<String, String>,
            ): Set<String> {
                val quantities = mutableSetOf<String>()
                val items = plural.childNodes
                for (index in 0 until items.length) {
                    val item = items.item(index) as? Element ?: continue
                    if (item.tagName != "item") continue
                    val quantity = item.getAttribute("quantity")
                    check(quantities.add(quantity)) {
                        "Duplicate quantity $quantity in plurals/$pluralName"
                    }
                    valuesByPath["plurals/$pluralName[$quantity]"] = item.textContent.trim()
                }
                return quantities
            }

            private fun recordTranslatableAttribute(
                resource: Element,
                resourcePath: String,
                attributes: MutableMap<String, String>,
            ) {
                if (resource.hasAttribute("translatable")) {
                    attributes[resourcePath] = resource.getAttribute("translatable")
                }
            }
        }
    }

    private data class Placeholder(
        val position: Int?,
        val type: String,
    )

    private companion object {
        const val DEFAULT_CATALOG = "app/src/main/res/values/strings.xml"
        const val GERMAN_CATALOG = "app/src/main/res/values-de/strings.xml"
        const val EXPECTED_STRING_COUNT = 197
        const val EXPECTED_PLURAL_COUNT = 4

        val REQUIRED_GERMAN_QUANTITIES = setOf("one", "other")
        val PLACEHOLDER_REGEX = Regex("""%(?:(\d+)\$)?([a-zA-Z])""")
    }
}
