package com.faarigh.app.service.vpn

import android.util.Log

/**
 * Decides whether a DNS query should be blocked or forwarded to upstream DNS.
 * Maintains an in-memory map of blocked domains -> categories for fast lookups
 * and category-aware stats tracking.
 */
class DnsInterceptor {

    companion object {
        private const val TAG = "DnsInterceptor"
    }

    data class BlockResult(val blocked: Boolean, val category: String = "allowed")

    // Blocked domains → category — populated from Room DB on service start
    @Volatile
    private var blockedDomainMap = emptyMap<String, String>()

    fun updateBlocklist(domains: Set<String>) {
        // Legacy: just a set, no categories
        blockedDomainMap = domains.associateWith { "ads" }
        Log.d(TAG, "Blocklist updated: ${domains.size} domains")
    }

    fun updateBlocklistWithCategories(domainCategories: Map<String, String>) {
        blockedDomainMap = domainCategories
        Log.d(TAG, "Blocklist updated: ${domainCategories.size} domains with categories")
    }

    /**
     * Check if a domain should be blocked and return the category.
     * Also checks parent domains (e.g., "ads.example.com" matches "example.com").
     */
    fun check(domain: String): BlockResult {
        val normalizedDomain = domain.lowercase().trimEnd('.')

        blockedDomainMap[normalizedDomain]?.let {
            return BlockResult(blocked = true, category = it)
        }

        // Check parent domains
        var parent = normalizedDomain
        while (parent.contains('.')) {
            parent = parent.substringAfter('.')
            blockedDomainMap[parent]?.let {
                return BlockResult(blocked = true, category = it)
            }
        }

        return BlockResult(blocked = false, category = "allowed")
    }

    /** Backward compat */
    fun isBlocked(domain: String): Boolean = check(domain).blocked
}
