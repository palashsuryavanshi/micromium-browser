package com.example.privacy

import android.net.Uri
import java.io.ByteArrayInputStream
import java.util.Locale

enum class BlockCategory(val displayName: String) {
    AD("Ad Network"),
    TRACKER("Tracker & Telemetry"),
    FINGERPRINT("Fingerprinting")
}

data class BlockResult(
    val isBlocked: Boolean,
    val category: BlockCategory = BlockCategory.AD,
    val matchedRule: String = ""
)

data class BlockedEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val host: String,
    val category: BlockCategory,
    val timestamp: Long = System.currentTimeMillis()
)

data class ShieldConfig(
    val isShieldEnabled: Boolean = true,
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val stripTrackingParams: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val cosmeticFiltering: Boolean = true,
    val forceHttps: Boolean = true
)

object PrivacyEngine {

    // Hardcoded set of well-known advertising host domains and subdomains
    private val AD_HOSTS = setOf(
        // Google Ads & DoubleClick
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "pagead.googlesyndication.com",
        "ads.google.com",
        "admob.com",
        "app-measurement.com",

        // Major Ad Exchanges & Networks
        "adnxs.com",
        "ib.adnxs.com",
        "secure.adnxs.com",
        "pubmatic.com",
        "ads.pubmatic.com",
        "rubiconproject.com",
        "optimized-by.rubiconproject.com",
        "openx.net",
        "us-u.openx.net",
        "criteo.com",
        "criteo.net",
        "static.criteo.net",
        "taboola.com",
        "cdn.taboola.com",
        "outbrain.com",
        "widgets.outbrain.com",
        "revcontent.com",
        "zergnet.com",
        "media.net",
        "contextual.media.net",
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",
        "s.amazon-adsystem.com",
        "sovrn.com",
        "ap.lijit.com",
        "triplelift.com",
        "yieldmo.com",
        "adform.net",
        "smartadserver.com",
        "casalemedia.com",
        "bidswitch.net",
        "exponential.com",
        "advertising.com",
        "adroll.com",
        "moatads.com",
        "mgid.com",
        "adpushup.com",

        // Mobile Ad Networks
        "applovin.com",
        "applvn.com",
        "inmobi.com",
        "ironsrc.com",
        "vungle.com",
        "unity3d.com/ads",
        "unityads.unity3d.com",
        "chartboost.com",
        "adcolony.com",
        "startapp.com",
        "mintegral.com",
        "fyber.com",

        // Popups, Popunders & Aggressive Ads
        "popads.net",
        "propellerads.com",
        "popcash.net",
        "exoclick.com",
        "trafficjunky.com",
        "juicyads.com",
        "yllix.com",
        "clicksor.com",
        "adblade.com",
        "bidvertiser.com",
        "chitika.com",
        "infolinks.com",
        "buysellads.com",
        "buyhitsfast.com"
    )

    // Hardcoded set of tracking, telemetry, web beacons, and analytics domains
    private val TRACKER_HOSTS = setOf(
        // Google Analytics & Tag Manager
        "google-analytics.com",
        "analytics.google.com",
        "ssl.google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",

        // Social Media Pixels & Telemetry
        "connect.facebook.net",
        "pixel.facebook.com",
        "tr.snapchat.com",
        "sc-static.net",
        "analytics.twitter.com",
        "static.ads-twitter.com",
        "ct.pinterest.com",
        "analytics.tiktok.com",
        "ads-sg.tiktok.com",

        // User Tracking & Session Recording
        "hotjar.com",
        "static.hotjar.com",
        "script.hotjar.com",
        "mouseflow.com",
        "fullstory.com",
        "rs.fullstory.com",
        "crazyegg.com",
        "script.crazyegg.com",
        "smartlook.com",
        "clarity.ms",
        "c.clarity.ms",
        "bat.bing.com",

        // Analytics & Product Telemetry
        "segment.io",
        "segment.com",
        "cdn.segment.com",
        "api.segment.io",
        "mixpanel.com",
        "api.mixpanel.com",
        "amplitude.com",
        "api.amplitude.com",
        "api2.amplitude.com",
        "heap.io",
        "heapanalytics.com",
        "optimizely.com",
        "cdn.optimizely.com",
        "scorecardresearch.com",
        "b.scorecardresearch.com",
        "sb.scorecardresearch.com",
        "quantserve.com",
        "edge.quantserve.com",

        // Error & Performance Telemetry
        "newrelic.com",
        "nr-data.net",
        "js-agent.newrelic.com",
        "bam.nr-data.net",
        "bam-cell.nr-data.net",
        "browser.sentry-cdn.com",
        "bugsnag.com",
        "notify.bugsnag.com",

        // Attribution & Mobile Trackers
        "branch.io",
        "api.branch.io",
        "api2.branch.io",
        "appsflyer.com",
        "t.appsflyer.com",
        "adjust.com",
        "app.adjust.com",
        "kochava.com",
        "control.kochava.com",
        "flurry.com",
        "data.flurry.com",

        // Yandex & International Trackers
        "mc.yandex.ru",
        "metrika.yandex.ru",
        "an.yandex.ru",
        "counter.yadro.ru",
        "top-fwz1.mail.ru"
    )

    // Hardcoded set of browser fingerprinting & crypto-mining domains
    private val FINGERPRINT_HOSTS = setOf(
        "fpjs.io",
        "api.fpjs.io",
        "fingerprintjs.com",
        "openfpcdn.io",
        "client.perimeterx.net",
        "coinhive.com",
        "coin-hive.com",
        "authedmine.com",
        "crypto-loot.com"
    )

    // URL Path Patterns that represent ad scripts or tracking beacons
    private val AD_PATH_PATTERNS = listOf(
        Regex("/ads?\\.js", RegexOption.IGNORE_CASE),
        Regex("/prebid.*\\.js", RegexOption.IGNORE_CASE),
        Regex("/gpt.*\\.js", RegexOption.IGNORE_CASE),
        Regex("/adsense", RegexOption.IGNORE_CASE),
        Regex("/fbevents\\.js", RegexOption.IGNORE_CASE),
        Regex("/gtm\\.js", RegexOption.IGNORE_CASE),
        Regex("/analytics\\.js", RegexOption.IGNORE_CASE),
        Regex("/ad-client.*\\.js", RegexOption.IGNORE_CASE),
        Regex("/advert.*\\.js", RegexOption.IGNORE_CASE),
        Regex("/ad_banner", RegexOption.IGNORE_CASE),
        Regex("/adserver", RegexOption.IGNORE_CASE),
        Regex("/banner_ads", RegexOption.IGNORE_CASE),
        Regex("/sponsored-ad", RegexOption.IGNORE_CASE)
    )

    // Tracking query parameters stripped from navigation URLs for privacy
    private val TRACKING_PARAMS = setOf(
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "utm_cid",
        "utm_reader",
        "utm_referrer",
        "utm_name",
        "fbclid",
        "gclid",
        "gclsrc",
        "msclkid",
        "mc_eid",
        "igshid",
        "yclid",
        "twclid",
        "wickedid",
        "dclid",
        "_hsenc",
        "_hsmi",
        "vero_id",
        "mkt_tok",
        "zanpid",
        "irclickid"
    )

    // Cosmetic CSS injected into pages to hide ad elements and collapse blank spaces
    val COSMETIC_FILTER_CSS = """
        /* Shield Browser - Cosmetic Ad Hiding Rules */
        .adsbygoogle,
        [id^="div-gpt-ad"],
        [id^="google_ads_iframe"],
        [id*="google_ads_iframe"],
        [class*="advertisement"],
        [class*="ad-container"],
        [class*="ad-wrapper"],
        [class*="ad-slot"],
        [class*="ad-banner"],
        [class*="ad-box"],
        [class*="ad_wrapper"],
        [class*="sponsored-post"],
        [class*="sponsored-content"],
        [class*="taboola-"],
        [class*="outbrain-"],
        .native-ad,
        .adhesion-ad,
        #banner-ad,
        #sidebar-ad,
        #top-ad,
        #bottom-ad,
        .ad-unit,
        .ad-space,
        .dfp-ad,
        .gpt-ad,
        iframe[src*="doubleclick"],
        iframe[src*="adnxs"],
        iframe[src*="amazon-adsystem"] {
            display: none !important;
            height: 0 !important;
            min-height: 0 !important;
            max-height: 0 !important;
            visibility: hidden !important;
            pointer-events: none !important;
            margin: 0 !important;
            padding: 0 !important;
            opacity: 0 !important;
        }
    """.trimIndent()

    val COSMETIC_FILTER_JS: String by lazy {
        val minifiedCss = COSMETIC_FILTER_CSS.replace("\n", " ").replace("\"", "\\\"")
        """
            (function() {
                try {
                    var styleId = 'shield-browser-cosmetic-filter';
                    if (!document.getElementById(styleId)) {
                        var style = document.createElement('style');
                        style.id = styleId;
                        style.type = 'text/css';
                        style.textContent = "$minifiedCss";
                        (document.head || document.documentElement).appendChild(style);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
    }

    /**
     * Checks whether a requested URL should be blocked based on current Shield configuration.
     */
    fun shouldBlock(url: String, config: ShieldConfig): BlockResult {
        if (!config.isShieldEnabled) {
            return BlockResult(isBlocked = false)
        }

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return BlockResult(isBlocked = false)
        }

        val host = uri.host?.lowercase(Locale.ROOT) ?: return BlockResult(isBlocked = false)
        val path = uri.path ?: ""

        // Check Fingerprinting & Cryptominers
        if (matchesDomain(host, FINGERPRINT_HOSTS)) {
            return BlockResult(
                isBlocked = true,
                category = BlockCategory.FINGERPRINT,
                matchedRule = host
            )
        }

        // Check Ad Networks
        if (config.blockAds) {
            if (matchesDomain(host, AD_HOSTS)) {
                return BlockResult(
                    isBlocked = true,
                    category = BlockCategory.AD,
                    matchedRule = host
                )
            }
        }

        // Check Trackers & Analytics
        if (config.blockTrackers) {
            if (matchesDomain(host, TRACKER_HOSTS)) {
                return BlockResult(
                    isBlocked = true,
                    category = BlockCategory.TRACKER,
                    matchedRule = host
                )
            }
        }

        // Check Script Path Patterns
        if (config.blockAds || config.blockTrackers) {
            for (pattern in AD_PATH_PATTERNS) {
                if (pattern.containsMatchIn(path)) {
                    val category = if (path.contains("analytics", ignoreCase = true) || path.contains("gtm", ignoreCase = true)) {
                        BlockCategory.TRACKER
                    } else {
                        BlockCategory.AD
                    }
                    return BlockResult(
                        isBlocked = true,
                        category = category,
                        matchedRule = pattern.pattern
                    )
                }
            }
        }

        return BlockResult(isBlocked = false)
    }

    /**
     * Helper to match domain or any subdomain against known host set.
     */
    private fun matchesDomain(host: String, domainSet: Set<String>): Boolean {
        if (domainSet.contains(host)) return true
        for (domain in domainSet) {
            if (host.endsWith(".$domain")) {
                return true
            }
        }
        return false
    }

    /**
     * Strips privacy-invasive tracking parameters (utm_*, fbclid, etc.) from destination URLs.
     */
    fun sanitizeUrl(rawUrl: String, stripTrackingParams: Boolean = true): String {
        if (!stripTrackingParams) return rawUrl
        return try {
            val uri = Uri.parse(rawUrl)
            if (uri.queryParameterNames.isEmpty()) return rawUrl

            val cleanBuilder = uri.buildUpon().clearQuery()
            var changed = false
            for (paramName in uri.queryParameterNames) {
                if (TRACKING_PARAMS.contains(paramName.lowercase(Locale.ROOT)) ||
                    paramName.startsWith("utm_", ignoreCase = true)
                ) {
                    changed = true
                    // Drop this tracking parameter!
                } else {
                    val values = uri.getQueryParameters(paramName)
                    for (value in values) {
                        cleanBuilder.appendQueryParameter(paramName, value)
                    }
                }
            }
            if (changed) cleanBuilder.build().toString() else rawUrl
        } catch (e: Exception) {
            rawUrl
        }
    }

    /**
     * Provides an empty dummy response to return from shouldInterceptRequest to block resources.
     */
    fun createEmptyResponse(): android.webkit.WebResourceResponse {
        return android.webkit.WebResourceResponse(
            "text/plain",
            "UTF-8",
            200,
            "OK",
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache"
            ),
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
