package com.shortsblockerkids.core.billing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
                JSONObject()
                    .put("install_id", request.installId)
                    .put("package_name", request.packageName)
                    .put("product_id", request.productId)
                    .put("purchase_token", request.purchaseToken)
                    .put("app_version", request.appVersion)
                    .toString()
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
    ): JSONObject {
        val connection = openConnection(path)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toByteArray()) }
        return readJson(connection)
    }

    private fun getJson(path: String): JSONObject {
        val connection = openConnection(path)
        connection.requestMethod = "GET"
        return readJson(connection)
    }

    private fun openConnection(path: String): HttpURLConnection =
        (URI("${baseUrl.trimEnd('/')}$path").toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }

    private fun readJson(connection: HttpURLConnection): JSONObject {
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
        return JSONObject(body)
    }

    private fun parseEntitlement(json: JSONObject): BillingEntitlementSnapshot {
        val state =
            runCatching {
                BillingEntitlementState.valueOf(json.optString("state", "UNKNOWN"))
            }.getOrDefault(BillingEntitlementState.UNKNOWN)
        val checkedAtMillis =
            json
                .takeIf { it.has("checked_at_millis") && !it.isNull("checked_at_millis") }
                ?.optLong("checked_at_millis")
                ?: nowMillis()
        val activeUntilMillis =
            json
                .takeIf { it.has("active_until_millis") && !it.isNull("active_until_millis") }
                ?.optLong("active_until_millis")
        return BillingEntitlementSnapshot(
            state = state,
            checkedAtMillis = checkedAtMillis,
            activeUntilMillis = activeUntilMillis,
        )
    }

    companion object {
        fun fromBaseUrl(baseUrl: String): BillingBackendClient =
            if (baseUrl.isBlank()) {
                DisabledBillingBackendClient
            } else {
                HttpBillingBackendClient(baseUrl)
            }
    }
}
