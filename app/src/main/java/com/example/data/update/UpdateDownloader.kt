package com.example.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState : java.io.Serializable {
    data object Idle : DownloadState()
    data class Downloading(val progressPercent: Int) : DownloadState()
    data class Failed(val reason: String) : DownloadState()
    data class Downloaded(val file: File) : DownloadState()
}

object UpdateDownloader {
    private const val TAG = "TikoUpdateDownloader"
    const val APK_URL = "https://github.com/musabbinumair-dev/Tiko/releases/latest/download/tiko.apk"

    fun getUpdateFile(context: Context): File {
        val dir = try {
            context.getExternalFilesDir(null) ?: context.cacheDir ?: context.filesDir
        } catch (_: Throwable) {
            context.cacheDir ?: context.filesDir
        }
        try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
        } catch (_: Throwable) {}
        return File(dir, "tiko-update.apk")
    }

    suspend fun downloadApk(
        context: Context,
        expectedVersionCode: Long,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val updateFile = try {
            getUpdateFile(context)
        } catch (e: Throwable) {
            return@withContext Result.failure(Exception("Cannot access storage: ${e.message}", e))
        }

        try {
            if (updateFile.exists()) {
                updateFile.delete()
            }
        } catch (_: Throwable) {}

        var currentUrl = APK_URL
        var redirects = 0
        val maxRedirects = 5

        try {
            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Tiko-Android-Downloader")
                }

                val statusCode = try {
                    connection.responseCode
                } catch (e: Throwable) {
                    try { connection.disconnect() } catch (_: Throwable) {}
                    throw IOException("Network error: ${e.message}", e)
                }

                if (statusCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("HTTP $statusCode redirect missing Location header")
                    try { connection.disconnect() } catch (_: Throwable) {}
                    currentUrl = location
                    redirects++
                    continue
                }

                if (statusCode !in 200..299) {
                    val err = try {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    } catch (_: Throwable) { "" }
                    try { connection.disconnect() } catch (_: Throwable) {}
                    throw IOException("HTTP $statusCode ${connection.responseMessage}$err")
                }

                val fileLength = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(updateFile)

                val buffer = ByteArray(8192)
                var bytesDownloaded = 0L
                var lastReportedPercent = -1

                try {
                    var bytesRead = inputStream.read(buffer)
                    while (bytesRead != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        if (fileLength > 0) {
                            val percent = ((bytesDownloaded * 100) / fileLength).toInt().coerceIn(0, 100)
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                try {
                                    onProgress(percent)
                                } catch (_: Throwable) {}
                            }
                        }
                        bytesRead = inputStream.read(buffer)
                    }
                    outputStream.flush()
                } finally {
                    try { inputStream.close() } catch (_: Throwable) {}
                    try { outputStream.close() } catch (_: Throwable) {}
                    try { connection.disconnect() } catch (_: Throwable) {}
                }
                break
            }

            // Validate package archive
            val validationError = try {
                validateApk(context, updateFile, expectedVersionCode)
            } catch (e: Throwable) {
                "Failed to validate APK: ${e.message}"
            }

            if (validationError != null) {
                try {
                    if (updateFile.exists()) updateFile.delete()
                } catch (_: Throwable) {}
                return@withContext Result.failure(IOException(validationError))
            }

            Result.success(updateFile)
        } catch (e: Throwable) {
            try {
                if (updateFile.exists()) {
                    updateFile.delete()
                }
            } catch (_: Throwable) {}
            Result.failure(if (e is Exception) e else Exception(e.message ?: "Download failed", e))
        }
    }

    private fun validateApk(context: Context, file: File, expectedVersionCode: Long): String? {
        try {
            if (!file.exists() || file.length() == 0L) {
                return "Downloaded file is empty or missing"
            }
            val pm = context.packageManager ?: return "PackageManager unavailable"
            val pkgInfo = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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

            val apkVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
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
