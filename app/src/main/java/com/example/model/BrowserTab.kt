package com.example.model

import com.example.privacy.BlockedEvent
import java.util.UUID

const val START_PAGE_URL = "shield://newtab"

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = START_PAGE_URL,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isDesktopSite: Boolean = false,
    val isIncognito: Boolean = false,
    val pageBlockedAds: Int = 0,
    val pageBlockedTrackers: Int = 0,
    val blockedEvents: List<BlockedEvent> = emptyList()
) {
    val isStartPage: Boolean
        get() = url.isBlank() || url == START_PAGE_URL || url == "about:blank"

    val totalBlockedOnPage: Int
        get() = pageBlockedAds + pageBlockedTrackers
}
