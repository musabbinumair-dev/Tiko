package com.example.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.musab.tiko.BuildConfig
import java.io.File

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Pending(val message: String = "Starting…") : DownloadState()
    data class Downloading(val progressPercent: Int) : DownloadState()
    data class Paused(val message: String = "Waiting for network…") : DownloadState()
    data class Failed(val reason: String) : DownloadState()
    data class Downloaded(val file: File) : DownloadState()
}

object UpdateDownloader {
    private const val TAG = "TikoUpdateDownloader"
    const val APK_URL = "https://github.com/musabbinumair-dev/Tiko/releases/latest/download/tiko.apk"

    fun getUpdateFile(context: Context): File {
        val dir = try {
            context.getExternalFilesDir("updates") ?: context.getExternalFilesDir(null) ?: context.cacheDir
        } catch (_: Throwable) {
            context.cacheDir
        }
        try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
        } catch (_: Throwable) {}
        return File(dir, "tiko-update.apk")
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                context.packageManager.canRequestPackageInstalls()
            } catch (_: Throwable) {
                false
            }
        } else {
            true
        }
    }

    fun openUnknownAppSourcesSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (_: Throwable) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Throwable) {}
        }
    }

    fun installApk(context: Context, file: File): Boolean {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start APK install", e)
            false
        }
    }

    fun cleanUpIfUpdated(context: Context) {
        try {
            val file = getUpdateFile(context)
            if (file.exists()) {
                val pm = context.packageManager
                val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(file.absolutePath, 0)
                }
                val apkVersionCode = pkgInfo?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        it.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        it.versionCode.toLong()
                    }
                } ?: -1L

                val currentVersion = try { BuildConfig.VERSION_CODE.toLong() } catch (_: Throwable) { 1L }
                if (apkVersionCode <= 0L || currentVersion >= apkVersionCode) {
                    file.delete()
                }
            }
        } catch (_: Throwable) {
            // Ignore failure during cleanup
        }
    }

    fun startDownload(context: Context, expectedVersionCode: Long): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        // Delete old file before starting new download
        val targetFile = getUpdateFile(context)
        try {
            if (targetFile.exists()) {
                targetFile.delete()
            }
        } catch (_: Throwable) {}

        val request = DownloadManager.Request(Uri.parse(APK_URL)).apply {
            setTitle("Downloading Tiko update")
            setDescription("Tiko ${expectedVersionCode}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationUri(Uri.fromFile(targetFile))
        }

        return downloadManager.enqueue(request)
    }

    fun queryDownloadStatus(context: Context, downloadId: Long): DownloadState {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = try {
            downloadManager.query(query)
        } catch (_: Throwable) {
            null
        }

        cursor?.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesDownloadedIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val reasonIndex = it.getColumnIndex(DownloadManager.COLUMN_REASON)

                if (statusIndex < 0) {
                    return DownloadState.Failed("Cannot query download status")
                }

                val status = it.getInt(statusIndex)
                val bytesDownloaded = if (bytesDownloadedIndex >= 0) it.getLong(bytesDownloadedIndex) else 0L
                val bytesTotal = if (bytesTotalIndex >= 0) it.getLong(bytesTotalIndex) else 0L

                return when (status) {
                    DownloadManager.STATUS_PENDING -> DownloadState.Pending()
                    DownloadManager.STATUS_RUNNING -> {
                        val percent = if (bytesTotal > 0) {
                            ((bytesDownloaded * 100) / bytesTotal).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        DownloadState.Downloading(percent)
                    }
                    DownloadManager.STATUS_PAUSED -> DownloadState.Paused()
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val file = getUpdateFile(context)
                        if (file.exists()) {
                            DownloadState.Downloaded(file)
                        } else {
                            DownloadState.Failed("Download completed but file not found")
                        }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reason = if (reasonIndex >= 0) {
                            val reasonCode = it.getInt(reasonIndex)
                            getFailureReason(reasonCode)
                        } else {
                            "Download failed"
                        }
                        DownloadState.Failed(reason)
                    }
                    else -> DownloadState.Failed("Unknown download status: $status")
                }
            }
        }

        return DownloadState.Failed("Download not found")
    }

    private fun getFailureReason(reasonCode: Int): String {
        return when (reasonCode) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No storage device found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_FILE_ERROR -> "Storage error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "HTTP error"
            DownloadManager.ERROR_UNKNOWN -> "Unknown error"
            else -> "Download failed (code: $reasonCode)"
        }
    }

    fun validateApk(context: Context, file: File, expectedVersionCode: Long): String? {
        try {
            if (!file.exists() || file.length() == 0L) {
                return "Downloaded file is empty or missing"
            }
            val pm = context.packageManager ?: return "PackageManager unavailable"
            val pkgInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(file.absolutePath, 0)
                }
            } catch (e: Throwable) {
                return "Failed to parse APK: ${e.message}"
            } ?: return "Invalid APK package archive"

            val packageName = pkgInfo.packageName
            if (packageName != "com.musab.tiko") {
                return "Invalid package name: $packageName"
            }

            val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }

            if (apkVersionCode < expectedVersionCode) {
                return "APK version ($apkVersionCode) is lower than expected ($expectedVersionCode)"
            }

            return null
        } catch (e: Throwable) {
            return "Failed to validate APK: ${e.message}"
        }
    }
}
