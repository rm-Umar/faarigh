package com.faarigh.app.module.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import com.faarigh.app.R
import com.faarigh.app.data.preferences.ModulePreferences
import com.faarigh.app.data.repository.DnsStatsRepository
import com.faarigh.app.data.repository.DomainFilterRepository
import com.faarigh.app.module.CardCategory
import com.faarigh.app.module.Citation
import com.faarigh.app.module.ModuleConfigItem
import com.faarigh.app.module.ModuleStat
import com.faarigh.app.module.OnboardingCard
import com.faarigh.app.module.WellbeingModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsFilterModule @Inject constructor(
    private val prefs: ModulePreferences,
    private val dnsStatsRepo: DnsStatsRepository,
    private val domainFilterRepo: DomainFilterRepository,
) : WellbeingModule {

    override val id = "dns_filter"
    override val name = "DNS Filter"
    override val description = "Blocks ads, trackers, telemetry, and explicit domains at the DNS level"
    override val iconRes = R.drawable.ic_module_dns_filter
    override val accentColor = 0xFFF5D76EL

    override val isEnabled: Flow<Boolean> = prefs.dnsFilterEnabled
    override suspend fun setEnabled(enabled: Boolean) = prefs.setDnsFilterEnabled(enabled)

    override val configItems: List<ModuleConfigItem> = listOf(
        ModuleConfigItem.Info(
            key = "dns_info",
            label = "Runs a local VPN to filter DNS queries on-device. Blocks ads, trackers, and harmful domains without sending any data off your phone.",
        ),
        ModuleConfigItem.Info(
            key = "dns_config_link",
            label = "Tap 'Content Filters' in the navigation to manage DNS categories and custom domains.",
        ),
    )

    override val statsItems: Flow<List<ModuleStat>> = run {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val totalFlow = dnsStatsRepo.getTotalQueries(todayStart)
        val blockedFlow = dnsStatsRepo.getBlockedQueries(todayStart)
        val enabledCountFlow = domainFilterRepo.getEnabledCount()
        val topBlockedFlow = dnsStatsRepo.getTopBlockedDomains(todayStart, 1)

        combine(totalFlow, blockedFlow, enabledCountFlow, topBlockedFlow) { total, blocked, enabledCount, topBlocked ->
            val filterRate = if (total > 0) (blocked * 100 / total) else 0
            val topDomain = topBlocked.firstOrNull()?.domain ?: "—"

            listOf(
                ModuleStat(label = "Queries today", value = "$total"),
                ModuleStat(label = "Blocked today", value = "$blocked"),
                ModuleStat(label = "Filter rate", value = "$filterRate%"),
                ModuleStat(label = "Active rules", value = "$enabledCount"),
                ModuleStat(label = "Top blocked", value = topDomain),
            )
        }
    }

    override val educationCards = listOf(
        OnboardingCard(
            title = "Your Phone's Firewall",
            body = "DNS Filter is like a gatekeeper for your internet traffic. Every " +
                "time an app tries to connect to a server (to load an ad, send tracking " +
                "data, or fetch content), it first asks \"where is this server?\" — " +
                "that's a DNS request. This module intercepts those requests and blocks " +
                "the ones that go to ad networks, trackers, or explicit content domains.",
            icon = Icons.Default.Shield,
            category = CardCategory.WHAT_IS_IT,
        ),
        OnboardingCard(
            title = "A Local VPN — Nothing Leaves",
            body = "This uses Android's VPN feature to create a local tunnel on your " +
                "device. Unlike a real VPN, your traffic does NOT go to any external " +
                "server. DNS requests are filtered right on your phone. " +
                "Blocked domains get a \"does not exist\" response. Allowed domains " +
                "are forwarded normally. Same concept as Pi-hole — but in your pocket.",
            icon = Icons.Default.Dns,
            category = CardCategory.HOW_IT_WORKS,
        ),
        OnboardingCard(
            title = "You're Being Tracked — Constantly",
            body = "An Oxford University study of nearly 1 million Android apps found " +
                "that the median app contains 5 third-party trackers, with some " +
                "containing over 30. These SDKs silently report what you do, when, " +
                "and how often — even when you're not actively using the app. " +
                "A 2020 Aarhus University study found that 85% of free Android apps " +
                "share data with Google, and 43% with Facebook's tracking infrastructure.",
            icon = Icons.Default.Visibility,
            category = CardCategory.WHY_NEEDED,
            citations = listOf(
                Citation(
                    label = "Binns et al., 2018",
                    title = "Third Party Tracking in the Mobile Ecosystem",
                    url = "https://doi.org/10.1145/3201064.3201089",
                ),
                Citation(
                    label = "Kollnig et al., 2022",
                    title = "Are iPhones Really Better for Privacy?",
                    url = "https://doi.org/10.2478/popets-2022-0033",
                ),
                Citation(
                    label = "Exodus Privacy",
                    title = "Exodus Privacy — Android App Tracker Analysis",
                    url = "https://exodus-privacy.eu.org/",
                ),
            ),
            stats = listOf(
                "Median app contains 5 third-party trackers (Binns et al., 2018 — ACM)",
                "61% of apps automatically send data to Facebook SDK when opened (Privacy International, 2018)",
                "90.4% of free Google Play apps contain at least one tracker (Exodus Privacy analysis of 80K+ apps)",
            ),
        ),
        OnboardingCard(
            title = "Cleaner, Faster, Private",
            body = "DNS filtering measurably improves your experience. Pi-hole users " +
                "report 15-30% reduction in DNS queries (all blocked ads/trackers). " +
                "Less network traffic means faster page loads, lower data usage, " +
                "and longer battery life. You control exactly which categories to " +
                "block and can add custom domains. Full transparency, full control.",
            icon = Icons.Default.PrivacyTip,
            category = CardCategory.WHY_HAVE_IT,
            citations = listOf(
                Citation(
                    label = "Pi-hole Project",
                    title = "Pi-hole: A Black Hole for Internet Advertisements",
                    url = "https://pi-hole.net/",
                ),
                Citation(
                    label = "OISD Blocklist",
                    title = "OISD — Internet's #1 Domain Blocklist",
                    url = "https://oisd.nl/",
                ),
            ),
            stats = listOf(
                "Pi-hole networks typically block 15-30% of all DNS queries (Pi-hole community telemetry)",
                "Privacy International found Facebook receives data from apps even when user has no Facebook account",
            ),
        ),
    )

    override fun onStart() { /* Started via VpnService */ }
    override fun onStop() { /* Stopped via VpnService */ }
}
