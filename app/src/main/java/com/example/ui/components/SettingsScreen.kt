package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.VaultLogin
import com.example.model.DefaultSearchEngines
import com.example.model.SearchEngine

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
    onBack: () -> Unit
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
            onBack = onBack
        )

            SettingsDestination.Appearance -> AppearanceSettingsPage(
                toolbarAtBottom = toolbarAtBottom,
                darkTheme = darkTheme,
                onToolbarPositionChange = onToolbarPositionChange,
                onDarkThemeChange = onDarkThemeChange,
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
        }
    }
}

private sealed class SettingsDestination {
    data object Root : SettingsDestination()
    data object Appearance : SettingsDestination()
    data object SearchEngine : SettingsDestination()
    data object About : SettingsDestination()
    data object Passwords : SettingsDestination()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsRootPage(
    onOpenAppearance: () -> Unit,
    onOpenSearchEngine: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPasswords: () -> Unit,
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

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCategoryRow(
            icon = Icons.Default.Search,
            title = "Search Engine",
            subtitle = "Choose your default search engine",
            onClick = onOpenSearchEngine,
            testTag = "settings_open_search_engine"
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsCategoryRow(
            icon = Icons.Default.Lock,
            title = "Passwords",
            subtitle = "On-device password manager",
            onClick = onOpenPasswords,
            testTag = "settings_open_passwords"
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsPage(
    toolbarAtBottom: Boolean,
    darkTheme: Boolean,
    onToolbarPositionChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    SettingsScaffold(
        title = "Appearance",
        onBack = onBack,
        contentTag = "appearance_screen"
    ) {
        SettingsSectionHeader("Theme")
        SettingRadioRow(
            selected = !darkTheme,
            label = "Light theme",
            onClick = { onDarkThemeChange(false) },
            testTag = "theme_light"
        )
        SettingRadioRow(
            selected = darkTheme,
            label = "Dark theme",
            onClick = { onDarkThemeChange(true) },
            testTag = "theme_dark"
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionHeader("Toolbar position")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("toolbar_position_options"),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToolbarPositionOption(
                label = "Top",
                barAtTop = true,
                selected = !toolbarAtBottom,
                onClick = { onToolbarPositionChange(false) },
                modifier = Modifier.weight(1f),
                testTag = "toolbar_position_top"
            )
            ToolbarPositionOption(
                label = "Bottom",
                barAtTop = false,
                selected = toolbarAtBottom,
                onClick = { onToolbarPositionChange(true) },
                modifier = Modifier.weight(1f),
                testTag = "toolbar_position_bottom"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchEngineSettingsPage(
    selectedUrl: String,
    customSearchEngines: List<SearchEngine>,
    onSearchEngineSelect: (String) -> Unit,
    onAddCustomSearchEngine: (String, String) -> Unit,
    onRemoveCustomSearchEngine: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = "Search Engine",
        onBack = onBack,
        contentTag = "search_engine_screen"
    ) {
        SettingsSectionHeader("Built-in")
        DefaultSearchEngines.all.forEach { engine ->
            SettingRadioRow(
                selected = selectedUrl.equals(engine.urlTemplate, ignoreCase = true),
                label = engine.name,
                onClick = { onSearchEngineSelect(engine.urlTemplate) },
                testTag = "search_engine_${engine.name.lowercase().replace(" ", "_")}"
            )
        }

        if (customSearchEngines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSectionHeader("Custom")
            customSearchEngines.forEach { engine ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedUrl.equals(engine.urlTemplate, ignoreCase = true),
                                onClick = { onSearchEngineSelect(engine.urlTemplate) }
                            )
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUrl.equals(engine.urlTemplate, ignoreCase = true),
                            onClick = { onSearchEngineSelect(engine.urlTemplate) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = engine.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onRemoveCustomSearchEngine(engine.urlTemplate) }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove ${engine.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Custom engines use %s as the placeholder for your search query.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.testTag("search_engine_add")
        ) {
            Text("Add search engine")
        }
    }

    if (showAddDialog) {
        AddSearchEngineDialog(
            onConfirm = { name, url ->
                onAddCustomSearchEngine(name, url)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddSearchEngineDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add search engine") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = { Text("Search URL") },
                    placeholder = { Text("https://example.com/search?q=%s") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = if (error != null) {
                        { Text(error.orEmpty()) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedUrl = url.trim()
                    when {
                        trimmedName.isEmpty() -> error = "Enter a name."
                        !trimmedUrl.contains("%s") -> {
                            error = "Search URL must contain %s where the query is inserted."
                        }
                        else -> onConfirm(trimmedName, trimmedUrl)
                    }
                },
                modifier = Modifier.testTag("search_engine_add_confirm")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current

    SettingsScaffold(
        title = "About",
        onBack = onBack,
        contentTag = "about_screen"
    ) {
        SettingsSectionHeader("App")
        Text(
            text = "Micromium\nVersion 1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionHeader("Developer")

        SettingsLinkRow(
            icon = Icons.Default.Info,
            title = "GitHub",
            subtitle = "github.com/palashsuryavanshi",
            url = "https://github.com/palashsuryavanshi",
            onOpen = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) },
            testTag = "about_github"
        )

        SettingsLinkRow(
            icon = Icons.Default.Info,
            title = "LinkedIn",
            subtitle = "in.linkedin.com/in/palashsuryavanshi",
            url = "https://in.linkedin.com/in/palashsuryavanshi",
            onOpen = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) },
            testTag = "about_linkedin"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    contentTag: String,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(contentTag),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 10.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            content()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingsCategoryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    url: String,
    onOpen: (String) -> Unit,
    testTag: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(url) }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingRadioRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .testTag(testTag)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** A horizontal-selectable card showing a phone outline with the toolbar bar at top or bottom. */
@Composable
private fun ToolbarPositionOption(
    label: String,
    barAtTop: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (selected) 2.dp else 1.dp

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(96.dp)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                val bodyColor = MaterialTheme.colorScheme.surfaceVariant
                val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                if (barAtTop) {
                    // Toolbar bar (top) + body below
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp)
                                .background(barColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(bodyColor)
                        )
                    }
                } else {
                    // Body above + toolbar bar (bottom)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(bodyColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp)
                                .background(barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}