package com.example

import com.example.privacy.BlockCategory
import com.example.privacy.PrivacyEngine
import com.example.privacy.ShieldConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrivacyEngineTest {

    private val defaultConfig = ShieldConfig()

    @Test
    fun `ad networks are blocked`() {
        val testUrls = listOf(
            "https://securepubads.g.doubleclick.net/gampad/ads?gdfp_req=1",
            "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            "https://ads.pubmatic.com/AdServer/js/pwt.js",
            "https://cdn.taboola.com/libtrc/unip/loader.js",
            "https://widgets.outbrain.com/outbrain.js",
            "https://static.criteo.net/js/ld/ld.js"
        )

        for (url in testUrls) {
            val result = PrivacyEngine.shouldBlock(url, defaultConfig)
            assertTrue("Expected $url to be blocked", result.isBlocked)
            assertEquals(BlockCategory.AD, result.category)
        }
    }

    @Test
    fun `trackers and analytics are blocked`() {
        val testUrls = listOf(
            "https://www.google-analytics.com/analytics.js",
            "https://www.googletagmanager.com/gtm.js?id=GTM-XXXX",
            "https://connect.facebook.net/en_US/fbevents.js",
            "https://static.hotjar.com/c/hotjar-12345.js?sv=6",
            "https://cdn.segment.com/analytics.js/v1/xyz/analytics.min.js",
            "https://mc.yandex.ru/metrika/tag.js"
        )

        for (url in testUrls) {
            val result = PrivacyEngine.shouldBlock(url, defaultConfig)
            assertTrue("Expected $url to be blocked", result.isBlocked)
            assertEquals(BlockCategory.TRACKER, result.category)
        }
    }

    @Test
    fun `fingerprinting and crypto miners are blocked`() {
        val result = PrivacyEngine.shouldBlock("https://api.fpjs.io/v3/telemetry", defaultConfig)
        assertTrue(result.isBlocked)
        assertEquals(BlockCategory.FINGERPRINT, result.category)
    }

    @Test
    fun `legitimate websites are NOT blocked`() {
        val safeUrls = listOf(
            "https://en.wikipedia.org/wiki/Android_(operating_system)",
            "https://duckduckgo.com/?q=android+privacy",
            "https://news.ycombinator.com/item?id=12345",
            "https://developer.android.com/reference/android/webkit/WebView",
            "https://github.com/torvalds/linux"
        )

        for (url in safeUrls) {
            val result = PrivacyEngine.shouldBlock(url, defaultConfig)
            assertFalse("Expected legitimate URL $url to be allowed", result.isBlocked)
        }
    }

    @Test
    fun `tracking parameters are stripped from URLs`() {
        val dirtyUrl = "https://example.com/article?title=test&utm_source=newsletter&utm_medium=email&fbclid=IwAR123&gclid=CjwKCAi"
        val cleanUrl = PrivacyEngine.sanitizeUrl(dirtyUrl, stripTrackingParams = true)

        assertFalse(cleanUrl.contains("utm_source"))
        assertFalse(cleanUrl.contains("utm_medium"))
        assertFalse(cleanUrl.contains("fbclid"))
        assertFalse(cleanUrl.contains("gclid"))
        assertTrue(cleanUrl.contains("title=test"))
    }

    @Test
    fun `disabling shield allows all traffic`() {
        val disabledConfig = ShieldConfig(isShieldEnabled = false)
        val adUrl = "https://googleads.g.doubleclick.net/pagead/ads"
        val result = PrivacyEngine.shouldBlock(adUrl, disabledConfig)
        assertFalse("Expected ad to be allowed when shield is disabled", result.isBlocked)
    }

    @Test
    fun `youtube ad requests pass through by default`() {
        val adOnYouTube = "https://googleads.g.doubleclick.net/pagead/ads?client=youtube"
        val onPage = PrivacyEngine.shouldBlock(adOnYouTube, defaultConfig, isYouTubePage = true)
        assertFalse("Expected YouTube ad request to be allowed by default", onPage.isBlocked)

        // Same request outside YouTube is still blocked.
        val offPage = PrivacyEngine.shouldBlock(adOnYouTube, defaultConfig, isYouTubePage = false)
        assertTrue("Expected ad request to be blocked off YouTube", offPage.isBlocked)
        assertEquals(BlockCategory.AD, offPage.category)
    }

    @Test
    fun `youtube watch pages are never blocked`() {
        val watchUrls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://m.youtube.com/",
            "https://youtu.be/dQw4w9WgXcQ"
        )
        val strict = defaultConfig.copy(blockYouTubeAds = true)
        for (url in watchUrls) {
            assertFalse(
                "Expected $url to load normally",
                PrivacyEngine.shouldBlock(url, defaultConfig, isYouTubePage = true).isBlocked
            )
            assertFalse(
                "Expected $url to load even with the YouTube switch on",
                PrivacyEngine.shouldBlock(url, strict, isYouTubePage = true).isBlocked
            )
        }
    }

    @Test
    fun `youtube ad beacons are blocked only when the youtube switch is on`() {
        val beacon = "https://www.youtube.com/api/stats/ads?ad_v=1"
        assertFalse(
            "Expected YouTube beacon to pass by default",
            PrivacyEngine.shouldBlock(beacon, defaultConfig, isYouTubePage = true).isBlocked
        )
        val enabled = PrivacyEngine.shouldBlock(
            beacon,
            defaultConfig.copy(blockYouTubeAds = true),
            isYouTubePage = true
        )
        assertTrue("Expected YouTube beacon to be blocked when enabled", enabled.isBlocked)
        assertEquals(BlockCategory.AD, enabled.category)
    }

    @Test
    fun `trackers are still blocked on youtube pages`() {
        val result = PrivacyEngine.shouldBlock(
            "https://www.google-analytics.com/analytics.js",
            defaultConfig,
            isYouTubePage = true
        )
        assertTrue("Expected tracker to be blocked on YouTube pages", result.isBlocked)
        assertEquals(BlockCategory.TRACKER, result.category)
    }
}
