package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.BrowserTab
import com.example.privacy.ShieldConfig

@Composable
fun BrowsingToolbar(
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
    onToggleReaderMode: () -> Unit,
    onFindInPageClick: (tabId: String) -> Unit,
    onPrintClick: (tabId: String) -> Unit,
    recentlyClosedCount: Int,
    onReopenClosedTab: () -> Unit,
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
            onToggleReaderMode = onToggleReaderMode,
            onFindInPageClick = onFindInPageClick,
            onPrintClick = onPrintClick,
            recentlyClosedCount = recentlyClosedCount,
            onReopenClosedTab = onReopenClosedTab,
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
