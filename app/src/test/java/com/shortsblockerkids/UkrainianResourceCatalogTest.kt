package com.shortsblockerkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class UkrainianResourceCatalogTest {
    @Test
    fun ukrainianCatalogMatchesDefaultResourceNames() {
        assertEquals(EXPECTED_STRING_COUNT, defaultCatalog.stringNames.size)
        assertEquals(EXPECTED_PLURAL_COUNT, defaultCatalog.pluralNames.size)

        val missingResources = defaultCatalog.resourceKeys - ukrainianCatalog.resourceKeys
        val extraResources = ukrainianCatalog.resourceKeys - defaultCatalog.resourceKeys

        assertTrue("Missing Ukrainian resources: ${missingResources.sorted()}", missingResources.isEmpty())
        assertTrue("Extra Ukrainian resources: ${extraResources.sorted()}", extraResources.isEmpty())
        assertEquals(EXPECTED_STRING_COUNT, ukrainianCatalog.stringNames.size)
        assertEquals(EXPECTED_PLURAL_COUNT, ukrainianCatalog.pluralNames.size)
    }

    @Test
    fun ukrainianStringsPreservePlaceholderPositionsTypesAndOrder() {
        val defaultPlaceholders = defaultCatalog.stringPlaceholderSignatures()
        val ukrainianPlaceholders = ukrainianCatalog.stringPlaceholderSignatures()
        val mismatches =
            (defaultPlaceholders.keys + ukrainianPlaceholders.keys)
                .filter { defaultPlaceholders[it] != ukrainianPlaceholders[it] }

        assertTrue(
            buildString {
                append("Ukrainian string placeholder mismatches:")
                mismatches.sorted().forEach { resourcePath ->
                    append(
                        "\n$resourcePath: default=${defaultPlaceholders[resourcePath]}, " +
                            "Ukrainian=${ukrainianPlaceholders[resourcePath]}",
                    )
                }
            },
            mismatches.isEmpty(),
        )
    }

    @Test
    fun ukrainianPluralContractsMatchDefaultCatalog() {
        val defaultContracts = defaultCatalog.pluralPlaceholderContracts()
        val ukrainianContracts = ukrainianCatalog.pluralPlaceholderContracts()
        val mismatches =
            (defaultContracts.keys + ukrainianContracts.keys)
                .filter { defaultContracts[it] != ukrainianContracts[it] }

        assertTrue(
            buildString {
                append("Ukrainian plural contract mismatches:")
                mismatches.sorted().forEach { pluralName ->
                    append(
                        "\n$pluralName: default=${defaultContracts[pluralName]}, " +
                            "Ukrainian=${ukrainianContracts[pluralName]}",
                    )
                }
            },
            mismatches.isEmpty(),
        )
    }

    @Test
    fun ukrainianPluralsContainRequiredQuantities() {
        val mismatches =
            ukrainianCatalog.pluralQuantities
                .filterValues { it != REQUIRED_UKRAINIAN_QUANTITIES }

        assertEquals(defaultCatalog.pluralNames, ukrainianCatalog.pluralNames)
        assertTrue("Ukrainian plural quantity mismatches: $mismatches", mismatches.isEmpty())
    }

    @Test
    fun ukrainianCatalogPreservesTranslatableAttributes() {
        assertEquals(defaultCatalog.translatableAttributes, ukrainianCatalog.translatableAttributes)
    }

    private val defaultCatalog by lazy { ResourceCatalog.parse(repoFile(DEFAULT_CATALOG)) }
    private val ukrainianCatalog by lazy { ResourceCatalog.parse(repoFile(UKRAINIAN_CATALOG)) }

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
        val stringValues: Map<String, String>,
        val pluralValues: Map<String, Map<String, String>>,
        val translatableAttributes: Map<String, String>,
    ) {
        val stringNames: Set<String>
            get() = stringValues.keys

        val pluralNames: Set<String>
            get() = pluralValues.keys

        val resourceKeys: Set<String>
            get() =
                buildSet {
                    stringNames.forEach { add("string/$it") }
                    pluralNames.forEach { add("plurals/$it") }
                }

        val pluralQuantities: Map<String, Set<String>>
            get() = pluralValues.mapValues { (_, valuesByQuantity) -> valuesByQuantity.keys }

        fun stringPlaceholderSignatures(): Map<String, List<Placeholder>> =
            stringValues
                .mapKeys { (name, _) -> "string/$name" }
                .mapValues { (_, value) -> value.placeholderSignature() }

        fun pluralPlaceholderContracts(): Map<String, List<Placeholder>> =
            pluralValues.mapValues { (pluralName, valuesByQuantity) ->
                val signatures =
                    valuesByQuantity.values
                        .map { value -> value.placeholderSignature() }
                        .toSet()
                check(signatures.size == 1) {
                    "Plural plurals/$pluralName has inconsistent placeholder signatures: $signatures"
                }
                signatures.single()
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
                val stringValues = mutableMapOf<String, String>()
                val pluralValues = mutableMapOf<String, Map<String, String>>()
                val translatableAttributes = mutableMapOf<String, String>()
                val resources = document.documentElement.childNodes

                for (index in 0 until resources.length) {
                    val resource = resources.item(index) as? Element ?: continue
                    val name = resource.getAttribute("name")
                    when (resource.tagName) {
                        "string" -> {
                            check(stringValues.put(name, resource.textContent.trim()) == null) {
                                "Duplicate string resource: $name"
                            }
                            recordTranslatableAttribute(resource, "string/$name", translatableAttributes)
                        }

                        "plurals" -> {
                            check(pluralValues.put(name, pluralItems(resource, name)) == null) {
                                "Duplicate plurals resource: $name"
                            }
                            recordTranslatableAttribute(resource, "plurals/$name", translatableAttributes)
                        }
                    }
                }

                return ResourceCatalog(
                    stringValues = stringValues,
                    pluralValues = pluralValues,
                    translatableAttributes = translatableAttributes,
                )
            }

            private fun pluralItems(
                plural: Element,
                pluralName: String,
            ): Map<String, String> {
                val valuesByQuantity = mutableMapOf<String, String>()
                val items = plural.childNodes
                for (index in 0 until items.length) {
                    val item = items.item(index) as? Element ?: continue
                    if (item.tagName != "item") continue
                    val quantity = item.getAttribute("quantity")
                    check(valuesByQuantity.put(quantity, item.textContent.trim()) == null) {
                        "Duplicate quantity $quantity in plurals/$pluralName"
                    }
                }
                return valuesByQuantity
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
        const val UKRAINIAN_CATALOG = "app/src/main/res/values-uk/strings.xml"
        const val EXPECTED_STRING_COUNT = 197
        const val EXPECTED_PLURAL_COUNT = 4

        val REQUIRED_UKRAINIAN_QUANTITIES = setOf("one", "few", "many", "other")
        val PLACEHOLDER_REGEX = Regex("""%(?:(\d+)\$)?([a-zA-Z])""")

        fun String.placeholderSignature(): List<Placeholder> =
            PLACEHOLDER_REGEX
                .findAll(this)
                .map { match ->
                    Placeholder(
                        position = match.groupValues[1].takeIf(String::isNotEmpty)?.toInt(),
                        type = match.groupValues[2],
                    )
                }.toList()
    }
}
