package com.faarigh.app.service.vpn

import java.nio.ByteBuffer

/**
 * Parses and constructs DNS packets for the local VPN DNS interceptor.
 *
 * DNS packet structure (over UDP):
 * [IP Header (20 bytes)] [UDP Header (8 bytes)] [DNS Payload]
 *
 * DNS Payload:
 * [Header (12 bytes)] [Questions] [Answers] [Authority] [Additional]
 */
object DnsPacketParser {

    private const val IP_HEADER_SIZE = 20
    private const val UDP_HEADER_SIZE = 8
    private const val DNS_HEADER_SIZE = 12

    data class ParsedPacket(
        val sourceIp: ByteArray,
        val destIp: ByteArray,
        val sourcePort: Int,
        val destPort: Int,
        val dnsTransactionId: Int,
        val queryDomain: String,
        val queryType: Int,
        val rawPacket: ByteArray,
    )

    /**
     * Parse an IP+UDP+DNS packet from the TUN interface.
     * Returns null if the packet is not a DNS query (UDP port 53).
     */
    fun parse(packet: ByteArray, length: Int): ParsedPacket? {
        if (length < IP_HEADER_SIZE + UDP_HEADER_SIZE + DNS_HEADER_SIZE) return null

        val buffer = ByteBuffer.wrap(packet, 0, length)

        // IP Header
        val versionIhl = buffer.get().toInt() and 0xFF
        val version = versionIhl shr 4
        if (version != 4) return null // Only IPv4

        val ihl = (versionIhl and 0x0F) * 4
        buffer.position(9)
        val protocol = buffer.get().toInt() and 0xFF
        if (protocol != 17) return null // Only UDP

        buffer.position(12)
        val sourceIp = ByteArray(4)
        buffer.get(sourceIp)
        val destIp = ByteArray(4)
        buffer.get(destIp)

        // UDP Header (starts at position ihl)
        buffer.position(ihl)
        val sourcePort = buffer.short.toInt() and 0xFFFF
        val destPort = buffer.short.toInt() and 0xFFFF

        // Only intercept DNS (port 53)
        if (destPort != 53) return null

        buffer.position(ihl + 4) // UDP length
        val udpLength = buffer.short.toInt() and 0xFFFF
        buffer.position(ihl + 8) // Start of DNS payload

        // DNS Header
        val dnsStart = ihl + UDP_HEADER_SIZE
        val transactionId = buffer.short.toInt() and 0xFFFF
        val flags = buffer.short.toInt() and 0xFFFF
        val isQuery = (flags and 0x8000) == 0
        if (!isQuery) return null

        val qdCount = buffer.short.toInt() and 0xFFFF
        buffer.short // anCount
        buffer.short // nsCount
        buffer.short // arCount

        if (qdCount < 1) return null

        // Parse query domain name
        val domain = readDomainName(packet, dnsStart + DNS_HEADER_SIZE)
        if (domain.isEmpty()) return null

        // Query type (A = 1, AAAA = 28, etc.)
        val domainEndPos = findDomainEndPosition(packet, dnsStart + DNS_HEADER_SIZE)
        val queryType = if (domainEndPos + 2 <= length) {
            ((packet[domainEndPos].toInt() and 0xFF) shl 8) or (packet[domainEndPos + 1].toInt() and 0xFF)
        } else {
            1 // Default to A record
        }

        return ParsedPacket(
            sourceIp = sourceIp,
            destIp = destIp,
            sourcePort = sourcePort,
            destPort = destPort,
            dnsTransactionId = transactionId,
            queryDomain = domain,
            queryType = queryType,
            rawPacket = packet.copyOf(length),
        )
    }

    /**
     * Build an NXDOMAIN response packet for a blocked domain.
     * This tells the requesting app that the domain does not exist.
     */
    fun buildNxdomainResponse(query: ParsedPacket): ByteArray {
        val dnsPayload = buildDnsNxdomainPayload(query.dnsTransactionId)
        return buildUdpIpPacket(
            sourceIp = query.destIp,
            destIp = query.sourceIp,
            sourcePort = query.destPort,
            destPort = query.sourcePort,
            payload = dnsPayload,
        )
    }

    /**
     * Build a DNS response with RCODE=3 (NXDOMAIN).
     */
    private fun buildDnsNxdomainPayload(transactionId: Int): ByteArray {
        val buffer = ByteBuffer.allocate(DNS_HEADER_SIZE)
        buffer.putShort(transactionId.toShort())
        buffer.putShort(0x8183.toShort()) // Response, NXDOMAIN (rcode=3)
        buffer.putShort(0) // QDCOUNT
        buffer.putShort(0) // ANCOUNT
        buffer.putShort(0) // NSCOUNT
        buffer.putShort(0) // ARCOUNT
        return buffer.array()
    }

    /**
     * Build a complete IP+UDP packet wrapping a DNS payload.
     */
    private fun buildUdpIpPacket(
        sourceIp: ByteArray,
        destIp: ByteArray,
        sourcePort: Int,
        destPort: Int,
        payload: ByteArray,
    ): ByteArray {
        val udpLength = UDP_HEADER_SIZE + payload.size
        val totalLength = IP_HEADER_SIZE + udpLength
        val buffer = ByteBuffer.allocate(totalLength)

        // IP Header
        buffer.put(0x45.toByte()) // Version 4, IHL 5
        buffer.put(0x00.toByte()) // DSCP/ECN
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0) // Identification
        buffer.putShort(0x4000.toShort()) // Flags: Don't Fragment
        buffer.put(64.toByte()) // TTL
        buffer.put(17.toByte()) // Protocol: UDP
        buffer.putShort(0) // Header checksum (set to 0, kernel will compute)
        buffer.put(sourceIp)
        buffer.put(destIp)

        // UDP Header
        buffer.putShort(sourcePort.toShort())
        buffer.putShort(destPort.toShort())
        buffer.putShort(udpLength.toShort())
        buffer.putShort(0) // UDP checksum (optional for IPv4)

        // DNS Payload
        buffer.put(payload)

        return buffer.array()
    }

    /**
     * Read a DNS domain name from the packet at the given offset.
     * Handles label format (length-prefixed segments ending with 0x00).
     */
    private fun readDomainName(packet: ByteArray, offset: Int): String {
        val parts = mutableListOf<String>()
        var pos = offset

        while (pos < packet.size) {
            val labelLength = packet[pos].toInt() and 0xFF
            if (labelLength == 0) break
            if (labelLength >= 0xC0) break // Compression pointer — skip for queries

            pos++
            if (pos + labelLength > packet.size) break

            parts.add(String(packet, pos, labelLength, Charsets.US_ASCII))
            pos += labelLength
        }

        return parts.joinToString(".")
    }

    /**
     * Find the position right after the domain name (the null terminator + 1).
     */
    private fun findDomainEndPosition(packet: ByteArray, offset: Int): Int {
        var pos = offset
        while (pos < packet.size) {
            val labelLength = packet[pos].toInt() and 0xFF
            if (labelLength == 0) return pos + 1
            if (labelLength >= 0xC0) return pos + 2
            pos += labelLength + 1
        }
        return pos
    }
}
