package com.shortsblockerkids.infrastructure.billing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// These credentials are independent of the legacy, unauthenticated settings UUID.
data class BillingInstallation(
    val installId: String,
    val bearerToken: String,
) {
    init {
        require(INSTALL_ID.matches(installId)) { "Invalid billing installation identity." }
        require(BEARER_TOKEN.matches(bearerToken)) { "Invalid billing installation authorization." }
    }

    private companion object {
        val INSTALL_ID = Regex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")
        val BEARER_TOKEN = Regex("[A-Za-z0-9_-]{43}")
    }
}

interface BillingInstallationStore {
    fun read(): BillingInstallation?

    fun write(installation: BillingInstallation)
}

class MemoryBillingInstallationStore(
    private var installation: BillingInstallation? = null,
) : BillingInstallationStore {
    @Synchronized
    override fun read(): BillingInstallation? = installation

    @Synchronized
    override fun write(installation: BillingInstallation) {
        this.installation = installation
    }
}

// Supply Context.noBackupFilesDir so device backup cannot copy installation ownership.
class AndroidBillingInstallationStore(
    noBackupFilesDir: File,
) : BillingInstallationStore {
    private val path = File(noBackupFilesDir, "billing-installation.json").toPath()

    @Synchronized
    override fun read(): BillingInstallation? {
        if (!Files.exists(path)) {
            return null
        }
        val json = Json.parseToJsonElement(String(Files.readAllBytes(path), Charsets.UTF_8)).jsonObject
        return BillingInstallation(
            installId = requireNotNull(json["install_id"]).jsonPrimitive.content,
            bearerToken = requireNotNull(json["bearer_token"]).jsonPrimitive.content,
        )
    }

    @Synchronized
    override fun write(installation: BillingInstallation) {
        Files.createDirectories(path.parent)
        val temporaryPath = path.resolveSibling("billing-installation.json.tmp")
        val body =
            JsonObject(
                mapOf(
                    "install_id" to JsonPrimitive(installation.installId),
                    "bearer_token" to JsonPrimitive(installation.bearerToken),
                ),
            ).toString()
        Files.write(temporaryPath, body.toByteArray(Charsets.UTF_8))
        try {
            Files.move(temporaryPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
