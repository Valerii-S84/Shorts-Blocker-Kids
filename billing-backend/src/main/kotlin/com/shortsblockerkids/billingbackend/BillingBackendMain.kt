package com.shortsblockerkids.billingbackend

fun main() {
    val config = BackendConfig.fromEnvironment(System.getenv())
    val backend = BillingBackendServer.create(config)
    backend.start()
    StructuredLogger().info(
        "backend.started",
        mapOf(
            "port" to config.port,
            "environment" to config.environment.name,
            "storage_backend" to config.storageBackend.name,
        ),
    )
}
