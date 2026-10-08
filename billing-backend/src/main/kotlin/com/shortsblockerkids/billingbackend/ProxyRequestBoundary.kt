package com.shortsblockerkids.billingbackend

import java.net.InetAddress

data class RequestOrigin(
    val clientAddress: String,
    val secure: Boolean,
)

class ProxyRequestBoundary(
    trustedProxyAddresses: Set<String>,
) {
    private val trustedPeers = trustedProxyAddresses.map { requireNotNull(numericIpAddress(it)) }.toSet()

    fun resolve(
        peerAddress: String,
        headers: Map<String, List<String>>,
    ): RequestOrigin {
        val peer = requireNotNull(numericIpAddress(peerAddress)) { "Invalid socket peer address" }
        val forwardedHeaders = headers.keys.filter { it.equals("Forwarded", true) || it.startsWith("X-Forwarded-", true) }
        if (peer !in trustedPeers) {
            require(forwardedHeaders.isEmpty()) { "Forwarded headers require a trusted proxy" }
            return RequestOrigin(peer, secure = false)
        }
        require(forwardedHeaders.all { it.equals("X-Forwarded-For", true) || it.equals("X-Forwarded-Proto", true) }) {
            "Unsupported forwarded header"
        }
        val clientAddress =
            requireNotNull(numericIpAddress(headers.singleHeader("X-Forwarded-For"))) {
                "X-Forwarded-For must contain one numeric client address"
            }
        val protocol = headers.singleHeader("X-Forwarded-Proto")
        require(protocol == "http" || protocol == "https") { "Invalid X-Forwarded-Proto" }
        return RequestOrigin(clientAddress, secure = protocol == "https")
    }

    private fun Map<String, List<String>>.singleHeader(name: String): String {
        val values = entries.filter { it.key.equals(name, true) }.flatMap { it.value }
        require(values.size == 1) { "$name must be supplied exactly once by the proxy" }
        return values.single().trim()
    }
}

internal fun numericIpAddress(address: String): String? {
    if (address.length > 45) return null
    val bytes = if (':' in address) ipv6Bytes(address) else ipv4Bytes(address)
    // getByAddress never resolves names; both configuration and forwarded input are numeric-only.
    return bytes?.let { InetAddress.getByAddress(it).hostAddress }
}

private fun ipv4Bytes(address: String): ByteArray? {
    val components = address.split('.')
    if (components.size != 4) return null
    val octets =
        components.map { component ->
            if (!component.matches(Regex("0|[1-9][0-9]{0,2}"))) return null
            component.toInt().takeIf { it <= 255 } ?: return null
        }
    return octets.map(Int::toByte).toByteArray()
}

private fun ipv6Bytes(address: String): ByteArray? {
    val literal = expandIpv4Tail(address) ?: return null
    if (literal.length > 39 || literal.any { it != ':' && it !in "0123456789abcdefABCDEF" }) return null
    val halves = literal.split("::")
    if (halves.size > 2) return null
    val left = ipv6Groups(halves[0]) ?: return null
    val right = if (halves.size == 2) ipv6Groups(halves[1]) ?: return null else emptyList()
    val padding = 8 - left.size - right.size
    if (padding < 0 || (halves.size == 1 && padding != 0) || (halves.size == 2 && padding == 0)) return null
    val groups = left + List(padding) { 0 } + right
    return ByteArray(16) { index ->
        val group = groups[index / 2]
        (if (index % 2 == 0) group ushr 8 else group and 0xff).toByte()
    }
}

private fun expandIpv4Tail(address: String): String? {
    if ('.' !in address) return address
    val separator = address.lastIndexOf(':')
    if (separator < 0) return null
    val octets = ipv4Bytes(address.substring(separator + 1)) ?: return null
    val high = ((octets[0].toInt() and 0xff) shl 8) or (octets[1].toInt() and 0xff)
    val low = ((octets[2].toInt() and 0xff) shl 8) or (octets[3].toInt() and 0xff)
    return address.substring(0, separator + 1) + high.toString(16) + ":" + low.toString(16)
}

private fun ipv6Groups(half: String): List<Int>? {
    if (half.isEmpty()) return emptyList()
    return half.split(':').map { group ->
        if (!group.matches(Regex("[0-9a-fA-F]{1,4}"))) return null
        group.toInt(16)
    }
}
