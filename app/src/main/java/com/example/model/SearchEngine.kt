package com.example.model

/**
 * A search engine used for omnibox queries.
 * [urlTemplate] must contain a `%s` placeholder where the encoded query is inserted,
 * e.g. "https://duckduckgo.com/?q=%s".
 */
data class SearchEngine(
    val name: String,
    val urlTemplate: String,
    val isCustom: Boolean = false
)

object DefaultSearchEngines {
    val DUCKDUCKGO = SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q=%s")
    val GOOGLE = SearchEngine("Google", "https://www.google.com/search?q=%s")
    val BRAVE = SearchEngine("Brave Search", "https://search.brave.com/search?q=%s")

    val all: List<SearchEngine> = listOf(DUCKDUCKGO, GOOGLE, BRAVE)
}