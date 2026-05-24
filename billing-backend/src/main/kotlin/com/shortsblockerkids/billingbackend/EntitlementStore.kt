package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class EntitlementStore(
    private val path: Path,
) {
    private val records = linkedMapOf<String, EntitlementRecord>()
    private val processedRtdnIds = linkedSetOf<String>()

    init {
        load()
    }

    @Synchronized
    fun upsert(record: EntitlementRecord) {
        records[record.installId] = record
        persist()
    }

    @Synchronized
    fun findByInstallId(installId: String): EntitlementRecord? = records[installId]

    @Synchronized
    fun findByPurchaseTokenHash(purchaseTokenHash: String): EntitlementRecord? =
        records.values.firstOrNull { it.purchaseTokenHash == purchaseTokenHash }

    @Synchronized
    fun markRtdnProcessed(messageId: String): Boolean {
        if (!processedRtdnIds.add(messageId)) {
            return false
        }
        persist()
        return true
    }

    @Synchronized
    fun isRtdnProcessed(messageId: String): Boolean = messageId in processedRtdnIds

    @Synchronized
    fun updateByPurchaseToken(
        purchaseToken: String,
        verified: VerifiedSubscription,
    ): Boolean {
        val hash = hashPurchaseToken(purchaseToken)
        val previous = findByPurchaseTokenHash(hash) ?: return false
        upsert(
            previous.copy(
                state = verified.state,
                activeUntilMillis = verified.activeUntilMillis,
                acknowledged = verified.acknowledged,
                lastVerifiedAtMillis = verified.verifiedAtMillis,
            ),
        )
        return true
    }

    private fun load() {
        if (!Files.isRegularFile(path)) {
            return
        }
        val text = Files.readString(path)
        if (text.isBlank()) {
            return
        }
        val root = JsonBodies.json.parseToJsonElement(text).jsonObject
        processedRtdnIds.addAll(
            (root["processed_rtdn_ids"] as? JsonArray)
                ?.mapNotNull { it.jsonPrimitive.content }
                .orEmpty(),
        )
        (root["entitlements"] as? JsonArray)
            ?.mapNotNull(::recordFromJson)
            ?.forEach { records[it.installId] = it }
    }

    private fun recordFromJson(element: kotlinx.serialization.json.JsonElement): EntitlementRecord? {
        val item = element.jsonObject
        val installId = item.optionalString("install_id") ?: return null
        val packageName = item.optionalString("package_name") ?: return null
        val productId = item.optionalString("product_id") ?: return null
        val tokenHash = item.optionalString("purchase_token_hash") ?: return null
        val state =
            item
                .optionalString("state")
                ?.let { runCatching { SubscriptionEntitlementState.valueOf(it) }.getOrNull() }
                ?: SubscriptionEntitlementState.UNKNOWN
        val checkedAt = item["last_verified_at_millis"]?.jsonPrimitive?.longOrNull ?: return null
        return EntitlementRecord(
            installId = installId,
            packageName = packageName,
            productId = productId,
            purchaseTokenHash = tokenHash,
            state = state,
            activeUntilMillis = item["active_until_millis"]?.jsonPrimitive?.longOrNull,
            acknowledged = item.optionalBoolean("acknowledged") ?: false,
            lastVerifiedAtMillis = checkedAt,
            appVersion = item.optionalString("app_version"),
        )
    }

    private fun persist() {
        path.parent?.let(Files::createDirectories)
        val root =
            JsonObject(
                mapOf(
                    "processed_rtdn_ids" to JsonArray(processedRtdnIds.map(::JsonPrimitive)),
                    "entitlements" to JsonArray(records.values.map(::recordToJson)),
                ),
            )
        Files.writeString(path, JsonBodies.json.encodeToString(JsonObject.serializer(), root))
    }

    private fun recordToJson(record: EntitlementRecord): JsonObject =
        JsonObject(
            mapOf(
                "install_id" to JsonPrimitive(record.installId),
                "package_name" to JsonPrimitive(record.packageName),
                "product_id" to JsonPrimitive(record.productId),
                "purchase_token_hash" to JsonPrimitive(record.purchaseTokenHash),
                "state" to JsonPrimitive(record.state.name),
                "active_until_millis" to longPrimitive(record.activeUntilMillis),
                "acknowledged" to JsonPrimitive(record.acknowledged),
                "last_verified_at_millis" to JsonPrimitive(record.lastVerifiedAtMillis),
                "app_version" to nullableStringPrimitive(record.appVersion),
            ),
        )

    private fun longPrimitive(value: Long?) = value?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull

    private fun nullableStringPrimitive(value: String?) = value?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull

    companion object {
        fun hashPurchaseToken(purchaseToken: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(purchaseToken.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
