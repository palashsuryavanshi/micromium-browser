package com.example.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BookmarkEntity
import com.example.ui.BrowserViewModel

/** Which add/edit dialog is open, if any. */
private sealed interface BookmarkDialog {
    data class AddBookmark(val parentId: Long?) : BookmarkDialog
    data class AddFolder(val parentId: Long?) : BookmarkDialog
    data class Edit(val item: BookmarkEntity) : BookmarkDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarksManagerScreen(
    viewModel: BrowserViewModel,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit
) {
    var path by remember { mutableStateOf<List<BookmarkEntity>>(emptyList()) }
    var dialog by remember { mutableStateOf<BookmarkDialog?>(null) }
    var pendingDelete by remember { mutableStateOf<BookmarkEntity?>(null) }

    val currentFolderId: Long? = path.lastOrNull()?.id
    val items: List<BookmarkEntity> = if (currentFolderId == null) {
        viewModel.rootBookmarks.collectAsStateWithLifecycle().value
    } else {
        remember(currentFolderId) {
            viewModel.bookmarksInFolder(currentFolderId)
        }.collectAsStateWithLifecycle().value
    }

    SettingsScaffold(
        title = path.lastOrNull()?.title ?: "Bookmarks",
        onBack = {
            if (path.isNotEmpty()) {
                path = path.dropLast(1)
            } else {
                onBack()
            }
        },
        contentTag = "bookmarks_screen"
    ) {
        // Breadcrumb: Root / Folder / Subfolder
        if (path.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Root",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { path = emptyList() }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                )
                path.forEachIndexed { index, folder ->
                    Text(
                        text = " / ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = folder.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index == path.lastIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = if (index == path.lastIndex) {
                            Modifier.padding(vertical = 8.dp)
                        } else {
                            Modifier
                                .clickable { path = path.take(index + 1) }
                                .padding(vertical = 8.dp)
                        }
                    )
                }
            }
        }

        // Add buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { dialog = BookmarkDialog.AddBookmark(currentFolderId) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bookmark")
            }
            Button(
                onClick = { dialog = BookmarkDialog.AddFolder(currentFolderId) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Folder")
            }
        }

        // Items
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (currentFolderId == null) {
                            Icons.Default.Bookmark
                        } else {
                            Icons.Default.FolderOpen
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (currentFolderId == null) "No bookmarks yet" else "This folder is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use the buttons above to add some",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Items (the settings scaffold already scrolls - no nested scroll here)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    BookmarkRow(
                        item = item,
                        onClick = {
                            if (item.isFolder) {
                                path = path + item
                            } else {
                                onOpenUrl(item.url)
                            }
                        },
                        onEdit = { dialog = BookmarkDialog.Edit(item) },
                        onDelete = { pendingDelete = item }
                    )
                }
            }
        }
    }

    // Add / edit dialog
    when (val state = dialog) {
        null -> Unit
        is BookmarkDialog.AddBookmark -> BookmarkEditDialog(
            title = "Add bookmark",
            confirmLabel = "Add",
            initialTitle = "",
            initialUrl = "",
            showUrl = true,
            locationName = path.lastOrNull()?.title ?: "Root",
            onConfirm = { name, url ->
                viewModel.addBookmark(name, url, state.parentId)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        is BookmarkDialog.AddFolder -> BookmarkEditDialog(
            title = "Add folder",
            confirmLabel = "Add",
            initialTitle = "",
            initialUrl = "",
            showUrl = false,
            locationName = path.lastOrNull()?.title ?: "Root",
            onConfirm = { name, _ ->
                viewModel.addFolder(name, state.parentId)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        is BookmarkDialog.Edit -> BookmarkEditDialog(
            title = if (state.item.isFolder) "Rename folder" else "Edit bookmark",
            confirmLabel = "Save",
            initialTitle = state.item.title,
            initialUrl = state.item.url,
            showUrl = !state.item.isFolder,
            locationName = path.lastOrNull()?.title ?: "Root",
            onConfirm = { name, url ->
                viewModel.updateBookmark(
                    state.item.copy(
                        title = name,
                        url = if (state.item.isFolder) "" else url
                    )
                )
                dialog = null
            },
            onDismiss = { dialog = null }
        )
    }

    // Delete confirmation
    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (toDelete.isFolder) "Delete folder?" else "Delete bookmark?") },
            text = {
                Text(
                    if (toDelete.isFolder) {
                        "\"${toDelete.title}\" and everything inside it will be removed."
                    } else {
                        "\"${toDelete.title}\" will be removed from your bookmarks."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeBookmark(toDelete.id)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BookmarkRow(
    item: BookmarkEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isFolder) Icons.Default.Folder else Icons.Default.Bookmark,
                contentDescription = null,
                tint = if (item.isFolder) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!item.isFolder) {
                    Text(
                        text = item.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkEditDialog(
    title: String,
    confirmLabel: String,
    initialTitle: String,
    initialUrl: String,
    showUrl: Boolean,
    locationName: String,
    onConfirm: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialTitle) { mutableStateOf(initialTitle) }
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = "Location: $locationName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = if (nameError != null) {
                        { Text(nameError.orEmpty()) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (showUrl) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; urlError = null },
                        label = { Text("URL") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        isError = urlError != null,
                        supportingText = if (urlError != null) {
                            { Text(urlError.orEmpty()) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedUrl = url.trim()
                    var valid = true
                    if (trimmedName.isEmpty()) {
                        nameError = "Name is required"
                        valid = false
                    }
                    if (showUrl && trimmedUrl.isEmpty()) {
                        urlError = "URL is required"
                        valid = false
                    }
                    if (valid) {
                        onConfirm(trimmedName, trimmedUrl)
                    }
                },
                modifier = Modifier.testTag("bookmark_dialog_confirm")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
