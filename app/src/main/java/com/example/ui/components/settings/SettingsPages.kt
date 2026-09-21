package com.example.ui.components.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.DefaultSearchEngines
import com.example.model.SearchEngine
import com.example.privacy.ShieldConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceSettingsPage(
    toolbarAtBottom: Boolean,
    darkTheme: Boolean,
    textZoom: Int,
    onToolbarPositionChange: (Boolean) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onTextZoomChange: (Int) -> Unit,
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

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionHeader("Text size")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onTextZoomChange(textZoom - 10) },
                enabled = textZoom > 50,
                modifier = Modifier.testTag("text_zoom_decrease")
            ) {
                Text("A-", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "$textZoom%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("text_zoom_value")
            )
            OutlinedButton(
                onClick = { onTextZoomChange(textZoom + 10) },
                enabled = textZoom < 200,
                modifier = Modifier.testTag("text_zoom_increase")
            ) {
                Text("A+", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
internal fun BrowserFeaturesPage(
    shieldConfig: ShieldConfig,
    onShieldConfigChange: (ShieldConfig) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenDownloadsScreen: () -> Unit,
    onBack: () -> Unit
) {
    SettingsScaffold(
        title = "Browser",
        onBack = onBack,
        contentTag = "browser_screen"
    ) {
        SettingsSectionHeader("Privacy & security")

        BrowserSwitchRow(
            title = "HTTPS-First Mode",
            subtitle = "Automatically upgrade http:// pages to https://",
            checked = shieldConfig.forceHttps,
            enabled = shieldConfig.isShieldEnabled,
            onCheckedChange = { onShieldConfigChange(shieldConfig.copy(forceHttps = it)) },
            testTag = "settings_https_first"
        )

        Spacer(modifier = Modifier.height(12.dp))

        BrowserSwitchRow(
            title = "Safe Browsing",
            subtitle = "Warn about deceptive and malicious sites",
            checked = shieldConfig.safeBrowsingEnabled,
            enabled = shieldConfig.isShieldEnabled,
            onCheckedChange = { onShieldConfigChange(shieldConfig.copy(safeBrowsingEnabled = it)) },
            testTag = "settings_safe_browsing"
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSectionHeader("Downloads")

        SettingsCategoryRow(
            icon = Icons.Default.Download,
            title = "Downloads",
            subtitle = "View and open downloaded files",
            onClick = onOpenDownloadsScreen,
            testTag = "settings_open_downloads"
        )
    }
}

@Composable
internal fun BrowserSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchEngineSettingsPage(
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
internal fun AddSearchEngineDialog(
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
internal fun AboutSettingsPage(onBack: () -> Unit) {
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
