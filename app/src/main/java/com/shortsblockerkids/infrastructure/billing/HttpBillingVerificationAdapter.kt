package com.shortsblockerkids.infrastructure.billing

import com.shortsblockerkids.application.billing.BillingVerificationPort
import com.shortsblockerkids.application.billing.BillingVerificationRequest
import com.shortsblockerkids.application.billing.DisabledBillingVerificationPort
import com.shortsblockerkids.domain.entitlement.BillingEntitlementSnapshot
import com.shortsblockerkids.domain.entitlement.BillingEntitlementState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

class HttpBillingVerificationAdapter(
    private val baseUrl: String,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val installationStore: BillingInstallationStore,
) : BillingVerificationPort {
    override val isConfigured: Boolean = baseUrl.isNotBlank()
    private val registrationMutex = Mutex()

    override suspend fun verifyPurchase(request: BillingVerificationRequest): BillingEntitlementSnapshot =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                throw IllegalStateException("Billing backend is not configured.")
            }
            val installation = installation()
            val body =
                JsonObject(
                    mapOf(
                        "install_id" to JsonPrimitive(installation.installId),
                        "package_name" to JsonPrimitive(request.packageName),
                        "product_id" to JsonPrimitive(request.productId),
                        "purchase_token" to JsonPrimitive(request.purchaseToken),
                        "app_version" to JsonPrimitive(request.appVersion),
                    ),
                ).toString()
            val response = postJson("/billing/play/verify", body, installation)
            parseEntitlement(response)
        }

    override suspend fun refreshEntitlement(installId: String): BillingEntitlementSnapshot? =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                return@withContext null
            }
            val installation = installation()
            val encodedInstallId = URLEncoder.encode(installation.installId, Charsets.UTF_8.name())
            val response = getJson("/entitlement/status?install_id=$encodedInstallId", installation)
            parseEntitlement(response)
        }

    private suspend fun installation(): BillingInstallation =
        registrationMutex.withLock {
            installationStore.read() ?: run {
                val response = postJson("/billing/installations", "{}")
                val installation =
                    BillingInstallation(
                        installId = requireNotNull(response["install_id"]).jsonPrimitive.content,
                        bearerToken = requireNotNull(response["bearer_token"]).jsonPrimitive.content,
                    )
                installationStore.write(installation)
                installation
            }
        }

    private fun postJson(
        path: String,
        body: String,
        installation: BillingInstallation? = null,
    ): JsonObject {
        val connection = openConnection(path, installation)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toByteArray()) }
        return readJson(connection)
    }

    private fun getJson(path: String, installation: BillingInstallation): JsonObject {
        val connection = openConnection(path, installation)
        connection.requestMethod = "GET"
        return readJson(connection)
    }

    private fun openConnection(path: String, installation: BillingInstallation?): HttpURLConnection =
        (URI("${baseUrl.trimEnd('/')}$path").toURL().openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            installation?.let { setRequestProperty("Authorization", "Bearer ${it.bearerToken}") }
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

        fun fromBaseUrl(baseUrl: String, installationStore: BillingInstallationStore): BillingVerificationPort =
            if (baseUrl.isBlank()) {
                DisabledBillingVerificationPort
            } else {
                HttpBillingVerificationAdapter(baseUrl, installationStore = installationStore)
            }
    }
}
