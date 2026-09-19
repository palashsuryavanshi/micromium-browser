package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A single preference question shown during first-run onboarding.
 *
 * Adding a future feature is a one-line change: append a new [OnboardingQuestion]
 * to [onboardingQuestions] using the same preference key the feature reads from
 * [com.example.data.BrowserRepository]. The wizard, persistence, and any future
 * settings screens will pick it up automatically.
 */
data class OnboardingQuestion(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val defaultValue: Boolean
)

/**
 * The full list of preferences asked during onboarding.
 * Keys must match the SharedPreferences keys used in BrowserRepository.
 */
val onboardingQuestions: List<OnboardingQuestion> = listOf(
    OnboardingQuestion(
        key = "shield_enabled",
        title = "Privacy Guard",
        description = "Enable all privacy protection features across your browsing.",
        icon = Icons.Default.Shield,
        defaultValue = true
    ),
    OnboardingQuestion(
        key = "block_ads",
        title = "Block ads",
        description = "Filter out intrusive advertisements so pages load faster and cleaner.",
        icon = Icons.Default.ClearAll,
        defaultValue = true
    ),
    OnboardingQuestion(
        key = "block_trackers",
        title = "Block trackers",
        description = "Stop advertising networks from tracking you across the web.",
        icon = Icons.Default.BusinessCenter,
        defaultValue = true
    ),
    OnboardingQuestion(
        key = "strip_tracking_params",
        title = "Strip tracking parameters",
        description = "Remove UTM and other tracking tokens from URLs before they load.",
        icon = Icons.Default.VisibilityOff,
        defaultValue = true
    ),
    OnboardingQuestion(
        key = "block_third_party_cookies",
        title = "Block third-party cookies",
        description = "Prevent sites from storing cross-site cookies on your device.",
        icon = Icons.Default.Cookie,
        defaultValue = true
    ),
    OnboardingQuestion(
        key = "cosmetic_filtering",
        title = "Hide ad placeholders",
        description = "Remove blank spaces and layout clutter left behind by blocked ads.",
        icon = Icons.Default.Bookmarks,
        defaultValue = true
    ),
    OnboardingQuestion(
        key = "force_https",
        title = "Force HTTPS",
        description = "Upgrade every connection to the secure, encrypted version of a site.",
        icon = Icons.Default.Https,
        defaultValue = true
    ),
    OnboardingQuestion(
        key = "toolbar_at_bottom",
        title = "Toolbar at bottom",
        description = "Place the address bar and controls at the bottom for easier one-handed use.",
        icon = Icons.Default.Security,
        defaultValue = false
    ),
    OnboardingQuestion(
        key = "dark_theme",
        title = "Dark theme",
        description = "Use a dark theme. You can switch between Light and Dark anytime in Settings.",
        icon = Icons.Default.DarkMode,
        defaultValue = true
    )
)