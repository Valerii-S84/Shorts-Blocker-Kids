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
            installId = root.requiredString("install_id").validated("install_id", INSTALL_ID_REGEX, 128),
            packageName = root.requiredString("package_name").validated("package_name", PACKAGE_NAME_REGEX, 255),
            productId = root.requiredString("product_id").validated("product_id", PRODUCT_ID_REGEX, 128),
            purchaseToken = root.requiredString("purchase_token").validated("purchase_token", TOKEN_REGEX, 4_096),
            appVersion = root.optionalString("app_version")?.validated("app_version", APP_VERSION_REGEX, 64),
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
            messageId = message.requiredString("messageId").validated("messageId", MESSAGE_ID_REGEX, 256),
            packageName = data.requiredString("packageName").validated("packageName", PACKAGE_NAME_REGEX, 255),
            productId = subscription.requiredString("subscriptionId").validated("subscriptionId", PRODUCT_ID_REGEX, 128),
            purchaseToken = subscription.requiredString("purchaseToken").validated("purchaseToken", TOKEN_REGEX, 4_096),
            notificationType = subscription.optionalInt("notificationType"),
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

    private fun String.validated(
        name: String,
        pattern: Regex,
        maxLength: Int,
    ): String {
        require(length <= maxLength && pattern.matches(this)) { "Invalid field: $name" }
        return this
    }

    private val PACKAGE_NAME_REGEX = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
    private val PRODUCT_ID_REGEX = Regex("[A-Za-z0-9_.-]{1,128}")
    private val INSTALL_ID_REGEX = Regex("[A-Za-z0-9_.:-]{1,128}")
    private val TOKEN_REGEX = Regex("[^\\p{Cntrl}\\s]{1,4096}")
    private val APP_VERSION_REGEX = Regex("[A-Za-z0-9_.+:-]{1,64}")
    private val MESSAGE_ID_REGEX = Regex("[A-Za-z0-9_.:-]{1,256}")
}

data class PubSubNotification(
    val messageId: String,
    val packageName: String,
    val productId: String,
    val purchaseToken: String,
    val notificationType: Int?,
)

fun JsonObject.optionalObject(name: String): JsonObject? = this[name] as? JsonObject

fun JsonObject.optionalArray(name: String): JsonArray? = this[name] as? JsonArray

fun JsonObject.optionalString(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull

fun JsonObject.optionalBoolean(name: String): Boolean? = this[name]?.jsonPrimitive?.booleanOrNull

fun JsonObject.optionalInt(name: String): Int? = this[name]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
