package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.print.PrintManager
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.BrowserTab
import com.example.model.START_PAGE_URL
import com.example.privacy.BlockedEvent
import com.example.privacy.PrivacyEngine
import com.example.privacy.ShieldConfig
import com.example.ui.WebViewCommand
import com.example.ui.components.webview.JsDialogRequest
import com.example.ui.components.webview.LOGIN_DETECT_JS
import com.example.ui.components.webview.LoginDetectedBridge
import com.example.ui.components.webview.applySafeBrowsingEnabled
import com.example.ui.components.webview.captureThumbnail
import com.example.ui.components.webview.enqueueDownload
import com.example.ui.components.webview.osPermissionsFor
import com.example.ui.components.webview.readerModeScript
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
    onLoginDetected: (url: String, username: String, password: String) -> Unit,
    onPopupUrl: (url: String) -> Unit,
    onReaderUnsupported: () -> Unit,
    textZoom: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // File upload: holds the pending <input type=file> callback while the picker is open.
    var fileChooserCallback by remember {
        mutableStateOf<ValueCallback<Array<Uri>>?>(null)
    }
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = fileChooserCallback
        fileChooserCallback = null
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = data?.clipData?.let { clip ->
                (0 until clip.itemCount).mapNotNull { clip.getItemAt(it)?.uri }.toTypedArray()
            } ?: data?.data?.let { arrayOf(it) }
            callback?.onReceiveValue(uris)
        } else {
            callback?.onReceiveValue(null)
        }
    }

    // JavaScript dialog (alert/confirm/prompt) currently awaiting user action.
    var jsDialog by remember { mutableStateOf<JsDialogRequest?>(null) }

    // Fullscreen custom view (e.g. video) + its exit callback.
    var fullscreen by remember {
        mutableStateOf<Pair<View, WebChromeClient.CustomViewCallback?>?>(null)
    }

    // Site permission requests (camera/mic) awaiting user decision.
    var webPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    // Geolocation request awaiting user decision.
    var geoRequest by remember {
        mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null)
    }

    // Main-frame load failure or renderer crash, shown as a friendly overlay.
    var pageError by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Safe Browsing interstitial awaiting user decision.
    var safeBrowsingHit by remember {
        mutableStateOf<Pair<Int, SafeBrowsingResponse>?>(null)
    }

    // In-page find bar + latest match counts.
    var findBarVisible by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatches by remember { mutableStateOf(0 to 0) }

    // Android runtime permissions backing camera/mic/location grants.
    val osPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val pending = webPermissionRequest
        if (pending != null) {
            webPermissionRequest = null
            val needed = osPermissionsFor(pending.resources)
            if (needed.all { grants[it] == true }) {
                pending.grant(pending.resources)
            } else {
                pending.deny()
            }
        }
        val pendingGeo = geoRequest
        if (pendingGeo != null) {
            geoRequest = null
            val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            pendingGeo.second.invoke(pendingGeo.first, granted, false)
        }
    }

    // Long-press hit target (link url and/or image src) for the context menu.
    var longPressTarget by remember {
        mutableStateOf<Pair<String?, String?>?>(null)
    }

    // Maintain a persistent WebView instance per tab
    val webView = remember(tab.id) {
        createCustomWebView(
            context = context,
            tab = tab,
            shieldConfig = shieldConfig,
            onProgressChanged = onProgressChanged,
            onUrlAndTitleChanged = onUrlAndTitleChanged,
            onBlockedItem = onBlockedItem,
            onThumbnailCaptured = onThumbnailCaptured,
            onLoginDetected = onLoginDetected,
            onPopupUrl = onPopupUrl,
            onLongPressLink = { linkUrl, imageSrc ->
                longPressTarget = linkUrl to imageSrc
            },
            onFileChooserRequest = { callback, params ->
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = callback
                try {
                    val intent = params?.createIntent()
                    if (intent != null) {
                        filePickerLauncher.launch(intent)
                    } else {
                        fileChooserCallback = null
                        callback?.onReceiveValue(null)
                    }
                } catch (_: Exception) {
                    fileChooserCallback = null
                    callback?.onReceiveValue(null)
                }
            },
            onJsDialog = { jsDialog = it },
            onFullscreenChanged = { view, callback -> fullscreen = view?.let { it to callback } },
            onWebPermissionRequest = { webPermissionRequest = it },
            onWebPermissionRequestCancelled = { webPermissionRequest = null },
            onGeolocationRequest = { origin, callback -> geoRequest = origin to callback },
            onGeolocationHide = { geoRequest = null },
            onPageError = { title, description -> pageError = title to description },
            onPageStartedClearErrors = { pageError = null },
            onSafeBrowsingHit = { threatType, callback ->
                safeBrowsingHit = threatType to callback
            },
            onFindResult = { active, total -> findMatches = active to total }
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

    // Safe Browsing toggle (applies live; guarded by WebView support)
    LaunchedEffect(shieldConfig.safeBrowsingEnabled) {
        applySafeBrowsingEnabled(webView.settings, shieldConfig.safeBrowsingEnabled)
    }

    // Page text zoom (50..200 percent)
    LaunchedEffect(textZoom) {
        webView.settings.textZoom = textZoom.coerceIn(50, 200)
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
                is WebViewCommand.ShowFindBar -> {
                    if (command.tabId == tab.id) {
                        findQuery = ""
                        findMatches = 0 to 0
                        findBarVisible = true
                    }
                }
                is WebViewCommand.SetReaderMode -> {
                    if (command.tabId == tab.id && command.enabled) {
                        webView.evaluateJavascript(readerModeScript()) { result ->
                            if (result?.contains("NO_ARTICLE") == true) {
                                Toast.makeText(
                                    context,
                                    "Reader view isn't available for this page",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onReaderUnsupported()
                            }
                        }
                    }
                }
                is WebViewCommand.PrintPage -> {
                    if (command.tabId == tab.id) {
                        val printManager = context.getSystemService(
                            Context.PRINT_SERVICE
                        ) as? PrintManager
                        if (printManager != null) {
                            try {
                                printManager.print(
                                    "Micromium page",
                                    webView.createPrintDocumentAdapter("Micromium"),
                                    null
                                )
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context, "Print failed", Toast.LENGTH_SHORT
                                ).show()
                            }
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

    // JavaScript dialogs requested by the page
    val pendingJsDialog = jsDialog
    if (pendingJsDialog != null) {
        when (pendingJsDialog) {
            is JsDialogRequest.Alert -> {
                AlertDialog(
                    onDismissRequest = {
                        pendingJsDialog.result.cancel()
                        jsDialog = null
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingJsDialog.result.confirm()
                            jsDialog = null
                        }) { Text("OK") }
                    },
                    text = { Text(pendingJsDialog.message) }
                )
            }
            is JsDialogRequest.Confirm -> {
                AlertDialog(
                    onDismissRequest = {
                        pendingJsDialog.result.cancel()
                        jsDialog = null
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingJsDialog.result.confirm()
                            jsDialog = null
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            pendingJsDialog.result.cancel()
                            jsDialog = null
                        }) { Text("Cancel") }
                    },
                    text = { Text(pendingJsDialog.message) }
                )
            }
            is JsDialogRequest.Prompt -> {
                var promptText by remember(pendingJsDialog) {
                    mutableStateOf(pendingJsDialog.defaultValue)
                }
                AlertDialog(
                    onDismissRequest = {
                        pendingJsDialog.result.cancel()
                        jsDialog = null
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingJsDialog.result.confirm(promptText)
                            jsDialog = null
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            pendingJsDialog.result.cancel()
                            jsDialog = null
                        }) { Text("Cancel") }
                    },
                    text = {
                        Column {
                            Text(pendingJsDialog.message)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }
        }
    }

    // Fullscreen custom view (e.g. fullscreen video) in a full-screen dialog
    val fullscreenState = fullscreen
    if (fullscreenState != null) {
        val (fullscreenView, fullscreenCallback) = fullscreenState
        BackHandler {
            fullscreenCallback?.onCustomViewHidden()
            fullscreen = null
        }
        Dialog(
            onDismissRequest = {
                fullscreenCallback?.onCustomViewHidden()
                fullscreen = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = {
                        (fullscreenView.parent as? ViewGroup)?.removeView(fullscreenView)
                        fullscreenView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Camera/mic permission requested by the page
    val pendingWebPermission = webPermissionRequest
    if (pendingWebPermission != null) {
        val origin = try {
            pendingWebPermission.origin.host
                ?: pendingWebPermission.origin.toString()
        } catch (_: Exception) {
            pendingWebPermission.origin.toString()
        }
        val usesMic = pendingWebPermission.resources.contains(
            PermissionRequest.RESOURCE_AUDIO_CAPTURE
        )
        val usesCamera = pendingWebPermission.resources.contains(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE
        )
        val what = when {
            usesMic && usesCamera -> "camera and microphone"
            usesCamera -> "camera"
            usesMic -> "microphone"
            else -> "device features"
        }
        AlertDialog(
            onDismissRequest = {
                pendingWebPermission.deny()
                webPermissionRequest = null
            },
            confirmButton = {
                TextButton(onClick = {
                    val needed = osPermissionsFor(pendingWebPermission.resources)
                    val missing = needed.filter {
                        ContextCompat.checkSelfPermission(context, it) !=
                            PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isEmpty()) {
                        pendingWebPermission.grant(pendingWebPermission.resources)
                        webPermissionRequest = null
                    } else {
                        osPermissionLauncher.launch(missing.toTypedArray())
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingWebPermission.deny()
                    webPermissionRequest = null
                }) { Text("Deny") }
            },
            title = { Text("Allow $what?") },
            text = { Text("$origin wants to use your $what.") }
        )
    }

    // Location requested by the page
    val pendingGeo = geoRequest
    if (pendingGeo != null) {
        AlertDialog(
            onDismissRequest = {
                pendingGeo.second.invoke(pendingGeo.first, false, false)
                geoRequest = null
            },
            confirmButton = {
                TextButton(onClick = {
                    val missing = listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ).filter {
                        ContextCompat.checkSelfPermission(context, it) !=
                            PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isEmpty()) {
                        pendingGeo.second.invoke(pendingGeo.first, true, false)
                        geoRequest = null
                    } else {
                        osPermissionLauncher.launch(missing.toTypedArray())
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingGeo.second.invoke(pendingGeo.first, false, false)
                    geoRequest = null
                }) { Text("Deny") }
            },
            title = { Text("Allow location?") },
            text = { Text("${pendingGeo.first} wants to know your location.") }
        )
    }

    // Load failure / crash overlay with reload
    val currentPageError = pageError
    if (currentPageError != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .testTag("page_error_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentPageError.first,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentPageError.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { webView.reload() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reload")
                }
            }
        }
    }

    // Safe Browsing interstitial
    val pendingThreat = safeBrowsingHit
    if (pendingThreat != null) {
        // Framework threat codes: 1 = malware, 2 = phishing, 3 = unwanted
        // software, 4 = harmful billing (0/unknown falls through to generic).
        val threatName = when (pendingThreat.first) {
            1 -> "malware"
            2 -> "deceptive content"
            3 -> "unwanted software"
            4 -> "harmful billing"
            else -> "a known threat"
        }
        AlertDialog(
            onDismissRequest = {
                pendingThreat.second.backToSafety(true)
                safeBrowsingHit = null
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingThreat.second.backToSafety(true)
                    safeBrowsingHit = null
                }) { Text("Go back") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingThreat.second.proceed(true)
                    safeBrowsingHit = null
                }) { Text("Continue anyway") }
            },
            title = { Text("Deceptive site ahead") },
            text = {
                Text(
                    "This site may contain $threatName. " +
                        "Micromium blocked it to protect you."
                )
            }
        )
    }

    // In-page find bar
    if (findBarVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("find_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    BasicTextField(
                        value = findQuery,
                        onValueChange = {
                            findQuery = it
                            if (it.isEmpty()) {
                                webView.clearMatches()
                                findMatches = 0 to 0
                            } else {
                                webView.findAllAsync(it)
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("find_query"),
                        decorationBox = { innerTextField ->
                            if (findQuery.isEmpty()) {
                                Text(
                                    text = "Find in page",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    )
                    val (findActive, findTotal) = findMatches
                    if (findQuery.isNotEmpty()) {
                        Text(
                            text = if (findTotal > 0) "$findActive/$findTotal" else "0/0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = { webView.findNext(false) },
                        modifier = Modifier.testTag("find_prev")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Previous match"
                        )
                    }
                    IconButton(
                        onClick = { webView.findNext(true) },
                        modifier = Modifier.testTag("find_next")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Next match"
                        )
                    }
                    IconButton(
                        onClick = {
                            webView.clearMatches()
                            findQuery = ""
                            findBarVisible = false
                        },
                        modifier = Modifier.testTag("find_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close find bar"
                        )
                    }
                }
            }
        }
    }

    // Long-press link/image context menu
    val longPress = longPressTarget
    if (longPress != null) {
        val (linkUrl, imageSrc) = longPress
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { longPressTarget = null },
            confirmButton = {},
            title = {
                Text(
                    text = linkUrl ?: imageSrc ?: "",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
            },
            text = {
                Column {
                    if (linkUrl != null) {
                        TextButton(
                            onClick = {
                                onPopupUrl(linkUrl)
                                longPressTarget = null
                            },
                            modifier = Modifier.testTag("link_menu_open_tab")
                        ) { Text("Open in new tab") }
                        TextButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(linkUrl))
                                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                longPressTarget = null
                            },
                            modifier = Modifier.testTag("link_menu_copy")
                        ) { Text("Copy link") }
                        TextButton(
                            onClick = {
                                try {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, linkUrl)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(share, "Share link")
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
                                }
                                longPressTarget = null
                            },
                            modifier = Modifier.testTag("link_menu_share")
                        ) { Text("Share link") }
                    }
                    if (imageSrc != null) {
                        TextButton(
                            onClick = {
                                onPopupUrl(imageSrc)
                                longPressTarget = null
                            },
                            modifier = Modifier.testTag("link_menu_open_image")
                        ) { Text("Open image in new tab") }
                        TextButton(
                            onClick = {
                                enqueueDownload(
                                    context,
                                    imageSrc,
                                    webView.settings.userAgentString,
                                    null,
                                    null
                                )
                                longPressTarget = null
                            },
                            modifier = Modifier.testTag("link_menu_download_image")
                        ) { Text("Download image") }
                    }
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createCustomWebView(
    context: Context,
    tab: BrowserTab,
    shieldConfig: ShieldConfig,
    onProgressChanged: (Int) -> Unit,
    onUrlAndTitleChanged: (url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onBlockedItem: (BlockedEvent) -> Unit,
    onThumbnailCaptured: (tabId: String, thumbnail: ImageBitmap) -> Unit,
    onLoginDetected: (url: String, username: String, password: String) -> Unit,
    onPopupUrl: (url: String) -> Unit,
    onLongPressLink: (linkUrl: String?, imageSrc: String?) -> Unit,
    onFileChooserRequest: (
        callback: ValueCallback<Array<Uri>>?,
        params: WebChromeClient.FileChooserParams?
    ) -> Unit,
    onJsDialog: (JsDialogRequest) -> Unit,
    onFullscreenChanged: (view: View?, callback: WebChromeClient.CustomViewCallback?) -> Unit,
    onWebPermissionRequest: (PermissionRequest) -> Unit,
    onWebPermissionRequestCancelled: () -> Unit,
    onGeolocationRequest: (origin: String, callback: GeolocationPermissions.Callback) -> Unit,
    onGeolocationHide: () -> Unit,
    onPageError: (title: String, description: String) -> Unit,
    onPageStartedClearErrors: () -> Unit,
    onSafeBrowsingHit: (threatType: Int, callback: SafeBrowsingResponse) -> Unit,
    onFindResult: (activeMatch: Int, matchCount: Int) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                // Darkens page content when the system is in night mode.
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, true)
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                applySafeBrowsingEnabled(this, shieldConfig.safeBrowsingEnabled)
            }
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, !shieldConfig.blockThirdPartyCookies)

        // Reports password-form submissions so the app can offer to save the login.
        addJavascriptInterface(
            LoginDetectedBridge { url, username, password ->
                onLoginDetected(url, username, password)
            },
            "MicromiumLoginBridge"
        )

        // In-page find match counts.
        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            onFindResult(activeMatchOrdinal, numberOfMatches)
        }

        // Route file downloads to the system DownloadManager (no storage
        // permission needed: MediaStore on Q+, app-private dir below that).
        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            enqueueDownload(context, url, userAgent, contentDisposition, mimeType)
        }

        // Long-press on links/images: report the hit target for the context menu.
        val longPressHandler = Handler(Looper.getMainLooper()) { msg ->
            val url = msg.data.getString("url")
            val src = msg.data.getString("src")
            if (url != null || src != null) {
                onLongPressLink(url, src)
            }
            true
        }
        setOnLongClickListener { v ->
            (v as? WebView)?.requestFocusNodeHref(longPressHandler.obtainMessage())
            false
        }

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

            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                onFileChooserRequest(filePathCallback, fileChooserParams)
                return true
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                if (result == null) return false
                onJsDialog(JsDialogRequest.Alert(message.orEmpty(), result))
                return true
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                if (result == null) return false
                onJsDialog(JsDialogRequest.Confirm(message.orEmpty(), result))
                return true
            }

            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                if (result == null) return false
                onJsDialog(JsDialogRequest.Prompt(message.orEmpty(), defaultValue.orEmpty(), result))
                return true
            }

            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                if (view == null) {
                    callback?.onCustomViewHidden()
                    return
                }
                onFullscreenChanged(view, callback)
            }

            override fun onHideCustomView() {
                onFullscreenChanged(null, null)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                onWebPermissionRequest(request)
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                // The page withdrew the request; nothing to show.
                onWebPermissionRequestCancelled()
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (origin == null || callback == null) return
                onGeolocationRequest(origin, callback)
            }

            override fun onGeolocationPermissionsHidePrompt() {
                onGeolocationHide()
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                if (view == null || resultMsg == null) return false
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                // Transient receiver: hands the popup URL to a real tab, then dies.
                val popup = WebView(view.context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            v: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://") || url.startsWith("https://") ||
                                url.startsWith("about:")
                            ) {
                                onPopupUrl(url)
                                try {
                                    v?.stopLoading()
                                    v?.destroy()
                                } catch (_: Exception) {
                                }
                                return true
                            }
                            return false
                        }
                    }
                }
                transport.webView = popup
                resultMsg.sendToTarget()
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (request == null) return null
                val url = request.url.toString()

                // Check against hardcoded AdBlock & Tracking Protection lists.
                // YouTube pages pass their context so YouTube ad blocking stays
                // on its own switch and never slows down video loading.
                val pageHost = runCatching { Uri.parse(tab.url).host.orEmpty() }.getOrDefault("")
                val blockResult = PrivacyEngine.shouldBlock(
                    url,
                    shieldConfig,
                    isYouTubePage = PrivacyEngine.isYouTubeHost(pageHost)
                )
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

                // HTTPS-First: upgrade plain-http navigations when enabled
                if (shieldConfig.isShieldEnabled && shieldConfig.forceHttps &&
                    rawUrl.startsWith("http://")
                ) {
                    view.loadUrl("https://" + rawUrl.removePrefix("http://"))
                    return true
                }

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
                onPageStartedClearErrors()
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

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && error != null) {
                    val description = error.description?.toString().orEmpty()
                    onPageError(
                        "Page failed to load",
                        if (description.isNotBlank()) description
                        else "Error ${error.errorCode}"
                    )
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true && errorResponse != null) {
                    onPageError(
                        "Page failed to load",
                        "HTTP ${errorResponse.statusCode}: ${errorResponse.reasonPhrase}"
                    )
                }
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                if (detail?.didCrash() == true) {
                    onPageError("Page crashed", "The renderer stopped unexpectedly. Reload to try again.")
                }
                // Handled here (reload overlay); do not crash the app.
                return true
            }

            override fun onSafeBrowsingHit(
                view: WebView?,
                request: WebResourceRequest?,
                threatType: Int,
                callback: SafeBrowsingResponse?
            ) {
                if (callback == null) return
                onSafeBrowsingHit(threatType, callback)
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

                // Hook password forms so submitted logins can be offered for saving
                view?.evaluateJavascript(LOGIN_DETECT_JS, null)

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
