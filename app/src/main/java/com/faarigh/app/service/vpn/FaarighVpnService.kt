package com.faarigh.app.service.vpn

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.faarigh.app.FaarighApp
import com.faarigh.app.R
import com.faarigh.app.data.db.entity.DnsQueryLog
import com.faarigh.app.data.preferences.ModulePreferences
import com.faarigh.app.data.repository.DnsStatsRepository
import com.faarigh.app.data.repository.DomainFilterRepository
import com.faarigh.app.ui.MainActivity
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * DNS-only VPN filter using the DNS66/PersonalDNSFilter pattern:
 *
 * 1. Use RFC 5737 TEST-NET IPs (192.0.2.x) as fake DNS server aliases
 * 2. addDnsServer() for each alias -> Android sends DNS queries to these IPs
 * 3. addRoute() only for alias IPs -> only DNS packets enter the TUN
 * 4. Parse DNS, check blocklist, block or forward to real upstream
 * 5. Regular traffic (HTTPS, TCP) never touches the TUN
 *
 * Key: protect() a single reusable UDP socket BEFORE starting the proxy loop.
 * This socket is used for ALL upstream forwarding, avoiding per-query socket creation.
 */
class FaarighVpnService : VpnService() {

    companion object {
        private const val TAG = "FaarighVPN"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.faarigh.app.VPN_START"
        const val ACTION_STOP = "com.faarigh.app.VPN_STOP"

        // RFC 5737 TEST-NET-1 range — safe to use, never routed on real networks
        private const val VPN_ADDRESS = "192.0.2.1"
        // Upstream DNS servers — 8.8.8.8 primary, 9.9.9.9 fallback
        // (1.1.1.1 blocked by many ISPs)
        private val UPSTREAM_DNS = listOf("8.8.8.8", "9.9.9.9")

        private val _isRunning = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunning.asStateFlow()
        val isRunning: Boolean get() = _isRunning.value

        private val _totalQueries = MutableStateFlow(0)
        private val _blockedQueries = MutableStateFlow(0)
        val totalQueries: StateFlow<Int> = _totalQueries.asStateFlow()
        val blockedQueries: StateFlow<Int> = _blockedQueries.asStateFlow()

        /**
         * Helper to start the VPN service — handles the case where the service
         * was previously stopped from the notification bar.
         */
        fun start(context: Context) {
            val intent = Intent(context, FaarighVpnService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FaarighVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VpnEntryPoint {
        fun domainFilterRepository(): DomainFilterRepository
        fun dnsStatsRepository(): DnsStatsRepository
        fun modulePreferences(): ModulePreferences
    }

    private var serviceScope: CoroutineScope? = null
    private var tunInterface: ParcelFileDescriptor? = null
    private val dnsInterceptor = DnsInterceptor()
    private var domainFilterRepo: DomainFilterRepository? = null
    private var dnsStatsRepo: DnsStatsRepository? = null
    private var modulePrefs: ModulePreferences? = null

    // Batch logging — accumulate queries and flush less frequently to avoid UI lag
    private val pendingLogs = mutableListOf<DnsQueryLog>()
    private val BATCH_SIZE = 200
    private val FLUSH_INTERVAL_MS = 30_000L // 30 seconds
    private var lastFlushTime = 0L

    // Map alias IP last octet -> upstream DNS address
    private val aliasToUpstream = mutableMapOf<Int, String>()

    // Pre-protected UDP sockets — one per upstream DNS for concurrent forwarding
    private val upstreamSockets = mutableMapOf<String, DatagramSocket>()

    @Volatile private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> {
                // Always allow restart — clean up stale state first
                if (running) stopVpnInternal()
                startVpn()
                START_STICKY
            }
        }
    }

    private fun startVpn() {
        serviceScope?.cancel()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        startForeground(NOTIFICATION_ID, buildNotification())

        // Load blocklist with categories SYNCHRONOUSLY before starting proxy
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext, VpnEntryPoint::class.java,
        )
        domainFilterRepo = entryPoint.domainFilterRepository()
        dnsStatsRepo = entryPoint.dnsStatsRepository()
        modulePrefs = entryPoint.modulePreferences()
        try {
            runBlocking(Dispatchers.IO) {
                val domainCategories = domainFilterRepo!!.getEnabledDomainsWithCategories()
                dnsInterceptor.updateBlocklistWithCategories(domainCategories)
                Log.i(TAG, "Loaded ${domainCategories.size} blocked domains with categories")
                if (domainCategories.isNotEmpty()) {
                    Log.i(TAG, "Sample blocklist: ${domainCategories.entries.take(5).map { "${it.key} [${it.value}]" }}")
                } else {
                    Log.w(TAG, "BLOCKLIST IS EMPTY! Make sure domain categories are enabled.")
                }
                // Cleanup old logs (keep 30 days)
                dnsStatsRepo?.cleanup(30)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load blocklist: ${e.message}", e)
        }

        // Sockets are created AFTER establish() — this is critical!
        // DNS66 pattern: protect() must be called after the TUN is up
        upstreamSockets.values.forEach { it.close() }
        upstreamSockets.clear()

        val builder = Builder()
            .setSession("Faarigh DNS Filter")
            .addAddress(VPN_ADDRESS, 24)
            .setBlocking(true)
            .setMtu(1500)

        // DNS server aliasing: for each upstream, create a fake alias IP
        aliasToUpstream.clear()
        UPSTREAM_DNS.forEachIndexed { index, upstream ->
            val lastOctet = index + 2  // .2, .3, ...
            val alias = "192.0.2.$lastOctet"
            aliasToUpstream[lastOctet] = upstream
            builder.addDnsServer(alias)
            builder.addRoute(alias, 32)
            Log.i(TAG, "DNS alias: $alias -> $upstream")
        }

        try { builder.addDisallowedApplication(packageName) } catch (_: Exception) {}

        tunInterface = builder.establish()
        if (tunInterface == null) {
            Log.e(TAG, "Failed to establish VPN — user may need to grant permission again")
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return
        }

        // Create and protect sockets AFTER the TUN is established
        // This is the DNS66 pattern — protect() routes traffic outside the VPN tunnel
        for (dns in UPSTREAM_DNS) {
            try {
                val socket = DatagramSocket()
                socket.soTimeout = 2500
                if (!protect(socket)) {
                    Log.e(TAG, "protect() FAILED for $dns")
                    socket.close()
                    continue
                }
                upstreamSockets[dns] = socket
                Log.i(TAG, "Socket for $dns created and protected (port ${socket.localPort})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create socket for $dns: ${e.message}")
            }
        }
        if (upstreamSockets.isEmpty()) {
            Log.e(TAG, "CRITICAL: No upstream sockets!")
            tunInterface?.close(); tunInterface = null
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return
        }

        running = true
        _isRunning.value = true
        _totalQueries.value = 0
        _blockedQueries.value = 0

        // Save VPN enabled state for BootReceiver and also to legacy SharedPreferences
        serviceScope?.launch {
            try { modulePrefs?.setVpnEnabled(true) } catch (_: Exception) {}
        }
        applicationContext.getSharedPreferences("faarigh_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("vpn_enabled", true).apply()

        serviceScope?.launch { runDnsProxy() }

        // Observe domain DB changes and reload blocklist automatically
        @OptIn(FlowPreview::class)
        serviceScope?.launch {
            domainFilterRepo?.getAllDomains()
                ?.drop(1) // Skip initial emission (already loaded above)
                ?.debounce(2000L)
                ?.collect {
                    try {
                        val updated = domainFilterRepo?.getEnabledDomainsWithCategories() ?: return@collect
                        dnsInterceptor.updateBlocklistWithCategories(updated)
                        Log.i(TAG, "Blocklist reloaded: ${updated.size} domains")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to reload blocklist: ${e.message}")
                    }
                }
        }

        Log.i(TAG, "VPN started — aliases: $aliasToUpstream")
    }

    private fun runDnsProxy() {
        val tunFd = tunInterface?.fileDescriptor ?: return
        val input = FileInputStream(tunFd)
        val output = FileOutputStream(tunFd)
        val buffer = ByteArray(32767)
        var queryCount = 0
        var blockedCount = 0

        Log.i(TAG, "DNS proxy loop starting")

        while (running) {
            try {
                val length = input.read(buffer)
                if (length <= 0) {
                    if (running) Log.w(TAG, "TUN read returned $length")
                    break
                }

                // Check protocol — only handle UDP (17)
                if (length < 20) continue
                val protocol = buffer[9].toInt() and 0xFF
                if (protocol != 17) continue // Drop TCP (DoT on 853) and anything else

                val parsed = DnsPacketParser.parse(buffer, length) ?: continue

                queryCount++
                _totalQueries.value = queryCount
                val domain = parsed.queryDomain

                // Determine which real upstream to use based on alias IP
                val destLastOctet = parsed.destIp[3].toInt() and 0xFF
                val upstreamDns = aliasToUpstream[destLastOctet] ?: UPSTREAM_DNS[0]

                val checkResult = dnsInterceptor.check(domain)
                if (queryCount <= 10) Log.d(TAG, "Query #$queryCount: $domain -> blocked=${checkResult.blocked} cat=${checkResult.category}")

                if (checkResult.blocked) {
                    blockedCount++
                    _blockedQueries.value = blockedCount
                    if (blockedCount <= 50) Log.i(TAG, "BLOCKED: $domain [${checkResult.category}]")
                    val response = DnsPacketParser.buildNxdomainResponse(parsed)
                    fixIpChecksum(response)
                    output.write(response)
                    output.flush()
                    queueLog(domain, true, checkResult.category, "", 0)
                } else {
                    if (queryCount <= 20) Log.d(TAG, "FORWARD: $domain -> $upstreamDns")
                    var forwarded = false
                    for (dns in listOf(upstreamDns) + UPSTREAM_DNS.filter { it != upstreamDns }) {
                        val startMs = System.currentTimeMillis()
                        val response = forwardDns(buffer, length, parsed, dns)
                        if (response != null) {
                            val elapsed = System.currentTimeMillis() - startMs
                            fixIpChecksum(response)
                            output.write(response)
                            output.flush()
                            forwarded = true
                            queueLog(domain, false, "allowed", dns, elapsed)
                            break
                        }
                    }
                    if (!forwarded) Log.w(TAG, "Forward FAILED (all DNS): $domain")
                }
            } catch (e: java.io.IOException) {
                if (running) Log.w(TAG, "TUN I/O error: ${e.message}")
                break
            } catch (e: Exception) {
                if (running) Log.e(TAG, "Proxy error: ${e.message}")
            }
        }

        Log.i(TAG, "DNS proxy exited — $queryCount queries, $blockedCount blocked")
    }

    /**
     * Forward DNS query to upstream using the pre-protected socket for that DNS.
     * Each upstream DNS has its own socket so they don't block each other.
     */
    private fun forwardDns(
        rawPacket: ByteArray, rawLength: Int,
        parsed: DnsPacketParser.ParsedPacket,
        upstreamDns: String,
    ): ByteArray? {
        val socket = upstreamSockets[upstreamDns] ?: getOrCreateSocket(upstreamDns) ?: return null

        return try {
            val ipHeaderLen = (rawPacket[0].toInt() and 0x0F) * 4
            val dnsOffset = ipHeaderLen + 8
            val udpTotalLen = ((rawPacket[ipHeaderLen + 4].toInt() and 0xFF) shl 8) or
                    (rawPacket[ipHeaderLen + 5].toInt() and 0xFF)
            val dnsPayloadLen = udpTotalLen - 8
            if (dnsOffset + dnsPayloadLen > rawLength || dnsPayloadLen <= 0) return null

            val upstream = InetAddress.getByName(upstreamDns)

            synchronized(socket) {
                socket.send(DatagramPacket(rawPacket, dnsOffset, dnsPayloadLen, upstream, 53))
                val responseBuf = ByteArray(4096)
                val resp = DatagramPacket(responseBuf, responseBuf.size)
                socket.receive(resp)

                buildIpUdpPacket(
                    parsed.destIp, parsed.sourceIp,
                    parsed.destPort, parsed.sourcePort,
                    responseBuf.copyOf(resp.length),
                )
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "DNS timeout: ${parsed.queryDomain} -> $upstreamDns")
            null
        } catch (e: Exception) {
            Log.e(TAG, "DNS forward error ($upstreamDns): ${e.message}")
            recreateSocket(upstreamDns)
            null
        }
    }

    private fun getOrCreateSocket(dns: String): DatagramSocket? {
        return try {
            val socket = DatagramSocket()
            socket.soTimeout = 2500
            if (protect(socket)) {
                upstreamSockets[dns] = socket
                socket
            } else {
                socket.close(); null
            }
        } catch (_: Exception) { null }
    }

    private fun recreateSocket(dns: String) {
        try {
            upstreamSockets[dns]?.close()
            upstreamSockets.remove(dns)
            getOrCreateSocket(dns)
            Log.i(TAG, "Socket recreated for $dns")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recreate socket for $dns: ${e.message}")
        }
    }

    private fun buildIpUdpPacket(
        srcIp: ByteArray, dstIp: ByteArray,
        srcPort: Int, dstPort: Int, payload: ByteArray,
    ): ByteArray {
        val udpLen = 8 + payload.size
        val totalLen = 20 + udpLen
        val buf = ByteBuffer.allocate(totalLen)
        buf.put(0x45.toByte()); buf.put(0); buf.putShort(totalLen.toShort())
        buf.putShort(0); buf.putShort(0x4000.toShort())
        buf.put(64); buf.put(17); buf.putShort(0)
        buf.put(srcIp); buf.put(dstIp)
        buf.putShort(srcPort.toShort()); buf.putShort(dstPort.toShort())
        buf.putShort(udpLen.toShort()); buf.putShort(0)
        buf.put(payload)
        return buf.array()
    }

    private fun fixIpChecksum(p: ByteArray) {
        if (p.size < 20) return
        val ihl = (p[0].toInt() and 0x0F) * 4
        p[10] = 0; p[11] = 0
        var s = 0L
        for (i in 0 until ihl step 2) {
            s += ((p[i].toInt() and 0xFF) shl 8) or (p[i + 1].toInt() and 0xFF)
        }
        while (s shr 16 != 0L) s = (s and 0xFFFF) + (s shr 16)
        val c = s.toInt().inv() and 0xFFFF
        p[10] = (c shr 8).toByte(); p[11] = (c and 0xFF).toByte()
    }

    /**
     * Queue a DNS query log entry. Batches writes to the database
     * to avoid I/O on every single query.
     */
    private fun queueLog(domain: String, blocked: Boolean, category: String, dns: String, responseMs: Long) {
        synchronized(pendingLogs) {
            pendingLogs.add(DnsQueryLog(
                domain = domain,
                isBlocked = blocked,
                category = category,
                upstreamDns = dns,
                responseTimeMs = responseMs,
            ))
            val now = System.currentTimeMillis()
            if (pendingLogs.size >= BATCH_SIZE || now - lastFlushTime > FLUSH_INTERVAL_MS) {
                val batch = pendingLogs.toList()
                pendingLogs.clear()
                lastFlushTime = now
                serviceScope?.launch {
                    try {
                        dnsStatsRepo?.logBatch(batch)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to flush DNS logs: ${e.message}")
                    }
                }
            }
        }
    }

    /** Flush any remaining logs */
    private fun flushLogs() {
        synchronized(pendingLogs) {
            if (pendingLogs.isNotEmpty()) {
                val batch = pendingLogs.toList()
                pendingLogs.clear()
                // Best-effort: try to write remaining logs
                try {
                    kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                        dnsStatsRepo?.logBatch(batch)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Internal cleanup without stopping the Android service.
     */
    private fun stopVpnInternal() {
        flushLogs()
        running = false
        try { tunInterface?.close() } catch (_: Exception) {}
        tunInterface = null
        upstreamSockets.values.forEach { try { it.close() } catch (_: Exception) {} }
        upstreamSockets.clear()
    }

    private fun stopVpn() {
        Log.i(TAG, "Stopping VPN...")
        stopVpnInternal()
        _isRunning.value = false

        // Save VPN disabled state
        try {
            kotlinx.coroutines.runBlocking { modulePrefs?.setVpnEnabled(false) }
        } catch (_: Exception) {}
        applicationContext.getSharedPreferences("faarigh_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("vpn_enabled", false).apply()

        serviceScope?.cancel(); serviceScope = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "VPN stopped")
    }

    private fun buildNotification() = NotificationCompat.Builder(this, FaarighApp.CHANNEL_VPN)
        .setContentTitle(getString(R.string.vpn_notification_title))
        .setContentText(getString(R.string.vpn_notification_text))
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        ))
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel, "Stop",
            PendingIntent.getService(
                this, 0,
                Intent(this, FaarighVpnService::class.java).apply { action = ACTION_STOP },
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    override fun onRevoke() {
        // Called when user revokes VPN permission from system settings
        Log.i(TAG, "VPN permission revoked by user")
        stopVpn()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnInternal()
        _isRunning.value = false
        serviceScope?.cancel(); serviceScope = null
    }
}
