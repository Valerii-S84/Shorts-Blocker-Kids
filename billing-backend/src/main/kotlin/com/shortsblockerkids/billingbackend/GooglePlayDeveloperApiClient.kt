package com.shortsblockerkids.billingbackend

import com.google.auth.oauth2.GoogleCredentials
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.Instant

interface PlaySubscriptionVerifier {
    fun verify(
        packageName: String,
        productId: String,
        purchaseToken: String,
    ): VerifiedSubscription
}

class GooglePlayDeveloperApiClient(
    private val credentials: GoogleCredentials =
        GoogleCredentials
            .getApplicationDefault()
            .createScoped(listOf(ANDROID_PUBLISHER_SCOPE)),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : PlaySubscriptionVerifier {
    override fun verify(
        packageName: String,
        productId: String,
        purchaseToken: String,
    ): VerifiedSubscription {
        val accessToken =
            runCatching { accessToken() }
                .getOrElse { throw BillingBackendUnavailableException("Google Play credentials unavailable") }
        val response =
            getJson(
                url =
                    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/" +
                        "${urlEncode(packageName)}/purchases/subscriptionsv2/tokens/" +
                        urlEncode(purchaseToken),
                accessToken = accessToken,
            )
        val subscription =
            GooglePlaySubscriptionParser.parse(
                response = response,
                productId = productId,
                purchaseToken = purchaseToken,
                nowMillis = nowMillis(),
            )
        if (subscription.state.allowsProtection() && !subscription.acknowledged) {
            acknowledge(packageName, productId, purchaseToken, accessToken)
            return subscription.copy(acknowledged = true)
        }
        return subscription
    }

    private fun acknowledge(
        packageName: String,
        productId: String,
        purchaseToken: String,
        accessToken: String,
    ) {
        val url =
            "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/" +
                "${urlEncode(packageName)}/purchases/subscriptions/${urlEncode(productId)}/tokens/" +
                "${urlEncode(purchaseToken)}:acknowledge"
        val connection = openConnection(url, accessToken)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write("{}".toByteArray()) }
        val code = connection.responseCode
        if (code !in 200..299) {
            throw BillingBackendUnavailableException("Google Play acknowledge failed with HTTP $code")
        }
    }

    private fun getJson(
        url: String,
        accessToken: String,
    ): JsonObject {
        val connection = openConnection(url, accessToken)
        connection.requestMethod = "GET"
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
            when (code) {
                HttpURLConnection.HTTP_BAD_REQUEST -> throw InvalidPurchaseTokenException()
                HttpURLConnection.HTTP_NOT_FOUND -> throw PurchaseNotFoundException()
                else -> throw BillingBackendUnavailableException("Google Play verification failed with HTTP $code")
            }
        }
        return JsonBodies.json.parseToJsonElement(body).jsonObject
    }

    private fun openConnection(
        url: String,
        accessToken: String,
    ): HttpURLConnection =
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

    private fun accessToken(): String {
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher"
    }
}

internal object GooglePlaySubscriptionParser {
    fun parse(
        response: JsonObject,
        productId: String,
        purchaseToken: String,
        nowMillis: Long,
    ): VerifiedSubscription {
        val lineItem =
            response
                .optionalArray("lineItems")
                ?.firstOrNull { item ->
                    item.jsonObject.optionalString("productId") == productId
                }?.jsonObject
                ?: throw InvalidPurchaseTokenException()
        val activeUntilMillis =
            lineItem
                .optionalString("expiryTime")
                ?.let { Instant.parse(it).toEpochMilli() }
        val autoRenewEnabled =
            lineItem
                .optionalObject("autoRenewingPlan")
                ?.optionalBoolean("autoRenewEnabled")
        return VerifiedSubscription(
            productId = productId,
            purchaseToken = purchaseToken,
            state =
                mapGoogleState(
                    rawState = response.optionalString("subscriptionState"),
                    activeUntilMillis = activeUntilMillis,
                    autoRenewEnabled = autoRenewEnabled,
                    nowMillis = nowMillis,
                ),
            activeUntilMillis = activeUntilMillis,
            acknowledged =
                response.optionalString("acknowledgementState") ==
                    "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
            verifiedAtMillis = nowMillis,
        )
    }

    private fun mapGoogleState(
        rawState: String?,
        activeUntilMillis: Long?,
        autoRenewEnabled: Boolean?,
        nowMillis: Long,
    ): SubscriptionEntitlementState =
        when (rawState) {
            "SUBSCRIPTION_STATE_ACTIVE" ->
                if (autoRenewEnabled == false && activeUntilMillis.isFuture(nowMillis)) {
                    SubscriptionEntitlementState.CANCELED_ACTIVE
                } else {
                    SubscriptionEntitlementState.ACTIVE
                }

            "SUBSCRIPTION_STATE_IN_GRACE_PERIOD" -> SubscriptionEntitlementState.IN_GRACE
            "SUBSCRIPTION_STATE_PENDING" -> SubscriptionEntitlementState.PENDING
            "SUBSCRIPTION_STATE_ON_HOLD",
            "SUBSCRIPTION_STATE_PAUSED",
            -> SubscriptionEntitlementState.ON_HOLD

            "SUBSCRIPTION_STATE_CANCELED" ->
                if (activeUntilMillis.isFuture(nowMillis)) {
                    SubscriptionEntitlementState.CANCELED_ACTIVE
                } else {
                    SubscriptionEntitlementState.EXPIRED
                }

            "SUBSCRIPTION_STATE_EXPIRED" -> SubscriptionEntitlementState.EXPIRED
            else -> SubscriptionEntitlementState.UNKNOWN
        }

    private fun Long?.isFuture(nowMillis: Long): Boolean = this != null && this > nowMillis
}
