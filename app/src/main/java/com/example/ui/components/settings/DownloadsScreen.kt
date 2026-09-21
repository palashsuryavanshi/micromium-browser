package com.example.ui.components.settings

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

/** One row in the in-app downloads list. */
private data class DownloadEntry(
    val id: Long,
    val title: String,
    val status: Int,
    val sizeBytes: Long,
    val mimeType: String?,
    val modifiedAt: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsScreen(
    onOpenSystemDownloads: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<DownloadEntry>>(emptyList()) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) {
        entries = queryDownloads(context)
    }

    SettingsScaffold(
        title = "Downloads",
        onBack = onBack,
        contentTag = "downloads_screen"
    ) {
        OutlinedButton(
            onClick = onOpenSystemDownloads,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("downloads_open_system")
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open system downloads")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Files you download will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entries.forEach { entry ->
                    DownloadRow(
                        entry = entry,
                        onOpen = { openDownload(context, entry) },
                        onDelete = {
                            removeDownload(context, entry)
                            refreshTick += 1
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    entry: DownloadEntry,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val completed = entry.status == DownloadManager.STATUS_SUCCESSFUL
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (entry.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> Icons.Default.CheckCircle
                    DownloadManager.STATUS_FAILED -> Icons.Default.ErrorOutline
                    else -> Icons.Default.HourglassEmpty
                },
                contentDescription = null,
                tint = when (entry.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> MaterialTheme.colorScheme.secondary
                    DownloadManager.STATUS_FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = downloadSubtitle(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (completed) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remove download",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun downloadSubtitle(entry: DownloadEntry): String {
    val statusText = when (entry.status) {
        DownloadManager.STATUS_SUCCESSFUL -> "Complete"
        DownloadManager.STATUS_RUNNING -> "Downloading…"
        DownloadManager.STATUS_PENDING -> "Waiting…"
        DownloadManager.STATUS_PAUSED -> "Paused"
        DownloadManager.STATUS_FAILED -> "Failed"
        else -> "Unknown"
    }
    val parts = mutableListOf(statusText)
    if (entry.sizeBytes > 0) {
        parts += formatBytes(entry.sizeBytes)
    }
    if (entry.modifiedAt > 0) {
        parts += DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(entry.modifiedAt))
    }
    return parts.joinToString(" · ")
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}

private fun queryDownloads(context: Context): List<DownloadEntry> {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val query = DownloadManager.Query()
    return runCatching {
        manager.query(query).use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val titleCol = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)
            val statusCol = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val sizeCol = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val mimeCol = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)
            val timeCol = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)
            buildList {
                while (cursor.moveToNext()) {
                    val title = runCatching { cursor.getString(titleCol) }.getOrNull()
                    add(
                        DownloadEntry(
                            id = cursor.getLong(idCol),
                            title = title?.takeIf { it.isNotBlank() } ?: "Download",
                            status = cursor.getInt(statusCol),
                            sizeBytes = runCatching { cursor.getLong(sizeCol) }.getOrDefault(-1),
                            mimeType = runCatching { cursor.getString(mimeCol) }.getOrNull(),
                            modifiedAt = runCatching { cursor.getLong(timeCol) }.getOrDefault(0)
                        )
                    )
                }
            }.sortedByDescending { it.modifiedAt }
        }
    }.getOrDefault(emptyList())
}

private fun openDownload(context: Context, entry: DownloadEntry) {
    if (entry.status != DownloadManager.STATUS_SUCCESSFUL) {
        Toast.makeText(context, downloadSubtitle(entry), Toast.LENGTH_SHORT).show()
        return
    }
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val uri = runCatching { manager.getUriForDownloadedFile(entry.id) }.getOrNull()
    if (uri == null) {
        Toast.makeText(context, "File no longer available", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, entry.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open file"))
    } catch (e: Exception) {
        Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
    }
}

private fun removeDownload(context: Context, entry: DownloadEntry) {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    runCatching { manager.remove(entry.id) }
    Toast.makeText(context, "Download removed", Toast.LENGTH_SHORT).show()
}
