package com.shortsblockerkids.billingbackend

import com.google.api.client.json.webtoken.JsonWebSignature
import com.google.auth.oauth2.TokenVerifier
import com.sun.net.httpserver.HttpExchange

class RtdnAuthenticator(
    private val config: BackendConfig,
    private val verifierFactory: (String) -> TokenVerifier = ::pubSubTokenVerifier,
) {
    fun authenticate(exchange: HttpExchange) {
        if (config.rtdnPubSubAudience != null && config.rtdnPubSubServiceAccountEmail != null) {
            authenticatePubSubJwt(exchange)
            return
        }
        authenticateSharedSecret(exchange)
    }

    private fun authenticatePubSubJwt(exchange: HttpExchange) {
        val token =
            exchange.requestHeaders
                .getFirst("Authorization")
                ?.takeIf { it.startsWith("Bearer ") }
                ?.removePrefix("Bearer ")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Missing RTDN authorization")
        val signature =
            runCatching { verifierFactory(requireNotNull(config.rtdnPubSubAudience)).verify(token) }
                .getOrElse { throw IllegalArgumentException("Invalid RTDN authorization") }
        val email = signature.payloadString("email")
        val emailVerified = signature.payloadString("email_verified")
        require(email == config.rtdnPubSubServiceAccountEmail && emailVerified == "true") {
            "Invalid RTDN source"
        }
    }

    private fun authenticateSharedSecret(exchange: HttpExchange) {
        val expectedSecret =
            config.rtdnSharedSecret
                ?: throw IllegalStateException("RTDN authentication is not configured")
        require(exchange.requestHeaders.getFirst(RTDN_SECRET_HEADER) == expectedSecret) {
            "Invalid RTDN source"
        }
    }

    companion object {
        const val RTDN_SECRET_HEADER = "X-SBK-RTDN-Secret"
    }
}

private fun pubSubTokenVerifier(audience: String): TokenVerifier =
    TokenVerifier
        .newBuilder()
        .setAudience(audience)
        .setIssuer("https://accounts.google.com")
        .setCertificatesLocation("https://www.googleapis.com/oauth2/v3/certs")
        .build()

private fun JsonWebSignature.payloadString(name: String): String? = payload[name]?.toString()
