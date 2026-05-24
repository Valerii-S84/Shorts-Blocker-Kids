package com.shortsblockerkids.billingbackend

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

class BillingBackendServer(
    private val config: BackendConfig,
    private val store: EntitlementStore,
    verifierProvider: () -> PlaySubscriptionVerifier,
    private val server: HttpServer,
) {
    private val verifier: PlaySubscriptionVerifier by lazy(verifierProvider)

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop(0)
    }

    val port: Int
        get() = server.address.port

    private fun health(exchange: HttpExchange) {
        exchange.respond(200, JsonBodies.statusResponse("ok"))
    }

    private fun verify(exchange: HttpExchange) {
        exchange.requireMethod("POST")
        val request = JsonBodies.parseVerificationRequest(exchange.requestBody.bufferedReader().readText())
        require(request.packageName == config.packageName) { "Unexpected package name" }
        require(request.productId == config.productId) { "Unexpected product ID" }

        val purchaseTokenHash = EntitlementStore.hashPurchaseToken(request.purchaseToken)
        val existingRecord = store.findByPurchaseTokenHash(purchaseTokenHash)
        require(existingRecord == null || existingRecord.installId == request.installId) {
            "Purchase token already belongs to another installation"
        }

        val verified =
            verifier.verify(
                packageName = request.packageName,
                productId = request.productId,
                purchaseToken = request.purchaseToken,
            )
        val record =
            EntitlementRecord(
                installId = request.installId,
                packageName = request.packageName,
                productId = request.productId,
                purchaseTokenHash = purchaseTokenHash,
                state = verified.state,
                activeUntilMillis = verified.activeUntilMillis,
                acknowledged = verified.acknowledged,
                lastVerifiedAtMillis = verified.verifiedAtMillis,
                appVersion = request.appVersion,
            )
        store.upsert(record)
        exchange.respond(200, JsonBodies.entitlementResponse(record))
    }

    private fun entitlementStatus(exchange: HttpExchange) {
        exchange.requireMethod("GET")
        val installId =
            exchange
                .requestURI
                .rawQuery
                .orEmpty()
                .split("&")
                .mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.firstOrNull { it.first == "install_id" }
                ?.second
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Missing install_id")
        exchange.respond(200, JsonBodies.entitlementResponse(store.findByInstallId(installId)))
    }

    private fun rtdn(exchange: HttpExchange) {
        exchange.requireMethod("POST")
        val expectedSecret =
            config.rtdnSharedSecret ?: throw IllegalStateException("RTDN shared secret is not configured")
        require(exchange.requestHeaders.getFirst(RTDN_SECRET_HEADER) == expectedSecret) {
            "Invalid RTDN source"
        }

        val notification = JsonBodies.parsePubSubNotification(exchange.requestBody.bufferedReader().readText())
        require(notification.productId == config.productId) { "Unexpected product ID" }
        if (store.isRtdnProcessed(notification.messageId)) {
            exchange.respond(200, JsonBodies.statusResponse("duplicate"))
            return
        }

        val verified =
            verifier.verify(
                packageName = config.packageName,
                productId = notification.productId,
                purchaseToken = notification.purchaseToken,
            )
        store.markRtdnProcessed(notification.messageId)
        val status =
            if (store.updateByPurchaseToken(notification.purchaseToken, verified)) {
                "updated"
            } else {
                "verified_without_install_record"
            }
        exchange.respond(200, JsonBodies.statusResponse(status))
    }

    private fun handle(
        exchange: HttpExchange,
        block: (HttpExchange) -> Unit,
    ) {
        try {
            block(exchange)
        } catch (exception: IllegalArgumentException) {
            exchange.respond(400, JsonBodies.errorResponse(exception.message ?: "Bad request"))
        } catch (exception: IllegalStateException) {
            exchange.respond(503, JsonBodies.errorResponse("Billing backend temporarily unavailable"))
        } catch (exception: Exception) {
            exchange.respond(500, JsonBodies.errorResponse("Internal billing backend error"))
        } finally {
            exchange.close()
        }
    }

    private fun HttpExchange.requireMethod(method: String) {
        require(requestMethod == method) { "Expected $method" }
    }

    private fun HttpExchange.respond(
        statusCode: Int,
        body: String,
    ) {
        val bytes = body.toByteArray()
        responseHeaders.set("Content-Type", "application/json")
        sendResponseHeaders(statusCode, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    companion object {
        const val RTDN_SECRET_HEADER = "X-SBK-RTDN-Secret"

        fun create(
            config: BackendConfig,
            store: EntitlementStore = EntitlementStore(config.storeFile),
            verifier: PlaySubscriptionVerifier? = null,
        ): BillingBackendServer {
            val server = HttpServer.create(InetSocketAddress(config.port), 0)
            val backend =
                BillingBackendServer(
                    config = config,
                    store = store,
                    verifierProvider = { verifier ?: GooglePlayDeveloperApiClient() },
                    server = server,
                )
            server.createContext("/health") { backend.handle(it, backend::health) }
            server.createContext("/billing/play/verify") { backend.handle(it, backend::verify) }
            server.createContext("/billing/play/rtdn") { backend.handle(it, backend::rtdn) }
            server.createContext("/entitlement/status") { backend.handle(it, backend::entitlementStatus) }
            return backend
        }
    }
}
