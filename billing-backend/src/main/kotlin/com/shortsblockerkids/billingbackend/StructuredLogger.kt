package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant

class StructuredLogger(
    private val now: () -> Instant = Instant::now,
) {
    fun info(
        event: String,
        fields: Map<String, Any?> = emptyMap(),
    ) {
        write("INFO", event, fields)
    }

    fun warn(
        event: String,
        fields: Map<String, Any?> = emptyMap(),
    ) {
        write("WARN", event, fields)
    }

    fun audit(
        event: String,
        fields: Map<String, Any?> = emptyMap(),
    ) {
        write("AUDIT", event, fields + ("audit" to "billing"))
    }

    private fun write(
        level: String,
        event: String,
        fields: Map<String, Any?>,
    ) {
        val payload =
            mutableMapOf(
                "ts" to JsonPrimitive(now().toString()),
                "level" to JsonPrimitive(level),
                "event" to JsonPrimitive(event),
            )
        fields.forEach { (name, value) ->
            value?.let { payload[name] = primitive(it) }
        }
        println(JsonBodies.json.encodeToString(JsonObject.serializer(), JsonObject(payload)))
    }

    private fun primitive(value: Any): JsonPrimitive =
        when (value) {
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
}
