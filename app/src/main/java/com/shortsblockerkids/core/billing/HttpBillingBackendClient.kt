package com.shortsblockerkids.core.billing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

class HttpBillingBackendClient(
    private val baseUrl: String,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : BillingBackendClient {
    override val isConfigured: Boolean = baseUrl.isNotBlank()

    override suspend fun verifyPurchase(request: BillingBackendPurchaseRequest): BillingEntitlementSnapshot =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                throw IllegalStateException("Billing backend is not configured.")
            }
            val body =
                JsonObject(
                    mapOf(
                        "install_id" to JsonPrimitive(request.installId),
                        "package_name" to JsonPrimitive(request.packageName),
                        "product_id" to JsonPrimitive(request.productId),
                        "purchase_token" to JsonPrimitive(request.purchaseToken),
                        "app_version" to JsonPrimitive(request.appVersion),
                    ),
                ).toString()
            val response = postJson("/billing/play/verify", body)
            parseEntitlement(response)
        }

    override suspend fun refreshEntitlement(installId: String): BillingEntitlementSnapshot? =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                return@withContext null
            }
            val encodedInstallId = URLEncoder.encode(installId, Charsets.UTF_8.name())
            val response = getJson("/entitlement/status?install_id=$encodedInstallId")
            parseEntitlement(response)
        }

    private fun postJson(
        path: String,
        body: String,
    ): JsonObject {
        val connection = openConnection(path)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toByteArray()) }
        return readJson(connection)
    }

    private fun getJson(path: String): JsonObject {
        val connection = openConnection(path)
        connection.requestMethod = "GET"
        return readJson(connection)
    }

    private fun openConnection(path: String): HttpURLConnection =
        (URI("${baseUrl.trimEnd('/')}$path").toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }

    private fun readJson(connection: HttpURLConnection): JsonObject {
        val code = connection.responseCode
        val body =
            if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
            }
        if (code !in 200..299) {
            throw IllegalStateException("Billing backend returned HTTP $code.")
        }
        return json.parseToJsonElement(body).jsonObject
    }

    private fun parseEntitlement(json: JsonObject): BillingEntitlementSnapshot {
        val state =
            runCatching {
                BillingEntitlementState.valueOf(
                    json["state"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN",
                )
            }.getOrDefault(BillingEntitlementState.UNKNOWN)
        val checkedAtMillis =
            json["checked_at_millis"]
                ?.jsonPrimitive
                ?.longOrNull
                ?: nowMillis()
        val activeUntilMillis =
            json["active_until_millis"]
                ?.jsonPrimitive
                ?.longOrNull
        return BillingEntitlementSnapshot(
            state = state,
            checkedAtMillis = checkedAtMillis,
            activeUntilMillis = activeUntilMillis,
        )
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromBaseUrl(baseUrl: String): BillingBackendClient =
            if (baseUrl.isBlank()) {
                DisabledBillingBackendClient
            } else {
                HttpBillingBackendClient(baseUrl)
            }
    }
}
