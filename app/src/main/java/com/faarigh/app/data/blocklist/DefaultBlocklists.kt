package com.faarigh.app.data.blocklist

/**
 * Starter blocklists shipped with the app. In production, these would be much larger
 * (loaded from a bundled text file in assets). This is a representative sample for the MVP.
 *
 * Full lists can be sourced from:
 * - OISD NSFW: https://oisd.nl/downloads (nsfw category)
 * - AdGuard DNS: https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt
 * - Energized Protection: https://energized.pro/
 */
object DefaultBlocklists {

    val EXPLICIT = listOf(
        "pornhub.com",
        "xvideos.com",
        "xnxx.com",
        "xhamster.com",
        "redtube.com",
        "youporn.com",
        "tube8.com",
        "spankbang.com",
        "eporner.com",
        "beeg.com",
        "porn.com",
        "brazzers.com",
        "bangbros.com",
        "naughtyamerica.com",
        "realitykings.com",
        "mofos.com",
        "hentaihaven.xxx",
        "nhentai.net",
        "rule34.xxx",
        "e621.net",
        "chaturbate.com",
        "stripchat.com",
        "cam4.com",
        "bongacams.com",
        "myfreecams.com",
        "livejasmin.com",
        "onlyfans.com",
        "fansly.com",
    )

    val ADS = listOf(
        // Google Ads
        "pagead2.googlesyndication.com",
        "googleadservices.com",
        "tpc.googlesyndication.com",
        "doubleclick.net",
        "ad.doubleclick.net",
        "googleads.g.doubleclick.net",
        // Facebook/Meta Ads
        "an.facebook.com",
        "ads.facebook.com",
        // General ad networks
        "ads.pubmatic.com",
        "adservice.google.com",
        "ade.googlesyndication.com",
        "adnxs.com",
        "ads.yahoo.com",
        "advertising.com",
        "outbrain.com",
        "taboola.com",
        "moatads.com",
        "amazon-adsystem.com",
        "ads.linkedin.com",
        "adsrvr.org",
        "adform.net",
        "criteo.com",
        "rubiconproject.com",
        "openx.net",
        "bidswitch.net",
        "smartadserver.com",
        "smaato.net",
        "mopub.com",
        "unity3d.com",
        "applovin.com",
        "vungle.com",
        "inmobi.com",
        "admob.com",
    )

    val TELEMETRY = listOf(
        // Facebook SDK
        "graph.facebook.com",
        "connect.facebook.net",
        "pixel.facebook.com",
        "www.facebook.com/tr",
        // Google Analytics/Firebase
        "google-analytics.com",
        "ssl.google-analytics.com",
        "firebase-settings.crashlytics.com",
        "app-measurement.com",
        "firebaselogging-pa.googleapis.com",
        // Microsoft
        "vortex.data.microsoft.com",
        "settings-win.data.microsoft.com",
        "watson.telemetry.microsoft.com",
        // Segment
        "api.segment.io",
        "cdn.segment.com",
        // Mixpanel
        "api.mixpanel.com",
        "decide.mixpanel.com",
        // Amplitude
        "api.amplitude.com",
        // Adjust
        "app.adjust.com",
        // AppsFlyer
        "launches.appsflyer.com",
        "t.appsflyer.com",
        // Branch
        "api.branch.io",
        // Sentry
        "sentry.io",
        "o0.ingest.sentry.io",
        // Crashlytics
        "firebase-settings.crashlytics.com",
        "reports.crashlytics.com",
    )
}
