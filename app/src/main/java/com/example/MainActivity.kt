package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BrowserTab
import com.example.privacy.ShieldConfig
import com.example.ui.BrowserViewModel
import com.example.ui.components.BookmarksHistoryDialog
import com.example.ui.components.ChromiumOmnibox
import com.example.ui.components.ChromiumStartPage
import com.example.ui.components.OnboardingScreen
import com.example.ui.components.PageProgressBar
import com.example.ui.components.PrivacyShieldSheet
import com.example.ui.components.SettingsScreen
import com.example.ui.components.ShieldWebView
import com.example.ui.components.TabGridSwitcher
import com.example.ui.components.isBiometricEnrolled
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BrowserViewModel = viewModel()
            val darkTheme by viewModel.darkTheme.collectAsState()
            MyApplicationTheme(darkTheme = darkTheme) {
                BrowserApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserApp(
    viewModel: BrowserViewModel = viewModel()
) {
    val context = LocalContext.current

    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    val omniboxText by viewModel.omniboxText.collectAsState()
    val isOmniboxEditing by viewModel.isOmniboxEditing.collectAsState()

    val isTabSwitcherVisible by viewModel.isTabSwitcherVisible.collectAsState()
    val isShieldSheetVisible by viewModel.isShieldSheetVisible.collectAsState()
    val isBookmarksHistoryDialogVisible by viewModel.isBookmarksHistoryDialogVisible.collectAsState()
    val isSettingsDialogVisible by viewModel.isSettingsDialogVisible.collectAsState()
    val selectedDialogTab by viewModel.selectedDialogTab.collectAsState()

    val shieldConfig by viewModel.shieldConfig.collectAsState()
    val toolbarAtBottom by viewModel.toolbarAtBottom.collectAsState()
    val onboardingComplete by viewModel.onboardingComplete.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()

    val selectedSearchEngineUrl by viewModel.selectedSearchEngineUrl.collectAsState()
    val customSearchEngines by viewModel.customSearchEngines.collectAsState()

    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()
    val thumbnails by viewModel.thumbnails.collectAsState()
    val vaultHasPassword by viewModel.hasVaultPassword.collectAsState()
    val vaultUnlocked by viewModel.vaultUnlocked.collectAsState()
    val vaultLogins by viewModel.vaultLogins.collectAsState()
    val vaultBusy by viewModel.vaultBusy.collectAsState()
    val vaultError by viewModel.vaultError.collectAsState()
    val vaultLastImportCount by viewModel.vaultLastImportCount.collectAsState()
    val biometricAllowed by viewModel.biometricAllowed.collectAsState()
    // Clear Data Dialog state
    var showClearDataDialog by remember { mutableStateOf(false) }
    var clearCacheSelected by remember { mutableStateOf(true) }
    var clearCookiesSelected by remember { mutableStateOf(true) }
    var clearHistorySelected by remember { mutableStateOf(true) }

    // Bottom sheet states
    val shieldSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bookmarksSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Hardware Back Button Behavior
    BackHandler(enabled = true) {
        when {
            isTabSwitcherVisible -> viewModel.setTabSwitcherVisible(false)
            isShieldSheetVisible -> viewModel.setShieldSheetVisible(false)
            isBookmarksHistoryDialogVisible -> viewModel.setBookmarksHistoryDialogVisible(false)
            isSettingsDialogVisible -> viewModel.setSettingsDialogVisible(false)
            isOmniboxEditing -> viewModel.setOmniboxEditing(false)
            activeTab?.canGoBack == true -> viewModel.goBack()
            activeTab?.isStartPage == false -> viewModel.goHome()
            tabs.size > 1 -> viewModel.closeTab(activeTabId)
            else -> {
                // If on start page with only 1 tab, minimize/exit
                (context as? ComponentActivity)?.finish()
            }
        }
    }

    val omniboxCallbacks = object {
        val onShareClick: () -> Unit = {
            activeTab?.url?.let { url ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, url)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share link"))
            }
        }
    }

    // First-run onboarding gate
    if (!onboardingComplete) {
        OnboardingScreen(
            onFinish = { answers ->
                viewModel.finishOnboarding(answers)
            },
            onSkip = {
                viewModel.skipOnboarding()
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isTabSwitcherVisible && !toolbarAtBottom) {
                BrowsingToolbar(
                    activeTab = activeTab,
                    openTabsCount = tabs.size,
                    inputText = omniboxText,
                    isEditing = isOmniboxEditing,
                    shieldConfig = shieldConfig,
                    canGoBack = activeTab?.canGoBack ?: false,
                    canGoForward = activeTab?.canGoForward ?: false,
                    isLoading = activeTab?.isLoading ?: false,
                    isBookmarked = bookmarks.any { it.url == activeTab?.url },
                    atTop = true,
                    onInputTextChange = { viewModel.setOmniboxText(it) },
                    onStartEditing = { viewModel.setOmniboxEditing(true) },
                    onSubmitQuery = { viewModel.submitUrlOrQuery(it) },
                    onCancelEditing = { viewModel.setOmniboxEditing(false) },
                    onShieldClick = { viewModel.setShieldSheetVisible(true) },
                    onTabSwitcherClick = { viewModel.setTabSwitcherVisible(true) },
                    onNewTabClick = { viewModel.openNewTab() },
                    onNewIncognitoTabClick = { viewModel.openNewTab(isIncognito = true) },
                    onBackClick = { viewModel.goBack() },
                    onForwardClick = { viewModel.goForward() },
                    onReloadOrStopClick = {
                        if (activeTab?.isLoading == true) {
                            viewModel.stopLoading()
                        } else {
                            viewModel.reload()
                        }
                    },
                    onHomeClick = { viewModel.goHome() },
                    onBookmarkClick = {
                        if (activeTab?.isStartPage == true) {
                            viewModel.setBookmarksHistoryDialogVisible(true, 0)
                        } else {
                            viewModel.toggleBookmarkCurrentPage()
                            Toast.makeText(context, "Bookmark updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBookmarksClick = { viewModel.setBookmarksHistoryDialogVisible(true, 0) },
                    onHistoryClick = { viewModel.setBookmarksHistoryDialogVisible(true, 1) },
                    onToggleDesktopSite = { viewModel.toggleDesktopSite() },
                    onClearDataClick = { showClearDataDialog = true },
                    onShareClick = omniboxCallbacks.onShareClick,
                    onSettingsClick = { viewModel.setSettingsDialogVisible(true) }
                )
            }
        },
        bottomBar = {
            if (!isTabSwitcherVisible && toolbarAtBottom) {
                BrowsingToolbar(
                    activeTab = activeTab,
                    openTabsCount = tabs.size,
                    inputText = omniboxText,
                    isEditing = isOmniboxEditing,
                    shieldConfig = shieldConfig,
                    canGoBack = activeTab?.canGoBack ?: false,
                    canGoForward = activeTab?.canGoForward ?: false,
                    isLoading = activeTab?.isLoading ?: false,
                    isBookmarked = bookmarks.any { it.url == activeTab?.url },
                    atTop = false,
                    onInputTextChange = { viewModel.setOmniboxText(it) },
                    onStartEditing = { viewModel.setOmniboxEditing(true) },
                    onSubmitQuery = { viewModel.submitUrlOrQuery(it) },
                    onCancelEditing = { viewModel.setOmniboxEditing(false) },
                    onShieldClick = { viewModel.setShieldSheetVisible(true) },
                    onTabSwitcherClick = { viewModel.setTabSwitcherVisible(true) },
                    onNewTabClick = { viewModel.openNewTab() },
                    onNewIncognitoTabClick = { viewModel.openNewTab(isIncognito = true) },
                    onBackClick = { viewModel.goBack() },
                    onForwardClick = { viewModel.goForward() },
                    onReloadOrStopClick = {
                        if (activeTab?.isLoading == true) {
                            viewModel.stopLoading()
                        } else {
                            viewModel.reload()
                        }
                    },
                    onHomeClick = { viewModel.goHome() },
                    onBookmarkClick = {
                        if (activeTab?.isStartPage == true) {
                            viewModel.setBookmarksHistoryDialogVisible(true, 0)
                        } else {
                            viewModel.toggleBookmarkCurrentPage()
                            Toast.makeText(context, "Bookmark updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBookmarksClick = { viewModel.setBookmarksHistoryDialogVisible(true, 0) },
                    onHistoryClick = { viewModel.setBookmarksHistoryDialogVisible(true, 1) },
                    onToggleDesktopSite = { viewModel.toggleDesktopSite() },
                    onClearDataClick = { showClearDataDialog = true },
                    onShareClick = omniboxCallbacks.onShareClick,
                    onSettingsClick = { viewModel.setSettingsDialogVisible(true) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Keep EVERY tab's WebView alive so the tab switcher can show live page
            // previews. Inactive tabs stay composed but are rendered invisible.
            tabs.forEach { tab ->
                val isVisible = tab.id == activeTabId
                if (tab.isStartPage) {
                    if (isVisible) {
                        ChromiumStartPage(
                            shieldConfig = shieldConfig,
                            onOpenShieldSettings = { viewModel.setShieldSheetVisible(true) }
                        )
                    }
                } else {
                    ShieldWebView(
                        tab = tab,
                        shieldConfig = shieldConfig,
                        webViewCommands = viewModel.webViewCommands,
                        onProgressChanged = { progress ->
                            viewModel.onTabProgressChanged(tab.id, progress)
                        },
                        onUrlAndTitleChanged = { url, title, canGoBack, canGoForward ->
                            viewModel.onTabUrlAndTitleChanged(
                                tab.id,
                                url,
                                title,
                                canGoBack,
                                canGoForward
                            )
                        },
                        onBlockedItem = { event ->
                            viewModel.recordBlockedEvent(tab.id, event)
                        },
                        onThumbnailCaptured = { tabId, thumbnail ->
                            viewModel.onTabThumbnailCaptured(tabId, thumbnail)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (isVisible) 1f else 0f)
                    )
                }
            }
        }
    }

    // Tab Grid Switcher Overlay
    AnimatedVisibility(
        visible = isTabSwitcherVisible,
        enter = fadeIn(animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )) + scaleIn(
            initialScale = 0.96f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = fadeOut(animationSpec = tween(150)) +
            scaleOut(targetScale = 0.96f, animationSpec = tween(150))
    ) {
        TabGridSwitcher(
            tabs = tabs,
            activeTabId = activeTabId,
            thumbnails = thumbnails,
            onSelectTab = { viewModel.selectTab(it) },
            onCloseTab = { viewModel.closeTab(it) },
            onNewTab = { viewModel.openNewTab() },
            onCloseAllTabs = { viewModel.closeAllTabs() },
            onDismiss = { viewModel.setTabSwitcherVisible(false) }
        )
    }

    // Privacy Shield Bottom Sheet
    if (isShieldSheetVisible) {
        PrivacyShieldSheet(
            sheetState = shieldSheetState,
            activeTab = activeTab,
            shieldConfig = shieldConfig,
            onConfigChange = { viewModel.updateShieldConfig(it) },
            onClearSiteData = {
                viewModel.clearBrowsingData(clearCache = true, clearCookies = true, clearHistory = false)
                Toast.makeText(context, "Site cookies & cache cleared", Toast.LENGTH_SHORT).show()
                viewModel.setShieldSheetVisible(false)
                viewModel.reload()
            },
            onDismiss = { viewModel.setShieldSheetVisible(false) }
        )
    }

    // Bookmarks and History Bottom Sheet
    if (isBookmarksHistoryDialogVisible) {
        BookmarksHistoryDialog(
            sheetState = bookmarksSheetState,
            selectedTab = selectedDialogTab,
            bookmarks = bookmarks,
            history = history,
            onTabSelected = { viewModel.setSelectedDialogTab(it) },
            onItemClick = { url ->
                viewModel.setBookmarksHistoryDialogVisible(false)
                viewModel.submitUrlOrQuery(url)
            },
            onDeleteBookmark = { viewModel.removeBookmark(it) },
            onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
            onClearHistory = {
                viewModel.clearAllHistory()
                Toast.makeText(context, "Browsing history cleared", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { viewModel.setBookmarksHistoryDialogVisible(false) }
        )
    }

    // Settings Screen (full page overlay)
    if (isSettingsDialogVisible) {
        SettingsScreen(
            toolbarAtBottom = toolbarAtBottom,
            darkTheme = darkTheme,
            selectedSearchEngineUrl = selectedSearchEngineUrl,
            customSearchEngines = customSearchEngines,
            onToolbarPositionChange = { viewModel.setToolbarAtBottom(it) },
            onDarkThemeChange = { viewModel.setDarkTheme(it) },
            onSearchEngineSelect = { viewModel.setSelectedSearchEngine(it) },
            onAddCustomSearchEngine = { name, url -> viewModel.addCustomSearchEngine(name, url) },
            onRemoveCustomSearchEngine = { viewModel.removeCustomSearchEngine(it) },
            vaultHasPassword = vaultHasPassword,
            vaultUnlocked = vaultUnlocked,
            vaultLogins = vaultLogins,
            vaultBusy = vaultBusy,
            vaultError = vaultError,
            vaultLastImportCount = vaultLastImportCount,
            biometricEnrolled = isBiometricEnrolled(context),
            biometricAllowed = biometricAllowed,
            onSetupVaultPassword = { password, enableBiometric -> viewModel.setupVaultPassword(password, enableBiometric) },
            onUnlockVault = { password -> viewModel.unlockVault(password) },
            onBiometricUnlock = { viewModel.unlockVaultWithBiometrics() },
            onSetBiometricAllowed = { allowed -> viewModel.setBiometricAllowed(allowed) },
            onLockVault = { viewModel.lockVault() },
            onAddVaultLogin = { site, username, password -> viewModel.addVaultLogin(site, username, password) },
            onDeleteVaultLogin = { id -> viewModel.deleteVaultLogin(id) },
            onImportVaultCsv = { content -> viewModel.importVaultCsv(content) },
            onClearVaultError = { viewModel.clearVaultError() },
            onClearVaultImportCount = { viewModel.clearVaultImportCount() },
            onBack = { viewModel.setSettingsDialogVisible(false) }
        )
    }

    // Clear Browsing Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear browsing data") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = clearHistorySelected,
                            onCheckedChange = { clearHistorySelected = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browsing history")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = clearCookiesSelected,
                            onCheckedChange = { clearCookiesSelected = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cookies and site data")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = clearCacheSelected,
                            onCheckedChange = { clearCacheSelected = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cached images and files")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearBrowsingData(
                            clearCache = clearCacheSelected,
                            clearCookies = clearCookiesSelected,
                            clearHistory = clearHistorySelected
                        )
                        showClearDataDialog = false
                        Toast.makeText(context, "Data cleared successfully", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_clear_data")
                ) {
                    Text("Clear data", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BrowsingToolbar(
    activeTab: BrowserTab?,
    openTabsCount: Int,
    inputText: String,
    isEditing: Boolean,
    shieldConfig: ShieldConfig,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    isBookmarked: Boolean,
    atTop: Boolean,
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
    onClearDataClick: () -> Unit,
    onShareClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(if (atTop) WindowInsets.statusBars else WindowInsets.navigationBars)
    ) {
        ChromiumOmnibox(
            activeTab = activeTab,
            openTabsCount = openTabsCount,
            inputText = inputText,
            isEditing = isEditing,
            shieldConfig = shieldConfig,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            isLoading = isLoading,
            isBookmarked = isBookmarked,
            onInputTextChange = onInputTextChange,
            onStartEditing = onStartEditing,
            onSubmitQuery = onSubmitQuery,
            onCancelEditing = onCancelEditing,
            onShieldClick = onShieldClick,
            onTabSwitcherClick = onTabSwitcherClick,
            onNewTabClick = onNewTabClick,
            onNewIncognitoTabClick = onNewIncognitoTabClick,
            onBackClick = onBackClick,
            onForwardClick = onForwardClick,
            onReloadOrStopClick = onReloadOrStopClick,
            onHomeClick = onHomeClick,
            onBookmarkClick = onBookmarkClick,
            onBookmarksClick = onBookmarksClick,
            onHistoryClick = onHistoryClick,
            onToggleDesktopSite = onToggleDesktopSite,
            onClearDataClick = onClearDataClick,
            onShareClick = onShareClick,
            onSettingsClick = onSettingsClick
        )

        PageProgressBar(
            isLoading = activeTab?.isLoading == true,
            progress = activeTab?.progress ?: 0
        )
    }
}
