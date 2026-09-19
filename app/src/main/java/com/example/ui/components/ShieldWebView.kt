package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.BrowserTab
import com.example.model.START_PAGE_URL
import com.example.privacy.BlockedEvent
import com.example.privacy.PrivacyEngine
import com.example.privacy.ShieldConfig
import com.example.ui.WebViewCommand
import kotlinx.coroutines.flow.SharedFlow

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ShieldWebView(
    tab: BrowserTab,
    shieldConfig: ShieldConfig,
    webViewCommands: SharedFlow<WebViewCommand>,
    onProgressChanged: (Int) -> Unit,
    onUrlAndTitleChanged: (url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onBlockedItem: (BlockedEvent) -> Unit,
    onThumbnailCaptured: (tabId: String, thumbnail: ImageBitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Maintain a persistent WebView instance per tab
    val webView = remember(tab.id) {
        createCustomWebView(
            context = context,
            tab = tab,
            shieldConfig = shieldConfig,
            onProgressChanged = onProgressChanged,
            onUrlAndTitleChanged = onUrlAndTitleChanged,
            onBlockedItem = onBlockedItem,
            onThumbnailCaptured = onThumbnailCaptured
        )
    }

    // Configure user agent for desktop mode
    LaunchedEffect(tab.isDesktopSite) {
        val settings = webView.settings
        if (tab.isDesktopSite) {
            settings.userAgentString = DESKTOP_USER_AGENT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        } else {
            settings.userAgentString = null // Reset to default mobile Chromium UA
            settings.useWideViewPort = false
            settings.loadWithOverviewMode = false
        }
    }

    // Configure third-party cookie blocking
    LaunchedEffect(shieldConfig.blockThirdPartyCookies) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptThirdPartyCookies(webView, !shieldConfig.blockThirdPartyCookies)
    }

    // Listen for commands targeting this tab
    LaunchedEffect(tab.id) {
        webViewCommands.collect { command ->
            when (command) {
                is WebViewCommand.LoadUrl -> {
                    if (command.tabId == tab.id) {
                        webView.loadUrl(command.url)
                    }
                }
                is WebViewCommand.GoBack -> {
                    if (command.tabId == tab.id && webView.canGoBack()) {
                        webView.goBack()
                    }
                }
                is WebViewCommand.GoForward -> {
                    if (command.tabId == tab.id && webView.canGoForward()) {
                        webView.goForward()
                    }
                }
                is WebViewCommand.Reload -> {
                    if (command.tabId == tab.id) {
                        webView.reload()
                    }
                }
                is WebViewCommand.Stop -> {
                    if (command.tabId == tab.id) {
                        webView.stopLoading()
                    }
                }
                is WebViewCommand.CaptureThumbnail -> {
                    if (command.tabId == tab.id) {
                        webView.post {
                            captureThumbnail(webView, tab.id, onThumbnailCaptured)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(tab.id) {
        onDispose {
            // Stop loading when destroyed
            webView.stopLoading()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
            .fillMaxSize()
            .testTag("shield_webview_${tab.id}")
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createCustomWebView(
    context: Context,
    tab: BrowserTab,
    shieldConfig: ShieldConfig,
    onProgressChanged: (Int) -> Unit,
    onUrlAndTitleChanged: (url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onBlockedItem: (BlockedEvent) -> Unit,
    onThumbnailCaptured: (tabId: String, thumbnail: ImageBitmap) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, !shieldConfig.blockThirdPartyCookies)

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                val currentUrl = view?.url ?: tab.url
                onUrlAndTitleChanged(
                    currentUrl,
                    title ?: currentUrl,
                    view?.canGoBack() ?: false,
                    view?.canGoForward() ?: false
                )
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (request == null) return null
                val url = request.url.toString()

                // Check against hardcoded AdBlock & Tracking Protection lists
                val blockResult = PrivacyEngine.shouldBlock(url, shieldConfig)
                if (blockResult.isBlocked) {
                    val uri = request.url
                    val host = uri.host ?: url
                    val event = BlockedEvent(
                        url = url,
                        host = host,
                        category = blockResult.category
                    )
                    // Post event to UI
                    view?.post { onBlockedItem(event) }

                    // Return empty dummy response to cancel network connection immediately
                    return PrivacyEngine.createEmptyResponse()
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request == null || view == null) return false
                val rawUrl = request.url.toString()

                // Handle external application schemes
                if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://") && !rawUrl.startsWith("about:")) {
                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl))
                        view.context.startActivity(intent)
                        true
                    } catch (e: Exception) {
                        true // Consume anyway to avoid WebView crash on unknown intent
                    }
                }

                // Strip tracking query parameters if configured
                val sanitizedUrl = PrivacyEngine.sanitizeUrl(rawUrl, shieldConfig.stripTrackingParams)
                if (sanitizedUrl != rawUrl) {
                    view.loadUrl(sanitizedUrl)
                    return true
                }

                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (url != null) {
                    onUrlAndTitleChanged(
                        url,
                        view?.title ?: url,
                        view?.canGoBack() ?: false,
                        view?.canGoForward() ?: false
                    )
                }

                // Inject cosmetic ad element hiding early
                if (shieldConfig.isShieldEnabled && shieldConfig.cosmeticFiltering) {
                    view?.evaluateJavascript(PrivacyEngine.COSMETIC_FILTER_JS, null)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null) {
                    onUrlAndTitleChanged(
                        url,
                        view?.title ?: url,
                        view?.canGoBack() ?: false,
                        view?.canGoForward() ?: false
                    )
                }

                // Re-inject cosmetic filter script to clean dynamically loaded ad containers
                if (shieldConfig.isShieldEnabled && shieldConfig.cosmeticFiltering) {
                    view?.evaluateJavascript(PrivacyEngine.COSMETIC_FILTER_JS, null)
                }

                // Capture live thumbnail after the page finishes rendering
                view?.post {
                    captureThumbnail(view, tab.id, onThumbnailCaptured)
                }
            }
        }

        // Initial load if tab has a valid web URL
        if (tab.url.isNotBlank() && tab.url != START_PAGE_URL && tab.url != "about:blank") {
            loadUrl(tab.url)
        }
    }
}

/**
 * Draws a live preview of the WebView into a downscaled [ImageBitmap].
 * Safe to call on the main thread after the page has been laid out.
 */
private fun captureThumbnail(
    webView: WebView,
    tabId: String,
    onThumbnailCaptured: (tabId: String, thumbnail: ImageBitmap) -> Unit
) {
    val w = webView.width
    val h = webView.height
    if (w <= 0 || h <= 0) return

    val maxDim = 320f
    val scale = minOf(maxDim / w.toFloat(), maxDim / h.toFloat(), 1f)
    val targetW = (w * scale).toInt().coerceAtLeast(1)
    val targetH = (h * scale).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(scale, scale)
    webView.draw(canvas)
    onThumbnailCaptured(tabId, bitmap.asImageBitmap())
}
