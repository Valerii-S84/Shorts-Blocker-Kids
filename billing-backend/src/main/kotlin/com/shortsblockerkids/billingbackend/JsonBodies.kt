package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

object JsonBodies {
    val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }

    fun parseVerificationRequest(body: String): VerificationRequest {
        val root = json.parseToJsonElement(body).jsonObject
        return VerificationRequest(
            installId = root.requiredString("install_id"),
            packageName = root.requiredString("package_name"),
            productId = root.requiredString("product_id"),
            purchaseToken = root.requiredString("purchase_token"),
            appVersion = root.optionalString("app_version"),
        )
    }

    fun parsePubSubNotification(body: String): PubSubNotification {
        val message = json.parseToJsonElement(body).jsonObject.requiredObject("message")
        val decodedData =
            String(
                Base64.getDecoder().decode(message.requiredString("data")),
                Charsets.UTF_8,
            )
        val data = json.parseToJsonElement(decodedData).jsonObject
        val subscription = data.requiredObject("subscriptionNotification")
        return PubSubNotification(
            messageId = message.requiredString("messageId"),
            productId = subscription.requiredString("subscriptionId"),
            purchaseToken = subscription.requiredString("purchaseToken"),
        )
    }

    fun entitlementResponse(record: EntitlementRecord?): String {
        val state = record?.state ?: SubscriptionEntitlementState.UNKNOWN
        val fields =
            mapOf(
                "state" to JsonPrimitive(state.name),
                "is_active" to JsonPrimitive(state.allowsProtection()),
                "active_until_millis" to nullableLong(record?.activeUntilMillis),
                "checked_at_millis" to nullableLong(record?.lastVerifiedAtMillis),
                "source" to JsonPrimitive("google_play_backend"),
            )
        return json.encodeToString(JsonObject.serializer(), JsonObject(fields))
    }

    fun statusResponse(status: String): String =
        json.encodeToString(
            JsonObject.serializer(),
            JsonObject(mapOf("status" to JsonPrimitive(status))),
        )

    fun errorResponse(message: String): String =
        json.encodeToString(
            JsonObject.serializer(),
            JsonObject(mapOf("error" to JsonPrimitive(message))),
        )

    private fun nullableLong(value: Long?): JsonElement =
        if (value == null) {
            JsonNull
        } else {
            JsonPrimitive(value)
        }

    private fun JsonObject.requiredString(name: String): String =
        optionalString(name)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required field: $name")

    private fun JsonObject.optionalString(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.requiredObject(name: String): JsonObject =
        this[name]?.jsonObject ?: throw IllegalArgumentException("Missing required object: $name")
}

data class PubSubNotification(
    val messageId: String,
    val productId: String,
    val purchaseToken: String,
)

fun JsonObject.optionalObject(name: String): JsonObject? = this[name] as? JsonObject

fun JsonObject.optionalArray(name: String): JsonArray? = this[name] as? JsonArray

fun JsonObject.optionalString(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull

fun JsonObject.optionalBoolean(name: String): Boolean? = this[name]?.jsonPrimitive?.booleanOrNull
