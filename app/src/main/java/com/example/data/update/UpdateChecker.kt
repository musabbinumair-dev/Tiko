package com.example.data.update

import android.util.Log
import com.musab.tiko.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateResult {
    data object UpToDate : UpdateResult()
    data class UpdateAvailable(
        val versionName: String,
        val versionCode: Long,
        val notes: String
    ) : UpdateResult()
    data class CheckFailed(val reason: String) : UpdateResult()
}

object UpdateChecker {
    const val UPDATE_URL = "https://github.com/musabbinumair-dev/Tiko/releases/latest/download/version.json"
    private const val TAG = "TikoUpdate"
    private const val TIMEOUT_MS = 10_000

    private val _result = MutableStateFlow<UpdateResult?>(null)
    val result: StateFlow<UpdateResult?> = _result.asStateFlow()

    @Volatile
    private var hasCheckedOnce = false

    fun checkOnce() {
        if (hasCheckedOnce) return
        hasCheckedOnce = true
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            check()
        }
    }

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        val checkResult = try {
            val responseBody = fetchWithRedirects(UPDATE_URL)
            parseAndCompare(responseBody)
        } catch (e: Exception) {
            val reason = e.message ?: e.javaClass.simpleName
            UpdateResult.CheckFailed(reason)
        }

        _result.value = checkResult

        when (checkResult) {
            is UpdateResult.UpToDate -> Log.d(TAG, "Update check: up to date")
            is UpdateResult.UpdateAvailable -> Log.d(TAG, "Update check: update available ${checkResult.versionName}")
            is UpdateResult.CheckFailed -> Log.d(TAG, "Update check: failed (${checkResult.reason})")
        }

        checkResult
    }

    private fun fetchWithRedirects(startUrl: String, maxRedirects: Int = 5): String {
        var currentUrl = startUrl
        var redirects = 0

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Tiko-Android-UpdateChecker")
            }

            try {
                val statusCode = connection.responseCode
                if (statusCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("HTTP $statusCode redirect missing Location header")
                    connection.disconnect()
                    currentUrl = location
                    redirects++
                    continue
                }

                if (statusCode !in 200..299) {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    val message = "HTTP $statusCode ${connection.responseMessage}${if (errorBody.isNotEmpty()) ": $errorBody" else ""}".trim()
                    throw IOException(message)
                }

                return connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }

        throw IOException("Too many redirects: $redirects")
    }

    private fun parseAndCompare(jsonString: String): UpdateResult {
        val json = JSONObject(jsonString)
        if (!json.has("versionCode")) {
            return UpdateResult.CheckFailed("Missing versionCode in version.json")
        }

        val remoteVersionCode = json.optLong("versionCode", -1L)
        if (remoteVersionCode <= 0L) {
            return UpdateResult.CheckFailed("Invalid versionCode ($remoteVersionCode)")
        }

        val remoteVersionName = json.optString("versionName", "")
        val notes = json.optString("notes", "")

        val currentVersionCode = BuildConfig.VERSION_CODE.toLong()
        return if (remoteVersionCode > currentVersionCode) {
            UpdateResult.UpdateAvailable(
                versionName = remoteVersionName.ifEmpty { "1.0.$remoteVersionCode" },
                versionCode = remoteVersionCode,
                notes = notes
            )
        } else {
            UpdateResult.UpToDate
        }
    }
}
