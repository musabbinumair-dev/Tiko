package com.example.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == -1L) return

            try {
                val prefs = context.getSharedPreferences("tiko_settings_prefs", Context.MODE_PRIVATE)
                val savedDownloadId = prefs.getLong("update_download_id", -1L)
                
                if (downloadId == savedDownloadId) {
                    Log.d("UpdateDownloadReceiver", "Update download completed: $downloadId")
                    // The screen will handle validation and install when the user opens the app
                }
            } catch (e: Throwable) {
                Log.e("UpdateDownloadReceiver", "Error handling download complete", e)
            }
        }
    }
}
