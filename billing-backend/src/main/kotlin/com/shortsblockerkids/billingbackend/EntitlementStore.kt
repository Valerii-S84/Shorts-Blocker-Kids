package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

interface EntitlementStorage {
    fun createInstallation(
        installId: String,
        bearerHash: String,
    ): Boolean

    fun installationBearerHash(installId: String): String?

    fun upsert(record: EntitlementRecord)

    fun findByInstallId(installId: String): EntitlementRecord?

    fun findByPurchaseTokenHash(purchaseTokenHash: String): EntitlementRecord?

    fun markRtdnProcessed(messageId: String): Boolean

    fun isRtdnProcessed(messageId: String): Boolean

    fun applyRtdn(
        messageId: String,
        purchaseToken: String,
        verified: VerifiedSubscription,
    ): RtdnApplyResult

    fun updateByPurchaseToken(
        purchaseToken: String,
        verified: VerifiedSubscription,
    ): Boolean

    fun processRtdn(messageId: String, purchaseToken: String, verify: () -> VerifiedSubscription): RtdnApplyResult
}

enum class RtdnApplyResult {
    DUPLICATE,
    UPDATED,
    NO_INSTALL_RECORD,
    STALE,
}

class EntitlementStore(
    private val path: Path,
    private val replaceFile: (Path, Path) -> Unit = { source, target ->
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        Unit
    },
) : EntitlementStorage {
    private var records: Map<String, EntitlementRecord> = emptyMap()
    private var processedRtdnIds: Set<String> = emptySet()
    private var installationAuthorizations: Map<String, String> = emptyMap()

    init {
        load()
    }

    @Synchronized
    override fun createInstallation(
        installId: String,
        bearerHash: String,
    ): Boolean {
        if (installId in records || installId in installationAuthorizations) {
            return false
        }
        val nextAuthorizations = installationAuthorizations + (installId to bearerHash)
        persist(authorizations = nextAuthorizations)
        installationAuthorizations = nextAuthorizations
        return true
    }

    @Synchronized
    override fun installationBearerHash(installId: String): String? = installationAuthorizations[installId]

    @Synchronized
    override fun upsert(record: EntitlementRecord) {
        val previous = records[record.installId]
        if (previous != null && record.lastVerifiedAtMillis < previous.lastVerifiedAtMillis) return
        val nextRecords = records + (record.installId to record)
        persist(entitlements = nextRecords)
        records = nextRecords
    }

    @Synchronized
    override fun findByInstallId(installId: String): EntitlementRecord? = records[installId]

    @Synchronized
    override fun findByPurchaseTokenHash(purchaseTokenHash: String): EntitlementRecord? =
        records.values.firstOrNull { it.purchaseTokenHash == purchaseTokenHash }

    @Synchronized
    override fun markRtdnProcessed(messageId: String): Boolean {
        if (messageId in processedRtdnIds) {
            return false
        }
        val nextIds = processedRtdnIds + messageId
        persist(messageIds = nextIds)
        processedRtdnIds = nextIds
        return true
    }

    @Synchronized
    override fun isRtdnProcessed(messageId: String): Boolean = messageId in processedRtdnIds

    @Synchronized
    override fun applyRtdn(
        messageId: String,
        purchaseToken: String,
        verified: VerifiedSubscription,
    ): RtdnApplyResult {
        if (messageId in processedRtdnIds) return RtdnApplyResult.DUPLICATE
        val previous = findByPurchaseTokenHash(hashPurchaseToken(purchaseToken))
        val stale = previous != null && verified.verifiedAtMillis < previous.lastVerifiedAtMillis
        val nextRecords =
            if (previous == null || stale) {
                records
            } else {
                records + (previous.installId to previous.withVerification(verified))
            }
        val nextIds = processedRtdnIds + messageId
        persist(entitlements = nextRecords, messageIds = nextIds)
        records = nextRecords
        processedRtdnIds = nextIds
        return when {
            previous == null -> RtdnApplyResult.NO_INSTALL_RECORD
            stale -> RtdnApplyResult.STALE
            else -> RtdnApplyResult.UPDATED
        }
    }

    @Synchronized
    override fun processRtdn(messageId: String, purchaseToken: String, verify: () -> VerifiedSubscription): RtdnApplyResult {
        if (messageId in processedRtdnIds) return RtdnApplyResult.DUPLICATE
        return applyRtdn(messageId, purchaseToken, verify())
    }

    @Synchronized
    override fun updateByPurchaseToken(
        purchaseToken: String,
        verified: VerifiedSubscription,
    ): Boolean {
        val hash = hashPurchaseToken(purchaseToken)
        val previous = findByPurchaseTokenHash(hash) ?: return false
        if (verified.verifiedAtMillis < previous.lastVerifiedAtMillis) return false
        upsert(previous.withVerification(verified))
        return true
    }

    private fun EntitlementRecord.withVerification(verified: VerifiedSubscription): EntitlementRecord =
        copy(
            state = verified.state,
            activeUntilMillis = verified.activeUntilMillis,
            acknowledged = verified.acknowledged,
            lastVerifiedAtMillis = verified.verifiedAtMillis,
        )

    private fun load() {
        if (!Files.isRegularFile(path)) {
            return
        }
        val text = Files.readString(path)
        if (text.isBlank()) {
            return
        }
        try {
            val root = JsonBodies.json.parseToJsonElement(text).jsonObject
            val loadedAuthorizations =
                root["installation_authorizations"]
                    ?.jsonObject
                    ?.mapValues { (_, value) ->
                        require(value.jsonPrimitive.isString) { "Invalid installation authorization" }
                        value.jsonPrimitive.content
                    }.orEmpty()
            val loadedIds =
                root["processed_rtdn_ids"]
                    ?.jsonArray
                    ?.map { value ->
                        require(value.jsonPrimitive.isString) { "Invalid RTDN message ID" }
                        value.jsonPrimitive.content
                    }.orEmpty()
                    .toSet()
            val loadedRecords =
                root["entitlements"]
                    ?.jsonArray
                    ?.map { element ->
                        requireNotNull(recordFromJson(element)) { "Invalid entitlement record" }
                    }.orEmpty()
                    .associateBy { it.installId }
            installationAuthorizations = loadedAuthorizations
            processedRtdnIds = loadedIds
            records = loadedRecords
        } catch (exception: IllegalArgumentException) {
            throw IllegalStateException(
                "Cannot load entitlement store $path; original file preserved. Restore a valid store before starting.",
                exception,
            )
        }
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

    private fun persist(
        entitlements: Map<String, EntitlementRecord> = records,
        messageIds: Set<String> = processedRtdnIds,
        authorizations: Map<String, String> = installationAuthorizations,
    ) {
        val target = path.toAbsolutePath()
        Files.createDirectories(target.parent)
        val root =
            JsonObject(
                mapOf(
                    "installation_authorizations" to
                        JsonObject(authorizations.mapValues { JsonPrimitive(it.value) }),
                    "processed_rtdn_ids" to JsonArray(messageIds.map(::JsonPrimitive)),
                    "entitlements" to JsonArray(entitlements.values.map(::recordToJson)),
                ),
            )
        val temporary = Files.createTempFile(target.parent, ".${target.fileName}.", ".tmp")
        try {
            Files.writeString(temporary, JsonBodies.json.encodeToString(JsonObject.serializer(), root))
            replaceFile(temporary, target)
        } finally {
            Files.deleteIfExists(temporary)
        }
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
