package com.example.ui.components.webview

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.URLUtil
import android.webkit.WebStorage
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** A JavaScript dialog requested by the page, awaiting user action. */
internal sealed interface JsDialogRequest {
    data class Alert(val message: String, val result: JsResult) : JsDialogRequest
    data class Confirm(val message: String, val result: JsResult) : JsDialogRequest
    data class Prompt(val message: String, val defaultValue: String, val result: JsPromptResult) : JsDialogRequest
}

/** Bridge receiving password-form submissions from injected page JavaScript. */
internal class LoginDetectedBridge(
    private val onDetected: (url: String, username: String, password: String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onLoginDetected(url: String?, username: String?, password: String?) {
        if (password.isNullOrEmpty()) return
        onDetected(url.orEmpty(), username.orEmpty(), password)
    }
}

/**
 * Hooks every form containing a password field (including ones added later
 * via JavaScript) and reports the submitted username + password once per
 * submit. Values are read at submit time only; nothing is scraped otherwise.
 */
internal const val LOGIN_DETECT_JS =
    "(function(){" +
        "function findUsername(form){" +
        "var s=['input[autocomplete=\"username\"]'," +
        "'input[type=\"email\"]'," +
        "'input[type=\"text\"]'];" +
        "for(var i=0;i<s.length;i++){" +
        "var el=form.querySelector(s[i]);" +
        "if(el&&el.value)return el.value;" +
        "}" +
        "return '';" +
        "}" +
        "function hook(form){" +
        "if(!form||form.__micromiumHooked)return;" +
        "form.__micromiumHooked=true;" +
        "form.addEventListener('submit',function(){" +
        "try{" +
        "var pw=form.querySelector('input[type=\"password\"]');" +
        "if(!pw||!pw.value)return;" +
        "if(typeof MicromiumLoginBridge==='undefined')return;" +
        "MicromiumLoginBridge.onLoginDetected(" +
        "window.location.href,findUsername(form),pw.value);" +
        "}catch(e){}" +
        "});" +
        "}" +
        "function hookAll(){try{" +
        "var forms=document.querySelectorAll('form');" +
        "for(var i=0;i<forms.length;i++)hook(forms[i]);" +
        "}catch(e){}}" +
        "hookAll();" +
        "try{" +
        "new MutationObserver(hookAll).observe(" +
        "document.documentElement,{childList:true,subtree:true});" +
        "}catch(e){}" +
        "})();"

/** Enqueues a URL in the system DownloadManager with cookies forwarded. */
internal fun enqueueDownload(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?
) {
    try {
        val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val downloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
            if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
            setTitle(filename)
            setDescription("Downloading via Micromium")
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    if (!mimeType.isNullOrBlank()) {
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    }
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val destination = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                if (destination != null) setDestinationUri(destination)
            } else {
                @Suppress("DEPRECATION")
                setDestinationInExternalFilesDir(
                    context, Environment.DIRECTORY_DOWNLOADS, filename
                )
            }
        }
        downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading $filename", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
    }
}

/** Starts Safe Browsing init once per process; safe to call repeatedly. */
internal fun initSafeBrowsing(context: Context) {
    try {
        WebViewCompat.startSafeBrowsing(context) { success ->
            android.util.Log.i("ShieldWebView", "Safe Browsing init success=$success")
        }
    } catch (_: Exception) {
    }
}

/** Android runtime permissions backing a page's camera/mic request. */
internal fun osPermissionsFor(webResources: Array<String>): List<String> {
    val needed = mutableListOf<String>()
    if (webResources.contains(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
        needed.add(android.Manifest.permission.RECORD_AUDIO)
    }
    if (webResources.contains(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
        needed.add(android.Manifest.permission.CAMERA)
    }
    return needed
}

/** Applies the theme's Safe Browsing flag to live WebView settings when supported. */
internal fun applySafeBrowsingEnabled(settings: android.webkit.WebSettings, enabled: Boolean) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
        WebSettingsCompat.setSafeBrowsingEnabled(settings, enabled)
    }
}

/** Draws a live preview of the WebView into a downscaled [ImageBitmap]. */
internal fun captureThumbnail(    webView: android.webkit.WebView,
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

/**
 * Forgets one site's local data: expires its cookies and drops its Web Storage
 * origin. History cleanup is handled by the repository layer.
 */internal fun forgetSiteStorage(url: String) {
    val host = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")
    if (host.isBlank()) return

    val cookieManager = CookieManager.getInstance()
    runCatching {
        val cookies = cookieManager.getCookie(url)?.split(";") ?: emptyList()
        cookies.forEach { cookie ->
            val name = cookie.substringBefore("=").trim()
            if (name.isNotEmpty()) {
                cookieManager.setCookie(url, "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT")
                cookieManager.setCookie(".$host", "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT")
            }
        }
        cookieManager.flush()
    }
    runCatching {
        val storage = WebStorage.getInstance()
        listOf("https://$host", "http://$host").forEach { origin ->
            @Suppress("DEPRECATION")
            storage.deleteOrigin(origin)
        }
    }
}

/**
 * Extracts the article from the current page and rewrites the document as a
 * clean reader view. Returns 'READER_OK', or 'NO_ARTICLE' when the page has
 * no extractable long-form text.
 */
internal fun readerModeScript(): String = """
(function(){
  function text(el){return ((el.innerText||'').trim());}
  var article=document.querySelector('article');
  var content='';
  if(article&&text(article).length>300){content=article.innerHTML;}
  else{
    var ps=Array.prototype.slice.call(document.querySelectorAll('main p, article p, p'));
    var good=ps.filter(function(p){return text(p).length>80;});
    if(good.length>=3){content=good.map(function(p){return '<p>'+p.innerHTML+'</p>';}).join('');}
  }
  if(!content)return 'NO_ARTICLE';
  function esc(s){return String(s).replace(/</g,'&lt;');}
  var h=document.querySelector('h1');
  var imgs=Array.prototype.slice.call(document.querySelectorAll('main img, article img'));
  var hero='';
  for(var i=0;i<imgs.length;i++){var src=imgs[i].getAttribute('src');if(src&&src.indexOf('data:')!==0){hero='<img src="'+src+'">';break;}}
  document.open();
  document.write('<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><title>'+esc(document.title)+'</title><style>body{font-family:sans-serif;line-height:1.7;max-width:42em;margin:0 auto;padding:24px 16px;color:#111;background:#fff}img{max-width:100%;height:auto}figure{margin:0}a{color:#0b57d0}@media(prefers-color-scheme:dark){body{color:#e8e8e8;background:#121212}a{color:#7aa7ff}}</style></head><body><h1>'+esc(h?h.innerText:document.title)+'</h1>'+hero+content+'</body></html>');
  document.close();
  return 'READER_OK';
})();
""".trimIndent()
