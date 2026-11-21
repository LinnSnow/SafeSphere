package com.safesphere.vpn

object DnsPacketParser {

    /** Length of the IPv4 header in bytes, or -1 if this isn't an IPv4 UDP packet to port 53. */
    fun dnsIpHeaderLength(p: ByteArray): Int {
        if (p.size < 28) return -1
        if ((p[0].toInt() shr 4) != 4) return -1           // IPv4 only
        if (p[9].toInt() != 17) return -1                  // protocol 17 = UDP
        val ihl = (p[0].toInt() and 0x0F) * 4
        val dstPort = ((p[ihl + 2].toInt() and 0xFF) shl 8) or (p[ihl + 3].toInt() and 0xFF)
        return if (dstPort == 53) ihl else -1
    }

    /** Extracts the queried domain (e.g. "www.google.com") or null if not parseable. */
    fun parse(p: ByteArray): String? {
        val ihl = dnsIpHeaderLength(p)
        if (ihl < 0) return null
        var i = ihl + 8 + 12            // skip IP + UDP (8) + DNS header (12)
        val sb = StringBuilder()
        while (i < p.size) {
            val len = p[i].toInt() and 0xFF
            if (len == 0) break
            if (i + 1 + len > p.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(p, i + 1, len, Charsets.US_ASCII))
            i += len + 1
        }
        return sb.toString().ifEmpty { null }
    }

    /** Wraps a raw DNS reply in IP+UDP headers, swapping src/dst of the original request. */
    fun buildResponse(req: ByteArray, ihl: Int, dns: ByteArray): ByteArray {
        val out = ByteArray(ihl + 8 + dns.size)
        System.arraycopy(req, 0, out, 0, ihl)
        System.arraycopy(req, 16, out, 12, 4)   // new src = old dst
        System.arraycopy(req, 12, out, 16, 4)   // new dst = old src
        out[2] = (out.size shr 8).toByte(); out[3] = out.size.toByte()
        out[10] = 0; out[11] = 0
        val cs = checksum(out, ihl)
        out[10] = (cs shr 8).toByte(); out[11] = cs.toByte()
        // UDP: swap ports, set length, checksum 0 (allowed for IPv4)
        out[ihl] = req[ihl + 2]; out[ihl + 1] = req[ihl + 3]
        out[ihl + 2] = req[ihl]; out[ihl + 3] = req[ihl + 1]
        val udpLen = 8 + dns.size
        out[ihl + 4] = (udpLen shr 8).toByte(); out[ihl + 5] = udpLen.toByte()
        out[ihl + 6] = 0; out[ihl + 7] = 0
        System.arraycopy(dns, 0, out, ihl + 8, dns.size)
        return out
    }

    private fun checksum(b: ByteArray, len: Int): Int {
        var sum = 0
        var i = 0
        while (i < len) {
            sum += ((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)
            i += 2
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }
    /** Builds a DNS reply saying "this domain does not exist" (RCODE 3) from the raw DNS query. */
    fun buildNxDomain(q: ByteArray): ByteArray {
        var i = 12
        while (i < q.size && q[i].toInt() != 0) i += (q[i].toInt() and 0xFF) + 1
        val end = minOf(i + 5, q.size)               // end label + QTYPE(2) + QCLASS(2)
        val r = q.copyOf(end)
        r[2] = (q[2].toInt() or 0x80).toByte()       // QR = 1 (this is a response)
        r[3] = 0x83.toByte()                         // RA = 1, RCODE = 3 (NXDOMAIN)
        for (k in 6..11) r[k] = 0                    // no answer/authority/extra records
        return r
    }
}