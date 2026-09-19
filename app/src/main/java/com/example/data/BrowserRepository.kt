package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DefaultSearchEngines
import com.example.model.SearchEngine
import com.example.privacy.ShieldConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BrowserRepository(
    private val browserDao: BrowserDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shield_browser_prefs", Context.MODE_PRIVATE)

    // Bookmarks
    val bookmarks: Flow<List<BookmarkEntity>> = browserDao.getAllBookmarks()

    suspend fun addBookmark(title: String, url: String) = withContext(Dispatchers.IO) {
        browserDao.insertBookmark(BookmarkEntity(title = title, url = url))
    }

    suspend fun removeBookmark(id: Long) = withContext(Dispatchers.IO) {
        browserDao.deleteBookmarkById(id)
    }

    fun isBookmarked(url: String): Flow<Boolean> = browserDao.isBookmarked(url)

    // History
    val history: Flow<List<HistoryEntity>> = browserDao.getHistory()

    suspend fun addHistory(title: String, url: String) = withContext(Dispatchers.IO) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            browserDao.insertHistory(HistoryEntity(title = title.ifBlank { url }, url = url))
        }
    }

    suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        browserDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        browserDao.clearAllHistory()
    }

    // Shield configuration state
    private val _shieldConfig = MutableStateFlow(loadShieldConfig())
    val shieldConfig: StateFlow<ShieldConfig> = _shieldConfig.asStateFlow()

    // Global all-time blocked statistics
    private val _totalAdsBlocked = MutableStateFlow(prefs.getInt("total_ads_blocked", 142))
    val totalAdsBlocked: StateFlow<Int> = _totalAdsBlocked.asStateFlow()

    private val _totalTrackersBlocked = MutableStateFlow(prefs.getInt("total_trackers_blocked", 89))
    val totalTrackersBlocked: StateFlow<Int> = _totalTrackersBlocked.asStateFlow()

    // Toolbar position (true = bottom, false = top)
    private val _toolbarAtBottom = MutableStateFlow(prefs.getBoolean("toolbar_at_bottom", false))
    val toolbarAtBottom: StateFlow<Boolean> = _toolbarAtBottom.asStateFlow()

    fun setToolbarAtBottom(atBottom: Boolean) {
        _toolbarAtBottom.value = atBottom
        prefs.edit().putBoolean("toolbar_at_bottom", atBottom).apply()
    }

    // First-run onboarding completion
    private val _onboardingComplete = MutableStateFlow(prefs.getBoolean("setup_complete", false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    // Theme (true = dark, false = light). Never follows the system theme.
    private val _darkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    fun setDarkTheme(dark: Boolean) {
        _darkTheme.value = dark
        prefs.edit().putBoolean("dark_theme", dark).apply()
    }

    /** Persists every onboarding answer keyed by its preference name. */
    fun applyOnboardingAnswers(answers: Map<String, Boolean>) {
        prefs.edit().apply {
            answers.forEach { (key, value) -> putBoolean(key, value) }
        }.apply()
        _shieldConfig.value = loadShieldConfig()
        _toolbarAtBottom.value = prefs.getBoolean("toolbar_at_bottom", false)
        _darkTheme.value = prefs.getBoolean("dark_theme", false)
    }

    fun completeOnboarding() {
        _onboardingComplete.value = true
        prefs.edit().putBoolean("setup_complete", true).apply()
    }

    private fun loadShieldConfig(): ShieldConfig {
        return ShieldConfig(
            isShieldEnabled = prefs.getBoolean("shield_enabled", true),
            blockAds = prefs.getBoolean("block_ads", true),
            blockTrackers = prefs.getBoolean("block_trackers", true),
            stripTrackingParams = prefs.getBoolean("strip_tracking_params", true),
            blockThirdPartyCookies = prefs.getBoolean("block_third_party_cookies", true),
            cosmeticFiltering = prefs.getBoolean("cosmetic_filtering", true),
            forceHttps = prefs.getBoolean("force_https", true)
        )
    }

    fun updateShieldConfig(newConfig: ShieldConfig) {
        _shieldConfig.value = newConfig
        prefs.edit()
            .putBoolean("shield_enabled", newConfig.isShieldEnabled)
            .putBoolean("block_ads", newConfig.blockAds)
            .putBoolean("block_trackers", newConfig.blockTrackers)
            .putBoolean("strip_tracking_params", newConfig.stripTrackingParams)
            .putBoolean("block_third_party_cookies", newConfig.blockThirdPartyCookies)
            .putBoolean("cosmetic_filtering", newConfig.cosmeticFiltering)
            .putBoolean("force_https", newConfig.forceHttps)
            .apply()
    }

    fun incrementBlockedCounts(adsCount: Int = 0, trackersCount: Int = 0) {
        if (adsCount > 0) {
            val newAds = _totalAdsBlocked.value + adsCount
            _totalAdsBlocked.value = newAds
            prefs.edit().putInt("total_ads_blocked", newAds).apply()
        }
        if (trackersCount > 0) {
            val newTrackers = _totalTrackersBlocked.value + trackersCount
            _totalTrackersBlocked.value = newTrackers
            prefs.edit().putInt("total_trackers_blocked", newTrackers).apply()
        }
    }

    fun resetBlockedCounters() {
        _totalAdsBlocked.value = 0
        _totalTrackersBlocked.value = 0
        prefs.edit().putInt("total_ads_blocked", 0).putInt("total_trackers_blocked", 0).apply()
    }

    // Search engine selection
    private val _selectedSearchEngineUrl = MutableStateFlow(
        prefs.getString("search_engine_url", DefaultSearchEngines.DUCKDUCKGO.urlTemplate)!!
    )
    val selectedSearchEngineUrl: StateFlow<String> = _selectedSearchEngineUrl.asStateFlow()

    fun setSelectedSearchEngine(urlTemplate: String) {
        _selectedSearchEngineUrl.value = urlTemplate
        prefs.edit().putString("search_engine_url", urlTemplate).apply()
    }

    // Custom search engines (persisted as a JSON array)
    private val _customSearchEngines = MutableStateFlow(loadCustomSearchEngines())
    val customSearchEngines: StateFlow<List<SearchEngine>> = _customSearchEngines.asStateFlow()

    fun addCustomSearchEngine(name: String, urlTemplate: String) {
        val updated = _customSearchEngines.value + SearchEngine(
            name = name,
            urlTemplate = urlTemplate,
            isCustom = true
        )
        _customSearchEngines.value = updated
        saveCustomSearchEngines(updated)
    }

    fun removeCustomSearchEngine(urlTemplate: String) {
        val updated = _customSearchEngines.value.filterNot { it.urlTemplate == urlTemplate }
        _customSearchEngines.value = updated
        saveCustomSearchEngines(updated)
    }

    /** Resolve a search engine name (either a built-in or a saved custom engine). */
    fun searchEngineNameFor(urlTemplate: String): String {
        val custom = _customSearchEngines.value.firstOrNull { it.urlTemplate.equals(urlTemplate, ignoreCase = true) }
        custom?.let { return it.name }
        return DefaultSearchEngines.all.firstOrNull {
            it.urlTemplate.equals(urlTemplate, ignoreCase = true)
        }?.name ?: "Custom"
    }

    private fun loadCustomSearchEngines(): List<SearchEngine> {
        val raw = prefs.getString("custom_search_engines", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        SearchEngine(
                            name = obj.getString("name"),
                            urlTemplate = obj.getString("url"),
                            isCustom = true
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun saveCustomSearchEngines(engines: List<SearchEngine>) {
        val array = JSONArray()
        engines.forEach { engine ->
            array.put(JSONObject().put("name", engine.name).put("url", engine.urlTemplate))
        }
        prefs.edit().putString("custom_search_engines", array.toString()).apply()
    }
}
