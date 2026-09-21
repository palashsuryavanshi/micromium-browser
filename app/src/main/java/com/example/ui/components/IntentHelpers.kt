package com.example.ui.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Opens the system Downloads list; falls back to a toast when unavailable. */
fun openSystemDownloads(context: Context) {
    try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No downloads viewer found", Toast.LENGTH_SHORT).show()
    }
}
