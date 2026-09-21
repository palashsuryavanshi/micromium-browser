package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import com.example.ui.theme.MenuBackgroundDark
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserTab
import com.example.privacy.ShieldConfig

@Composable
fun ChromiumOmnibox(
    activeTab: BrowserTab?,
    openTabsCount: Int,
    inputText: String,
    isEditing: Boolean,
    shieldConfig: ShieldConfig,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    isBookmarked: Boolean,
    onInputTextChange: (String) -> Unit,
    onStartEditing: () -> Unit,
    onSubmitQuery: (String) -> Unit,
    onCancelEditing: () -> Unit,
    onShieldClick: () -> Unit,
    onTabSwitcherClick: () -> Unit,
    onNewTabClick: () -> Unit,
    onNewIncognitoTabClick: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReloadOrStopClick: () -> Unit,
    onHomeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onToggleDesktopSite: () -> Unit,
    onToggleReaderMode: () -> Unit,
    onFindInPageClick: (tabId: String) -> Unit,
    onPrintClick: (tabId: String) -> Unit,
    recentlyClosedCount: Int,
    onReopenClosedTab: () -> Unit,
    onClearDataClick: () -> Unit,
    onShareClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    // Menu open animation progress (0 = closed, 1 = open): springy scale + fade from top-end
    val menuOpenProgress by animateFloatAsState(
        targetValue = if (isMenuExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "menu_open"
    )
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().imePadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Privacy Shield Button with Badge
            IconButton(
                onClick = onShieldClick,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("shield_button")
            ) {
                val blockedCount = activeTab?.totalBlockedOnPage ?: 0
                val shieldActive = shieldConfig.isShieldEnabled

                BadgedBox(
                    badge = {
                        if (shieldActive && blockedCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.testTag("shield_badge")
                            ) {
                                Text(
                                    text = if (blockedCount > 99) "99+" else blockedCount.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Privacy Shield",
                        tint = if (shieldActive) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Omnibox URL / Search input box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isEditing
                    ) {
                        onStartEditing()
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (!isEditing) {
                    // Display mode: lock / globe icon + domain
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentUrl = activeTab?.url ?: ""
                        val isHttps = currentUrl.startsWith("https://")
                        val isStartPage = activeTab?.isStartPage == true

                        Icon(
                            imageVector = when {
                                isStartPage -> Icons.Default.Search
                                isHttps -> Icons.Default.Lock
                                else -> Icons.Default.Public
                            },
                            contentDescription = if (isHttps) "Secure connection" else "Address",
                            tint = when {
                                isHttps -> MaterialTheme.colorScheme.secondary
                                isStartPage -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val displayText = when {
                            isStartPage -> "Search or Enter URL"
                            else -> {
                                try {
                                    val uri = Uri.parse(currentUrl)
                                    uri.host ?: currentUrl
                                } catch (e: Exception) {
                                    currentUrl
                                }
                            }
                        }

                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isStartPage) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Edit mode: text field + clear button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputTextChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("url_text_field"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    focusManager.clearFocus()
                                    onSubmitQuery(inputText)
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty() && !isEditing) {
                                    Text(
                                        text = "Search with DuckDuckGo or enter URL",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (inputText.isNotEmpty()) {
                            IconButton(
                                onClick = { onInputTextChange("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear input",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Tab Switcher Button (Chromium square badge)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onTabSwitcherClick() }
                    .testTag("tab_switcher_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (openTabsCount > 99) ":D" else openTabsCount.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Chromium 3-Dot Overflow Menu
            Box {
                IconButton(
                    onClick = { isMenuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    modifier = Modifier
                        .width(264.dp)
                        .graphicsLayer {
                            val scale = 0.92f + 0.08f * menuOpenProgress
                            scaleX = scale
                            scaleY = scale
                            alpha = menuOpenProgress
                            transformOrigin = TransformOrigin(0.9f, 0f)
                        },
                    containerColor = MenuBackgroundDark
                ) {
                    // Horizontal navigation row (Home, Back, Forward, Reload, Bookmark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isMenuExpanded = false
                                onHomeClick()
                            },
                            modifier = Modifier.testTag("menu_nav_home")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                isMenuExpanded = false
                                onBackClick()
                            },
                            enabled = canGoBack,
                            modifier = Modifier.testTag("menu_nav_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                isMenuExpanded = false
                                onForwardClick()
                            },
                            enabled = canGoForward,
                            modifier = Modifier.testTag("menu_nav_forward")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                isMenuExpanded = false
                                onReloadOrStopClick()
                            },
                            modifier = Modifier.testTag("menu_nav_reload")
                        ) {
                            Icon(
                                imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                contentDescription = if (isLoading) "Stop" else "Reload",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                isMenuExpanded = false
                                onBookmarkClick()
                            },
                            modifier = Modifier.testTag("menu_nav_bookmark")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = { Text("New Tab") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onNewTabClick()
                        },
                        modifier = Modifier.testTag("menu_new_tab")
                    )

                    DropdownMenuItem(
                        text = { Text("New Incognito Tab") },
                        leadingIcon = {
                            Icon(Icons.Outlined.VisibilityOff, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onNewIncognitoTabClick()
                        },
                        modifier = Modifier.testTag("menu_new_incognito")
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        leadingIcon = {
                            Icon(Icons.Default.Bookmark, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onBookmarksClick()
                        },
                        modifier = Modifier.testTag("menu_bookmarks")
                    )

                    DropdownMenuItem(
                        text = { Text("History") },
                        leadingIcon = {
                            Icon(Icons.Default.History, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onHistoryClick()
                        },
                        modifier = Modifier.testTag("menu_history")
                    )

                    DropdownMenuItem(
                        text = { Text("Downloads") },
                        leadingIcon = {
                            Icon(Icons.Default.Download, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            openSystemDownloads(context)
                        },
                        modifier = Modifier.testTag("menu_downloads")
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Desktop site")
                                if (activeTab?.isDesktopSite == true) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Enabled",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Computer, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onToggleDesktopSite()
                        },
                        modifier = Modifier.testTag("menu_desktop_site")
                    )

                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reader view")
                                if (activeTab?.isReaderMode == true) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Enabled",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Article, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onToggleReaderMode()
                        },
                        enabled = activeTab?.isStartPage == false,
                        modifier = Modifier.testTag("menu_reader_view")
                    )

                    DropdownMenuItem(
                        text = { Text("Find in page") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            activeTab?.id?.let { onFindInPageClick(it) }
                        },
                        modifier = Modifier.testTag("menu_find_in_page")
                    )

                    DropdownMenuItem(
                        text = { Text("Print page") },
                        onClick = {
                            isMenuExpanded = false
                            activeTab?.id?.let { onPrintClick(it) }
                        },
                        modifier = Modifier.testTag("menu_print")
                    )

                    DropdownMenuItem(
                        text = { Text("Share via QR code") },
                        onClick = {
                            isMenuExpanded = false
                            showQrDialog = true
                        },
                        enabled = !activeTab?.url.isNullOrBlank(),
                        modifier = Modifier.testTag("menu_qr_share")
                    )

                    DropdownMenuItem(
                        text = { Text("Reopen closed tab") },
                        onClick = {
                            isMenuExpanded = false
                            onReopenClosedTab()
                        },
                        enabled = recentlyClosedCount > 0,
                        modifier = Modifier.testTag("menu_reopen_tab")
                    )

                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onSettingsClick()
                        },
                        modifier = Modifier.testTag("menu_settings")
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = { Text("Share Page") },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onShareClick()
                        },
                        enabled = activeTab?.isStartPage == false
                    )

                    DropdownMenuItem(
                        text = { Text("Clear Browsing Data") },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        },
                        onClick = {
                            isMenuExpanded = false
                            onClearDataClick()
                        },
                        modifier = Modifier.testTag("menu_clear_data")
                    )
                }
            }
        }
    }

    // QR code sharing dialog for the current page URL
    if (showQrDialog) {
        val qrUrl = activeTab?.url.orEmpty()
        val qrBitmap = remember(qrUrl) { qrBitmapFor(qrUrl, 512) }
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) { Text("Close") }
            },
            title = { Text("Share via QR code") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "QR code for $qrUrl",
                            modifier = Modifier
                                .size(240.dp)
                                .testTag("qr_image")
                        )
                    } else {
                        Text("Could not generate a code for this page.")
                    }
                    Text(
                        text = qrUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }
}

/** Renders [content] as a black-on-white QR bitmap, or null when it fails. */
private fun qrBitmapFor(
    content: String,
    sizePx: Int
): androidx.compose.ui.graphics.ImageBitmap? {
    if (content.isBlank()) return null
    return try {
        val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(
            content,
            com.google.zxing.BarcodeFormat.QR_CODE,
            sizePx,
            sizePx
        )
        val pixels = IntArray(sizePx * sizePx)
        val black = android.graphics.Color.BLACK
        val white = android.graphics.Color.WHITE
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                pixels[y * sizePx + x] = if (matrix.get(x, y)) black else white
            }
        }
        Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
