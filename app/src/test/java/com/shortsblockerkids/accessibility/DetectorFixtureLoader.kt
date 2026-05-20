package com.shortsblockerkids.accessibility

object DetectorFixtureLoader {
    fun load(fileName: String): DetectorFixture {
        val text =
            requireNotNull(
                javaClass.classLoader?.getResourceAsStream("fixtures/$fileName"),
            ) {
                "Missing fixture: $fileName"
            }.bufferedReader().use { it.readText() }

        return DetectorFixture(
            packageName = text.stringValue("packageName"),
            snapshot =
                AccessibilityTreeSnapshot(
                    nodes = nodeObjects(text).map { it.toNodeSignal() },
                ),
        )
    }

    private fun nodeObjects(text: String): List<String> =
        Regex(
            "\\{\\s*\"className\".*?\\n\\s*\\}",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ).findAll(text).map { it.value }.toList()

    private fun String.toNodeSignal(): AccessibilityNodeSignal =
        AccessibilityNodeSignal(
            className = stringValue("className"),
            viewIdResourceName = nullableStringValue("viewIdResourceName"),
            contentDescriptionSignals = stringArray("contentDescriptionSignals"),
            isClickable = booleanValue("isClickable"),
            isScrollable = booleanValue("isScrollable"),
            isVisibleToUser = booleanValue("isVisibleToUser"),
            left = intValue("left"),
            top = intValue("top"),
            right = intValue("right"),
            bottom = intValue("bottom"),
            width = intValue("width"),
            height = intValue("height"),
            depth = intValue("depth"),
        )

    private fun String.stringValue(key: String): String =
        requireNotNull(
            Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(this)?.groupValues?.get(1),
        ) {
            "Missing string value for $key"
        }

    private fun String.nullableStringValue(key: String): String? {
        if (Regex("\"$key\"\\s*:\\s*null").containsMatchIn(this)) {
            return null
        }
        return stringValue(key)
    }

    private fun String.booleanValue(key: String): Boolean =
        requireNotNull(
            Regex("\"$key\"\\s*:\\s*(true|false)").find(this)?.groupValues?.get(1),
        ) {
            "Missing boolean value for $key"
        }.toBoolean()

    private fun String.intValue(key: String): Int =
        requireNotNull(
            Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(this)?.groupValues?.get(1),
        ) {
            "Missing int value for $key"
        }.toInt()

    private fun String.stringArray(key: String): Set<String> {
        val raw =
            requireNotNull(
                Regex("\"$key\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
                    .find(this)
                    ?.groupValues
                    ?.get(1),
            ) {
                "Missing array value for $key"
            }

        return Regex("\"([^\"]*)\"")
            .findAll(raw)
            .map { it.groupValues[1] }
            .toSet()
    }
}

data class DetectorFixture(
    val packageName: String,
    val snapshot: AccessibilityTreeSnapshot,
)
