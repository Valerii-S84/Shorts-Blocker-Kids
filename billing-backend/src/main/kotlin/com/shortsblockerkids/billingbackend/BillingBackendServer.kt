package com.shortsblockerkids.billingbackend

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

class BillingBackendServer(
    private val config: BackendConfig,
    private val store: EntitlementStorage,
    verifierProvider: () -> PlaySubscriptionVerifier,
    private val server: HttpServer,
    private val rateLimiter: RateLimiter = RateLimiter(),
    private val logger: StructuredLogger = StructuredLogger(),
    private val rtdnAuthenticator: RtdnAuthenticator = RtdnAuthenticator(config),
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

    private fun health(
        exchange: HttpExchange,
        context: RequestContext,
    ): Int =
        exchange.respond(
            200,
            JsonBodies.statusResponse("ok"),
            context,
        )

    private fun verify(
        exchange: HttpExchange,
        context: RequestContext,
    ): Int {
        exchange.requireMethod("POST")
        val request = JsonBodies.parseVerificationRequest(exchange.readBody())
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
        val record = request.toEntitlementRecord(verified, purchaseTokenHash)
        store.upsert(record)
        logger.audit(
            "billing.verify.completed",
            context.auditFields(
                "install_id" to record.installId,
                "product_id" to record.productId,
                "state" to record.state.name,
                "acknowledged" to record.acknowledged,
                "purchase_token_hash_prefix" to purchaseTokenHash.prefix(),
            ),
        )
        return exchange.respond(200, JsonBodies.entitlementResponse(record), context)
    }

    private fun entitlementStatus(
        exchange: HttpExchange,
        context: RequestContext,
    ): Int {
        exchange.requireMethod("GET")
        val installId =
            exchange
                .queryParam("install_id")
                ?.takeIf { INSTALL_ID_REGEX.matches(it) }
                ?: throw IllegalArgumentException("Missing install_id")
        return exchange.respond(200, JsonBodies.entitlementResponse(store.findByInstallId(installId)), context)
    }

    private fun rtdn(
        exchange: HttpExchange,
        context: RequestContext,
    ): Int {
        exchange.requireMethod("POST")
        rtdnAuthenticator.authenticate(exchange)
        val notification = JsonBodies.parsePubSubNotification(exchange.readBody())
        require(notification.packageName == config.packageName) { "Unexpected package name" }
        require(notification.productId == config.productId) { "Unexpected product ID" }
        if (store.isRtdnProcessed(notification.messageId)) {
            logger.audit("billing.rtdn.duplicate", context.rtdnAuditFields(notification))
            return exchange.respond(200, JsonBodies.statusResponse("duplicate"), context)
        }

        val verified =
            verifier.verify(
                packageName = config.packageName,
                productId = notification.productId,
                purchaseToken = notification.purchaseToken,
            )
        val status =
            if (store.updateByPurchaseToken(notification.purchaseToken, verified)) {
                "updated"
            } else {
                "verified_without_install_record"
            }
        store.markRtdnProcessed(notification.messageId)
        logger.audit(
            "billing.rtdn.processed",
            context.rtdnAuditFields(
                notification,
                "status" to status,
                "state" to verified.state.name,
                "acknowledged" to verified.acknowledged,
            ),
        )
        return exchange.respond(200, JsonBodies.statusResponse(status), context)
    }

    private fun handle(
        exchange: HttpExchange,
        endpoint: Endpoint,
        block: (HttpExchange, RequestContext) -> Int,
    ) {
        val startedAtMillis = System.currentTimeMillis()
        val context = RequestContext(UUID.randomUUID().toString(), endpoint.path)
        var statusCode = 500
        var failureType: String? = null
        try {
            exchange.requireExactPath(endpoint.path)
            exchange.requireHttpsIfNeeded(endpoint)
            exchange.requireRateLimit(endpoint)
            statusCode = block(exchange, context)
        } catch (exception: TooManyRequestsException) {
            failureType = exception.failureType()
            statusCode = exchange.respond(429, JsonBodies.errorResponse("Rate limit exceeded"), context)
        } catch (exception: RequestTooLargeException) {
            failureType = exception.failureType()
            statusCode = exchange.respond(413, JsonBodies.errorResponse("Request body too large"), context)
        } catch (exception: PurchaseNotFoundException) {
            failureType = exception.failureType()
            statusCode = exchange.respond(404, JsonBodies.errorResponse(exception.message ?: "Purchase not found"), context)
        } catch (exception: InvalidPurchaseTokenException) {
            failureType = exception.failureType()
            statusCode = exchange.respond(400, JsonBodies.errorResponse(exception.message ?: "Invalid purchase token"), context)
        } catch (exception: IllegalArgumentException) {
            failureType = exception.failureType()
            statusCode = exchange.respond(400, JsonBodies.errorResponse(exception.message ?: "Bad request"), context)
        } catch (exception: BillingBackendUnavailableException) {
            failureType = exception.failureType()
            statusCode = exchange.respond(503, JsonBodies.errorResponse("Billing backend temporarily unavailable"), context)
        } catch (exception: IllegalStateException) {
            failureType = exception.failureType()
            statusCode = exchange.respond(503, JsonBodies.errorResponse("Billing backend temporarily unavailable"), context)
        } catch (exception: Exception) {
            failureType = exception.failureType()
            statusCode = exchange.respond(500, JsonBodies.errorResponse("Internal billing backend error"), context)
        } finally {
            logRequest(exchange, context, statusCode, startedAtMillis, failureType)
            exchange.close()
        }
    }

    private fun logRequest(
        exchange: HttpExchange,
        context: RequestContext,
        statusCode: Int,
        startedAtMillis: Long,
        failureType: String?,
    ) {
        val fields =
            context.auditFields(
                "method" to exchange.requestMethod,
                "status" to statusCode,
                "duration_ms" to (System.currentTimeMillis() - startedAtMillis),
                "remote_addr" to exchange.remoteAddress.address.hostAddress,
                "error_type" to failureType,
            )
        if (statusCode >= 500) {
            logger.warn("http.request.completed", fields)
        } else {
            logger.info("http.request.completed", fields)
        }
        if (context.endpoint.startsWith("/billing/") && statusCode >= 400) {
            logger.audit("billing.request.failed", fields)
        }
    }

    private fun Throwable.failureType(): String = javaClass.simpleName.ifBlank { "Exception" }

    private fun HttpExchange.requireMethod(method: String) {
        require(requestMethod == method) { "Expected $method" }
    }

    private fun HttpExchange.requireExactPath(path: String) {
        require(requestURI.path == path) { "Unknown endpoint" }
    }

    private fun HttpExchange.requireHttpsIfNeeded(endpoint: Endpoint) {
        if (!config.requireHttps || endpoint == Endpoint.HEALTH) {
            return
        }
        require(requestHeaders.getFirst("X-Forwarded-Proto") == "https") {
            "HTTPS is required"
        }
    }

    private fun HttpExchange.requireRateLimit(endpoint: Endpoint) {
        val key = "${endpoint.path}:${remoteAddress.address.hostAddress}"
        if (!rateLimiter.allow(key, endpoint.limit(config.rateLimits))) {
            throw TooManyRequestsException()
        }
    }

    private fun HttpExchange.readBody(): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var total = 0
        while (true) {
            val read = requestBody.read(buffer)
            if (read == -1) break
            total += read
            if (total > config.maxRequestBytes) {
                throw RequestTooLargeException()
            }
            output.write(buffer, 0, read)
        }
        return output.toString(StandardCharsets.UTF_8)
    }

    private fun HttpExchange.respond(
        statusCode: Int,
        body: String,
        context: RequestContext,
    ): Int {
        val bytes = body.toByteArray()
        responseHeaders.set("Content-Type", "application/json")
        responseHeaders.set("X-Request-Id", context.requestId)
        sendResponseHeaders(statusCode, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
        return statusCode
    }

    private fun HttpExchange.queryParam(name: String): String? =
        requestURI
            .rawQuery
            .orEmpty()
            .split("&")
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) {
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                } else {
                    null
                }
            }.firstOrNull { it.first == name }
            ?.second

    private fun VerificationRequest.toEntitlementRecord(
        verified: VerifiedSubscription,
        purchaseTokenHash: String,
    ): EntitlementRecord =
        EntitlementRecord(
            installId = installId,
            packageName = packageName,
            productId = productId,
            purchaseTokenHash = purchaseTokenHash,
            state = verified.state,
            activeUntilMillis = verified.activeUntilMillis,
            acknowledged = verified.acknowledged,
            lastVerifiedAtMillis = verified.verifiedAtMillis,
            appVersion = appVersion,
        )

    private fun RequestContext.rtdnAuditFields(
        notification: PubSubNotification,
        vararg fields: Pair<String, Any?>,
    ): Map<String, Any?> =
        auditFields(
            "message_id" to notification.messageId,
            "product_id" to notification.productId,
            "notification_type" to notification.notificationType,
            "purchase_token_hash_prefix" to EntitlementStore.hashPurchaseToken(notification.purchaseToken).prefix(),
            *fields,
        )

    private fun RequestContext.auditFields(vararg fields: Pair<String, Any?>): Map<String, Any?> =
        mapOf(
            "request_id" to requestId,
            "endpoint" to endpoint,
        ) + fields

    private fun String.prefix(): String = take(12)

    companion object {
        const val RTDN_SECRET_HEADER = RtdnAuthenticator.RTDN_SECRET_HEADER

        fun create(
            config: BackendConfig,
            store: EntitlementStorage = EntitlementStorageFactory.create(config),
            verifier: PlaySubscriptionVerifier? = null,
            rateLimiter: RateLimiter = RateLimiter(),
        ): BillingBackendServer {
            val server = HttpServer.create(InetSocketAddress(config.bindAddress, config.port), 0)
            val backend =
                BillingBackendServer(
                    config = config,
                    store = store,
                    verifierProvider = { verifier ?: GooglePlayDeveloperApiClient(GooglePlayAuth.load(config)) },
                    server = server,
                    rateLimiter = rateLimiter,
                )
            server.createContext(Endpoint.HEALTH.path) { backend.handle(it, Endpoint.HEALTH, backend::health) }
            server.createContext(Endpoint.VERIFY.path) { backend.handle(it, Endpoint.VERIFY, backend::verify) }
            server.createContext(Endpoint.RTDN.path) { backend.handle(it, Endpoint.RTDN, backend::rtdn) }
            server.createContext(Endpoint.STATUS.path) { backend.handle(it, Endpoint.STATUS, backend::entitlementStatus) }
            return backend
        }
    }
}

private data class RequestContext(
    val requestId: String,
    val endpoint: String,
)

private enum class Endpoint(
    val path: String,
) {
    HEALTH("/health"),
    VERIFY("/billing/play/verify"),
    RTDN("/billing/play/rtdn"),
    STATUS("/entitlement/status"),
    ;

    fun limit(config: RateLimitConfig): Int =
        when (this) {
            HEALTH -> Int.MAX_VALUE
            VERIFY -> config.verifyPerMinute
            RTDN -> config.rtdnPerMinute
            STATUS -> config.statusPerMinute
        }
}

private class RequestTooLargeException : RuntimeException()

private class TooManyRequestsException : RuntimeException()

private val INSTALL_ID_REGEX = Regex("[A-Za-z0-9_.:-]{1,128}")
