package com.shortsblockerkids.billingbackend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyRequestBoundaryTest {
    private val boundary = ProxyRequestBoundary(setOf("192.0.2.10"))

    @Test
    fun directHttpHasNoSecureTransportClaim() {
        assertEquals(RequestOrigin("198.51.100.1", false), boundary.resolve("198.51.100.1", emptyMap()))
    }

    @Test
    fun untrustedPeerCannotSpoofHttpsOrClientIdentity() {
        for (headers in listOf(forwarded("198.51.100.1"), mapOf("X-Forwarded-Proto" to listOf("https")))) {
            assertRejected("198.51.100.99", headers)
        }
    }

    @Test
    fun untrustedPeerCannotSupplyOtherForwardingHeaders() {
        assertRejected("198.51.100.99", mapOf("Forwarded" to listOf("for=198.51.100.1;proto=https")))
        assertRejected("198.51.100.99", mapOf("X-Forwarded-Host" to listOf("billing.example")))
    }

    @Test
    fun explicitlyTrustedProxySuppliesClientIdentityAndHttps() {
        assertEquals(RequestOrigin("198.51.100.1", true), boundary.resolve("192.0.2.10", forwarded("198.51.100.1")))
    }

    @Test
    fun trustedProxyCannotPromotePlaintextIngressToHttps() {
        val headers = forwarded("198.51.100.1") + ("X-Forwarded-Proto" to listOf("http"))

        assertFalse(boundary.resolve("192.0.2.10", headers).secure)
    }

    @Test
    fun trustedProxyMustSupplyBothHeadersExactlyOnce() {
        assertRejected("192.0.2.10", emptyMap())
        assertRejected("192.0.2.10", mapOf("X-Forwarded-For" to listOf("198.51.100.1")))
        assertRejected("192.0.2.10", forwarded("198.51.100.1") + ("X-Forwarded-For" to listOf("198.51.100.1", "198.51.100.2")))
        assertRejected("192.0.2.10", forwarded("198.51.100.1") + ("X-Forwarded-Proto" to listOf("https", "https")))
    }

    @Test
    fun duplicateHeaderNamesWithDifferentCaseAreRejected() {
        assertRejected("192.0.2.10", forwarded("198.51.100.1") + ("x-forwarded-for" to listOf("198.51.100.2")))
    }

    @Test
    fun forwardedChainsNamesPortsAndAmbiguousNumericAddressesAreRejected() {
        val invalidAddresses = listOf(
            "198.51.100.1, 198.51.100.2", "client.example", "127.1", "010.0.0.1",
            "198.51.100.1:80", "256.0.0.1", "fe80::1%eth0", "1:::2",
        )
        for (clientAddress in invalidAddresses) {
            assertRejected("192.0.2.10", forwarded(clientAddress))
        }
    }

    @Test
    fun forwardedProtocolChainsAndUnsupportedForwardedHeaderAreRejected() {
        assertRejected("192.0.2.10", forwarded("198.51.100.1") + ("X-Forwarded-Proto" to listOf("https,http")))
        assertRejected("192.0.2.10", forwarded("198.51.100.1") + ("Forwarded" to listOf("for=198.51.100.1")))
    }

    @Test
    fun ipv6ProxyAndClientAreCanonicalizedWithoutNameResolution() {
        val ipv6Boundary = ProxyRequestBoundary(setOf("2001:db8::10"))
        val origin = ipv6Boundary.resolve("2001:db8:0:0:0:0:0:10", forwarded("2001:DB8::1"))

        assertEquals(RequestOrigin("2001:db8:0:0:0:0:0:1", true), origin)
    }

    @Test
    fun ipv4MappedIpv6CannotCreateASecondClientIdentity() {
        val ipv4 = boundary.resolve("192.0.2.10", forwarded("198.51.100.1"))
        val mapped = boundary.resolve("192.0.2.10", forwarded("::ffff:198.51.100.1"))

        assertEquals(ipv4, mapped)
    }

    @Test
    fun twoClientsBehindOneTrustedProxyHaveIndependentPreHandlerQuotas() {
        val limiter = RateLimiter { 0L }
        val firstClient = boundary.resolve("192.0.2.10", forwarded("198.51.100.1"))
        val secondClient = boundary.resolve("192.0.2.10", forwarded("198.51.100.2"))

        assertTrue(limiter.allow("/billing/play/verify:${firstClient.clientAddress}", 1))
        assertFalse(limiter.allow("/billing/play/verify:${firstClient.clientAddress}", 1))
        assertTrue(limiter.allow("/billing/play/verify:${secondClient.clientAddress}", 1))
    }

    private fun forwarded(clientAddress: String): Map<String, List<String>> =
        mapOf("X-Forwarded-For" to listOf(clientAddress), "X-Forwarded-Proto" to listOf("https"))

    private fun assertRejected(
        peerAddress: String,
        headers: Map<String, List<String>>,
    ) {
        assertTrue(runCatching { boundary.resolve(peerAddress, headers) }.exceptionOrNull() is IllegalArgumentException)
    }
}
