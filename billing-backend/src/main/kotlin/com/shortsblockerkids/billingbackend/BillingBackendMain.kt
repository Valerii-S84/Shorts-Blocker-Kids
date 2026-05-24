package com.shortsblockerkids.billingbackend

fun main() {
    val config = BackendConfig.fromEnvironment(System.getenv())
    val backend = BillingBackendServer.create(config)
    backend.start()
    println("Shorts Blocker Kids billing backend listening on port ${config.port}")
}
