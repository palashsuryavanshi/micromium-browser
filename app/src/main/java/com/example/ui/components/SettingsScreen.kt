package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.VaultLogin
import com.example.model.DefaultSearchEngines
import com.example.model.SearchEngine
import com.example.privacy.ShieldConfig
import com.example.ui.components.settings.AboutSettingsPage
import com.example.ui.components.settings.AppearanceSettingsPage
import com.example.ui.components.settings.BookmarksManagerScreen
import com.example.ui.components.settings.BrowserFeaturesPage
import com.example.ui.components.settings.DownloadsScreen
import com.example.ui.components.settings.HistoryScreen
import com.example.ui.components.settings.SearchEngineSettingsPage
import com.example.ui.components.settings.SettingsCategoryRow
import com.example.ui.components.settings.SettingsScaffold
import com.example.ui.components.settings.SettingsSectionHeader

/**
 * Full-page settings. The root screen is a list of category rows; tapping one
 * opens a dedicated sub-page, so each new setting group gets its own screen:
 *  - Settings -> Appearance (theme + toolbar)
 *  - Settings -> About (app info + social links)
 *
 * New features should follow this pattern: add a row here and a new sub-page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    toolbarAtBottom: Boolean,
    darkTheme: Boolean,
    selectedSearchEngineUrl: String,
    customSearchEngines: List<SearchEngine>,
    onToolbarPositionChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    textZoom: Int,
    onTextZoomChange: (Int) -> Unit,
    shieldConfig: ShieldConfig,
    onShieldConfigChange: (ShieldConfig) -> Unit,
    onOpenDownloads: () -> Unit,
    onSearchEngineSelect: (String) -> Unit,
    onAddCustomSearchEngine: (String, String) -> Unit,
    onRemoveCustomSearchEngine: (String) -> Unit,
    vaultHasPassword: Boolean,
    vaultUnlocked: Boolean,
    vaultLogins: List<VaultLogin>,
    vaultBusy: Boolean,
    vaultError: String?,
    vaultLastImportCount: Int?,
    biometricEnrolled: Boolean,
    biometricAllowed: Boolean,
    onSetupVaultPassword: (password: String, enableBiometric: Boolean) -> Unit,
    onUnlockVault: (password: String) -> Unit,
    onBiometricUnlock: () -> Unit,
    onSetBiometricAllowed: (allowed: Boolean) -> Unit,
    onLockVault: () -> Unit,
    onAddVaultLogin: (site: String, username: String, password: String) -> Unit,
    onDeleteVaultLogin: (id: Long) -> Unit,
    onImportVaultCsv: (content: String) -> Unit,
    onClearVaultError: () -> Unit,
    onClearVaultImportCount: () -> Unit,
    onBack: () -> Unit,
    viewModel: com.example.ui.BrowserViewModel,
    onOpenUrl: (String) -> Unit
) {
    var destination by remember { mutableStateOf<SettingsDestination>(SettingsDestination.Root) }

    BackHandler(enabled = destination != SettingsDestination.Root) {
        destination = SettingsDestination.Root
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            // Drilling in slides from the right, going back slides from the left.
            // Spring offset for fluid natural motion, quick fade to hide the swap.
            val forward = targetState != SettingsDestination.Root
            val offsetSpec = spring<IntOffset>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
            (slideInHorizontally(
                animationSpec = offsetSpec,
                initialOffsetX = { fullWidth -> if (forward) fullWidth else -fullWidth }
            ) + fadeIn(animationSpec = tween(200))).togetherWith(
                slideOutHorizontally(
                    animationSpec = offsetSpec,
                    targetOffsetX = { fullWidth -> if (forward) -fullWidth else fullWidth }
                ) + fadeOut(animationSpec = tween(200))
            )
        },
        label = "settings_navigation"
    ) { target ->
        when (target) {
            SettingsDestination.Root -> SettingsRootPage(
                onOpenAppearance = { destination = SettingsDestination.Appearance },
                onOpenSearchEngine = { destination = SettingsDestination.SearchEngine },
                onOpenAbout = { destination = SettingsDestination.About },
                onOpenPasswords = { destination = SettingsDestination.Passwords },
                onOpenBookmarks = { destination = SettingsDestination.Bookmarks },
                onOpenHistory = { destination = SettingsDestination.History },
                onOpenBrowser = { destination = SettingsDestination.Browser },
                onBack = onBack
            )

            SettingsDestination.Appearance -> AppearanceSettingsPage(
                toolbarAtBottom = toolbarAtBottom,
                darkTheme = darkTheme,
                textZoom = textZoom,
                onToolbarPositionChange = onToolbarPositionChange,
                onDarkThemeChange = onDarkThemeChange,
                onTextZoomChange = onTextZoomChange,
                onBack = { destination = SettingsDestination.Root }
            )

            SettingsDestination.SearchEngine -> SearchEngineSettingsPage(
                selectedUrl = selectedSearchEngineUrl,
                customSearchEngines = customSearchEngines,
                onSearchEngineSelect = onSearchEngineSelect,
                onAddCustomSearchEngine = onAddCustomSearchEngine,
                onRemoveCustomSearchEngine = onRemoveCustomSearchEngine,
                onBack = { destination = SettingsDestination.Root }
            )

            SettingsDestination.About -> AboutSettingsPage(
                onBack = { destination = SettingsDestination.Root }
            )

            SettingsDestination.Passwords -> PasswordsScreen(
                hasPassword = vaultHasPassword,
                unlocked = vaultUnlocked,
                logins = vaultLogins,
                busy = vaultBusy,
                error = vaultError,
                lastImportCount = vaultLastImportCount,
                biometricEnrolled = biometricEnrolled,
                biometricAllowed = biometricAllowed,
                onSetupPassword = onSetupVaultPassword,
                onUnlock = onUnlockVault,
                onBiometricUnlock = onBiometricUnlock,
                onSetBiometricAllowed = onSetBiometricAllowed,
                onLock = onLockVault,
                onAdd = onAddVaultLogin,
                onDelete = onDeleteVaultLogin,
                onImportCsv = onImportVaultCsv,
                onClearError = onClearVaultError,
                onClearImportCount = onClearVaultImportCount,
                onBack = { destination = SettingsDestination.Root }
            )

            SettingsDestination.Bookmarks -> BookmarksManagerScreen(
                viewModel = viewModel,
                onOpenUrl = onOpenUrl,
                onBack = { destination = SettingsDestination.Root }
            )

            SettingsDestination.History -> HistoryScreen(
                viewModel = viewModel,
                onOpenUrl = onOpenUrl,
                onBack = { destination = SettingsDestination.Root }
            )

            SettingsDestination.Browser -> BrowserFeaturesPage(
                shieldConfig = shieldConfig,
                onShieldConfigChange = onShieldConfigChange,
                onOpenDownloads = onOpenDownloads,
                onOpenDownloadsScreen = { destination = SettingsDestination.Downloads },
                onBack = { destination = SettingsDestination.Root }
            )

            SettingsDestination.Downloads -> DownloadsScreen(
                onOpenSystemDownloads = onOpenDownloads,
                onBack = { destination = SettingsDestination.Browser }
            )
        }
    }
}

private sealed class SettingsDestination {
    data object Root : SettingsDestination()
    data object Appearance : SettingsDestination()
    data object SearchEngine : SettingsDestination()
    data object About : SettingsDestination()
    data object Passwords : SettingsDestination()
    data object Bookmarks : SettingsDestination()
    data object History : SettingsDestination()
    data object Downloads : SettingsDestination()
    data object Browser : SettingsDestination()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsRootPage(
    onOpenAppearance: () -> Unit,
    onOpenSearchEngine: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPasswords: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBrowser: () -> Unit,
    onBack: () -> Unit
) {
    SettingsScaffold(
        title = "Settings",
        onBack = onBack,
        contentTag = "settings_screen"
    ) {
        SettingsSectionHeader("Preferences")

        SettingsCategoryRow(
            icon = Icons.Default.Palette,
            title = "Appearance",
            subtitle = "Theme and toolbar position",
            onClick = onOpenAppearance,
            testTag = "settings_open_appearance"
        )

        SettingsCategoryRow(
            icon = Icons.Default.Search,
            title = "Search Engine",
            subtitle = "Choose your default search engine",
            onClick = onOpenSearchEngine,
            testTag = "settings_open_search_engine"
        )

        SettingsCategoryRow(
            icon = Icons.Default.Lock,
            title = "Passwords",
            subtitle = "On-device password manager",
            onClick = onOpenPasswords,
            testTag = "settings_open_passwords"
        )

        SettingsCategoryRow(
            icon = Icons.Filled.Bookmark,
            title = "Bookmarks",
            subtitle = "Manage bookmarks and folders",
            onClick = onOpenBookmarks,
            testTag = "settings_open_bookmarks"
        )

        SettingsCategoryRow(
            icon = Icons.Filled.History,
            title = "History",
            subtitle = "View and manage browsing history",
            onClick = onOpenHistory,
            testTag = "settings_open_history"
        )

        SettingsCategoryRow(
            icon = Icons.Default.Public,
            title = "Browser",
            subtitle = "HTTPS-First, Safe Browsing, downloads",
            onClick = onOpenBrowser,
            testTag = "settings_open_browser"
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionHeader("Support")

        SettingsCategoryRow(
            icon = Icons.Default.Info,
            title = "About",
            subtitle = "About Micromium and the developer",
            onClick = onOpenAbout,
            testTag = "settings_open_about"
        )
    }
}
