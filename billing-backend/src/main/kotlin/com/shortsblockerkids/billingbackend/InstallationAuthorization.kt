package com.shortsblockerkids.billingbackend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class InstallationAuthorization(
    private val store: EntitlementStorage,
) {
    private val random = SecureRandom()

    fun register(): InstallationRegistration {
        repeat(MAX_COLLISION_ATTEMPTS) {
            val registration =
                InstallationRegistration(
                    installId = UUID.randomUUID().toString(),
                    bearerToken = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes)),
                )
            if (store.createInstallation(registration.installId, hashBearer(registration.bearerToken))) {
                return registration
            }
        }
        throw IllegalStateException("Unable to allocate installation identity")
    }

    fun authenticate(
        installId: String,
        authorizationHeader: String?,
    ): Boolean {
        val bearer = authorizationHeader?.takeIf { BEARER_HEADER.matches(it) }?.removePrefix("Bearer ") ?: return false
        val expectedHash = store.installationBearerHash(installId) ?: return false
        return MessageDigest.isEqual(expectedHash.toByteArray(Charsets.US_ASCII), hashBearer(bearer).toByteArray(Charsets.US_ASCII))
    }

    private fun hashBearer(bearer: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bearer.toByteArray(Charsets.US_ASCII))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_COLLISION_ATTEMPTS = 3
        val BEARER_HEADER = Regex("Bearer [A-Za-z0-9_-]{43}")
    }
}

data class InstallationRegistration(
    val installId: String,
    val bearerToken: String,
) {
    fun toJson(): String =
        JsonBodies.json.encodeToString(
            JsonObject.serializer(),
            JsonObject(mapOf("install_id" to JsonPrimitive(installId), "bearer_token" to JsonPrimitive(bearerToken))),
        )
}
