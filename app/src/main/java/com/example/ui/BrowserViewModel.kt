package com.example.ui

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookmarkEntity
import com.example.data.BrowserDatabase
import com.example.data.BrowserRepository
import com.example.data.HistoryEntity
import com.example.data.VaultLogin
import com.example.data.VaultRepository
import com.example.model.BrowserTab
import com.example.model.START_PAGE_URL
import com.example.model.SearchEngine
import com.example.privacy.BlockCategory
import com.example.privacy.BlockedEvent
import com.example.privacy.PrivacyEngine
import com.example.privacy.ShieldConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.ImageBitmap
import java.net.URLEncoder

sealed class WebViewCommand {
    data class LoadUrl(val tabId: String, val url: String) : WebViewCommand()
    data class GoBack(val tabId: String) : WebViewCommand()
    data class GoForward(val tabId: String) : WebViewCommand()
    data class Reload(val tabId: String) : WebViewCommand()
    data class Stop(val tabId: String) : WebViewCommand()
    data class CaptureThumbnail(val tabId: String) : WebViewCommand()
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository
    private val vaultRepository: VaultRepository

    init {
        val database = BrowserDatabase.getInstance(application)
        repository = BrowserRepository(database.browserDao(), application)
        vaultRepository = VaultRepository(application)
        viewModelScope.launch {
            vaultRepository.refreshMeta()
            _biometricAllowed.value = vaultRepository.isBiometricAllowed()
        }
    }

    // Local vault (device login + saved passwords) state
    val vaultUnlocked: StateFlow<Boolean> = vaultRepository.unlocked
    val vaultLogins: StateFlow<List<VaultLogin>> = vaultRepository.logins
    val hasVaultPassword: StateFlow<Boolean> = vaultRepository.hasPassword

    private val _vaultBusy = MutableStateFlow(false)
    val vaultBusy: StateFlow<Boolean> = _vaultBusy.asStateFlow()

    private val _vaultError = MutableStateFlow<String?>(null)
    val vaultError: StateFlow<String?> = _vaultError.asStateFlow()

    fun clearVaultError() {
        _vaultError.value = null
    }

    fun setupVaultPassword(password: String, enableBiometric: Boolean) {
        viewModelScope.launch {
            _vaultBusy.value = true
            _vaultError.value = vaultRepository.setupPassword(password)
                .exceptionOrNull()?.message
            if (_vaultError.value == null && enableBiometric) {
                vaultRepository.setBiometricAllowed(true)
                _biometricAllowed.value = true
            }
            _vaultBusy.value = false
        }
    }

    fun unlockVault(password: String) {
        viewModelScope.launch {
            _vaultBusy.value = true
            _vaultError.value = vaultRepository.unlock(password)
                .exceptionOrNull()?.message
            _vaultBusy.value = false
        }
    }

    fun unlockVaultWithBiometrics() {
        viewModelScope.launch {
            vaultRepository.unlockWithBiometrics()
        }
    }

    fun lockVault() {
        vaultRepository.lock()
        _vaultError.value = null
    }

    fun addVaultLogin(site: String, username: String, password: String) {
        viewModelScope.launch {
            _vaultError.value = vaultRepository.addLogin(site, username, password)
                .exceptionOrNull()?.message
        }
    }

    fun deleteVaultLogin(id: Long) {
        viewModelScope.launch {
            vaultRepository.deleteLogin(id)
        }
    }

    private val _vaultLastImportCount = MutableStateFlow<Int?>(null)
    val vaultLastImportCount: StateFlow<Int?> = _vaultLastImportCount.asStateFlow()

    private val _biometricAllowed = MutableStateFlow(false)
    val biometricAllowed: StateFlow<Boolean> = _biometricAllowed.asStateFlow()

    fun setBiometricAllowed(allowed: Boolean) {
        viewModelScope.launch {
            vaultRepository.setBiometricAllowed(allowed)
            _biometricAllowed.value = allowed
        }
    }

    fun importVaultCsv(content: String) {
        viewModelScope.launch {
            _vaultBusy.value = true
            val result = vaultRepository.importCsv(content)
            _vaultLastImportCount.value = result.getOrNull()
            _vaultError.value = result.exceptionOrNull()?.message
            _vaultBusy.value = false
        }
    }

    fun clearVaultImportCount() {
        _vaultLastImportCount.value = null
    }

    // Tabs state
    private val initialTab = BrowserTab()
    private val _tabs = MutableStateFlow<List<BrowserTab>>(listOf(initialTab))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<BrowserTab?> = combine(_tabs, _activeTabId) { tabList, id ->
        tabList.firstOrNull { it.id == id } ?: tabList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialTab)

    // Omnibox state
    private val _omniboxText = MutableStateFlow("")
    val omniboxText: StateFlow<String> = _omniboxText.asStateFlow()

    private val _isOmniboxEditing = MutableStateFlow(false)
    val isOmniboxEditing: StateFlow<Boolean> = _isOmniboxEditing.asStateFlow()

    // Sheet / Dialog visibility states
    private val _isTabSwitcherVisible = MutableStateFlow(false)
    val isTabSwitcherVisible: StateFlow<Boolean> = _isTabSwitcherVisible.asStateFlow()

    private val _isShieldSheetVisible = MutableStateFlow(false)
    val isShieldSheetVisible: StateFlow<Boolean> = _isShieldSheetVisible.asStateFlow()

    private val _isBookmarksHistoryDialogVisible = MutableStateFlow(false)
    val isBookmarksHistoryDialogVisible: StateFlow<Boolean> = _isBookmarksHistoryDialogVisible.asStateFlow()

    private val _selectedDialogTab = MutableStateFlow(0) // 0 = Bookmarks, 1 = History
    val selectedDialogTab: StateFlow<Int> = _selectedDialogTab.asStateFlow()

    private val _isSettingsDialogVisible = MutableStateFlow(false)
    val isSettingsDialogVisible: StateFlow<Boolean> = _isSettingsDialogVisible.asStateFlow()

    // Database & Repository flows
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shieldConfig: StateFlow<ShieldConfig> = repository.shieldConfig
    val totalAdsBlocked: StateFlow<Int> = repository.totalAdsBlocked
    val totalTrackersBlocked: StateFlow<Int> = repository.totalTrackersBlocked
    val toolbarAtBottom: StateFlow<Boolean> = repository.toolbarAtBottom
    val onboardingComplete: StateFlow<Boolean> = repository.onboardingComplete
    val darkTheme: StateFlow<Boolean> = repository.darkTheme
    val selectedSearchEngineUrl: StateFlow<String> = repository.selectedSearchEngineUrl
    val customSearchEngines: StateFlow<List<SearchEngine>> = repository.customSearchEngines

    // Live page previews for the tab switcher (keyed by tab id)
    private val _thumbnails = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, ImageBitmap>> = _thumbnails.asStateFlow()

    fun onTabThumbnailCaptured(tabId: String, bitmap: ImageBitmap) {
        _thumbnails.value = _thumbnails.value + (tabId to bitmap)
    }

    // Commands to send to the active WebView
    private val _webViewCommands = MutableSharedFlow<WebViewCommand>(extraBufferCapacity = 10)
    val webViewCommands: SharedFlow<WebViewCommand> = _webViewCommands.asSharedFlow()

    // Omnibox text management
    fun setOmniboxText(text: String) {
        _omniboxText.value = text
    }

    fun setOmniboxEditing(editing: Boolean) {
        _isOmniboxEditing.value = editing
        if (editing) {
            val currentUrl = activeTab.value?.url ?: ""
            _omniboxText.value = if (currentUrl == START_PAGE_URL || currentUrl == "about:blank") "" else currentUrl
        }
    }

    // Navigation and URL submission
    fun submitUrlOrQuery(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        val finalUrl = resolveInputToUrl(trimmed)
        val tabId = _activeTabId.value

        _isOmniboxEditing.value = false
        _omniboxText.value = finalUrl

        // Update tab model
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    url = finalUrl,
                    title = finalUrl,
                    isLoading = true,
                    progress = 10,
                    pageBlockedAds = 0,
                    pageBlockedTrackers = 0,
                    blockedEvents = emptyList()
                )
            } else tab
        }

        viewModelScope.launch {
            _webViewCommands.emit(WebViewCommand.LoadUrl(tabId, finalUrl))
        }
    }

    private fun resolveInputToUrl(input: String): String {
        val sanitized = PrivacyEngine.sanitizeUrl(input, shieldConfig.value.stripTrackingParams)
        if (sanitized.startsWith("http://") || sanitized.startsWith("https://")) {
            return sanitized
        }
        if (sanitized.startsWith("shield://") || sanitized.startsWith("about:")) {
            return sanitized
        }
        // Check if it's a domain name (e.g. google.com, wikipedia.org, github.com)
        val hasDomainStructure = sanitized.contains(".") && !sanitized.contains(" ")
        if (hasDomainStructure) {
            return "https://$sanitized"
        }
        // Fall back to the configured search engine
        return try {
            val query = URLEncoder.encode(sanitized, "UTF-8")
            selectedSearchEngineUrl.value.replace("%s", query)
        } catch (e: Exception) {
            "https://duckduckgo.com/?q=$sanitized"
        }
    }

    // Tab Operations
    fun openNewTab(url: String = START_PAGE_URL, isIncognito: Boolean = false) {
        val newTab = BrowserTab(
            url = url,
            title = if (url == START_PAGE_URL) "New Tab" else url,
            isIncognito = isIncognito
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        _isTabSwitcherVisible.value = false
        _isOmniboxEditing.value = false
        _omniboxText.value = if (url == START_PAGE_URL) "" else url

        if (url != START_PAGE_URL) {
            viewModelScope.launch {
                _webViewCommands.emit(WebViewCommand.LoadUrl(newTab.id, url))
            }
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        _isTabSwitcherVisible.value = false
        val currentTab = _tabs.value.firstOrNull { it.id == tabId }
        _omniboxText.value = if (currentTab?.isStartPage == true) "" else (currentTab?.url ?: "")
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val index = currentTabs.indexOfFirst { it.id == tabId }
        if (index == -1) return

        val updatedTabs = currentTabs.filter { it.id != tabId }
        if (updatedTabs.isEmpty()) {
            val freshTab = BrowserTab()
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
            _omniboxText.value = ""
        } else {
            _tabs.value = updatedTabs
            if (_activeTabId.value == tabId) {
                val newActiveIndex = (index - 1).coerceAtLeast(0).coerceAtMost(updatedTabs.size - 1)
                _activeTabId.value = updatedTabs[newActiveIndex].id
                _omniboxText.value = if (updatedTabs[newActiveIndex].isStartPage) "" else updatedTabs[newActiveIndex].url
            }
        }
    }

    fun closeAllTabs() {
        val freshTab = BrowserTab()
        _tabs.value = listOf(freshTab)
        _activeTabId.value = freshTab.id
        _omniboxText.value = ""
        _isTabSwitcherVisible.value = false
    }

    // WebView Callbacks
    fun onTabProgressChanged(tabId: String, progress: Int) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    progress = progress,
                    isLoading = progress in 1..99
                )
            } else tab
        }
    }

    fun onTabUrlAndTitleChanged(
        tabId: String,
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    url = url,
                    title = if (title.isNotBlank()) title else (if (url == START_PAGE_URL) "New Tab" else url),
                    canGoBack = canGoBack,
                    canGoForward = canGoForward
                )
            } else tab
        }

        if (_activeTabId.value == tabId && !_isOmniboxEditing.value) {
            _omniboxText.value = if (url == START_PAGE_URL || url == "about:blank") "" else url
        }

        // Record history for regular (non-incognito) tabs
        val currentTab = _tabs.value.firstOrNull { it.id == tabId }
        if (currentTab != null && !currentTab.isIncognito && !currentTab.isStartPage) {
            viewModelScope.launch {
                repository.addHistory(title = title.ifBlank { url }, url = url)
            }
        }
    }

    fun recordBlockedEvent(tabId: String, event: BlockedEvent) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                val isAd = event.category == BlockCategory.AD
                tab.copy(
                    pageBlockedAds = if (isAd) tab.pageBlockedAds + 1 else tab.pageBlockedAds,
                    pageBlockedTrackers = if (!isAd) tab.pageBlockedTrackers + 1 else tab.pageBlockedTrackers,
                    blockedEvents = listOf(event) + tab.blockedEvents.take(49)
                )
            } else tab
        }

        // Increment global persisted counter
        repository.incrementBlockedCounts(
            adsCount = if (event.category == BlockCategory.AD) 1 else 0,
            trackersCount = if (event.category != BlockCategory.AD) 1 else 0
        )
    }

    // Navigation Controls
    fun goBack() {
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _webViewCommands.emit(WebViewCommand.GoBack(tabId))
        }
    }

    fun goForward() {
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _webViewCommands.emit(WebViewCommand.GoForward(tabId))
        }
    }

    fun reload() {
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _webViewCommands.emit(WebViewCommand.Reload(tabId))
        }
    }

    fun stopLoading() {
        val tabId = _activeTabId.value
        viewModelScope.launch {
            _webViewCommands.emit(WebViewCommand.Stop(tabId))
        }
    }

    fun goHome() {
        val tabId = _activeTabId.value
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    url = START_PAGE_URL,
                    title = "New Tab",
                    isLoading = false,
                    progress = 0
                )
            } else tab
        }
        _omniboxText.value = ""
        _isOmniboxEditing.value = false
    }

    fun toggleDesktopSite() {
        val tabId = _activeTabId.value
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(isDesktopSite = !tab.isDesktopSite)
            } else tab
        }
        reload()
    }

    // Shield Controls
    fun updateShieldConfig(newConfig: ShieldConfig) {
        repository.updateShieldConfig(newConfig)
    }

    fun toggleMasterShield() {
        val current = shieldConfig.value
        updateShieldConfig(current.copy(isShieldEnabled = !current.isShieldEnabled))
    }

    // Bookmarks and History
    fun toggleBookmarkCurrentPage() {
        val current = activeTab.value ?: return
        if (current.isStartPage) return

        viewModelScope.launch {
            repository.addBookmark(title = current.title, url = current.url)
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmark(id)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearBrowsingData(clearCache: Boolean, clearCookies: Boolean, clearHistory: Boolean) {
        viewModelScope.launch {
            if (clearHistory) {
                repository.clearHistory()
            }
            if (clearCookies) {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
            if (clearCache) {
                WebStorage.getInstance().deleteAllData()
            }
        }
    }

    // UI Dialog & Switcher Toggles
    fun setTabSwitcherVisible(visible: Boolean) {
        _isTabSwitcherVisible.value = visible
        if (visible) {
            captureAllThumbnails()
        }
    }

    // Capture the current on-screen preview of every tab's WebView
    fun captureAllThumbnails() {
        val tabList = _tabs.value
        viewModelScope.launch {
            tabList.forEach { tab ->
                _webViewCommands.emit(WebViewCommand.CaptureThumbnail(tab.id))
            }
        }
    }

    fun setShieldSheetVisible(visible: Boolean) {
        _isShieldSheetVisible.value = visible
    }

    fun setBookmarksHistoryDialogVisible(visible: Boolean, initialTab: Int = 0) {
        _selectedDialogTab.value = initialTab
        _isBookmarksHistoryDialogVisible.value = visible
    }

    fun setSelectedDialogTab(index: Int) {
        _selectedDialogTab.value = index
    }

    // Settings
    fun setSettingsDialogVisible(visible: Boolean) {
        _isSettingsDialogVisible.value = visible
    }

    fun setToolbarAtBottom(atBottom: Boolean) {
        repository.setToolbarAtBottom(atBottom)
    }

    // Onboarding
    fun finishOnboarding(answers: Map<String, Boolean>) {
        repository.applyOnboardingAnswers(answers)
        repository.completeOnboarding()
    }

    fun skipOnboarding() {
        repository.completeOnboarding()
    }

    // Theme
    fun setDarkTheme(dark: Boolean) {
        repository.setDarkTheme(dark)
    }

    // Search engine
    fun setSelectedSearchEngine(urlTemplate: String) {
        repository.setSelectedSearchEngine(urlTemplate)
    }

    fun addCustomSearchEngine(name: String, urlTemplate: String) {
        repository.addCustomSearchEngine(name.trim(), urlTemplate.trim())
    }

    fun removeCustomSearchEngine(urlTemplate: String) {
        repository.removeCustomSearchEngine(urlTemplate)
    }

    fun searchEngineNameFor(urlTemplate: String): String =
        repository.searchEngineNameFor(urlTemplate)
}
