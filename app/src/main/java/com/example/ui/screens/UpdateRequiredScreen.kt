package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.update.DownloadState
import com.example.data.update.UpdateDownloader
import com.example.data.update.UpdateResult
import com.example.ui.components.AppOverlayHost
import com.example.ui.components.AppToastController
import com.example.ui.components.ToastType
import com.example.ui.theme.OutfitFontFamily
import com.musab.tiko.BuildConfig
import kotlinx.coroutines.launch
import java.io.File

fun parseReleaseNotes(raw: String): List<String> {
    val lines = raw.lines()
        .map { it.trim() }
        .filter { line ->
            line.isNotEmpty() &&
            !line.startsWith("Co-authored-by:", ignoreCase = true) &&
            !line.startsWith("Signed-off-by:", ignoreCase = true) &&
            !line.startsWith("Merge ", ignoreCase = true)
        }
    if (lines.isEmpty()) {
        return listOf(
            "Performance improvements and optimizations",
            "Bug fixes and UI polish"
        )
    }
    return lines.map { line ->
        var cleaned = line.removePrefix("- ").removePrefix("* ").removePrefix("• ").trim()
        val colonIndex = cleaned.indexOf(':')
        if (colonIndex in 1..12) {
            val prefix = cleaned.substring(0, colonIndex).trim().lowercase()
            if (prefix in listOf("feat", "fix", "chore", "perf", "refactor", "style", "docs", "test")) {
                cleaned = cleaned.substring(colonIndex + 1).trim()
            }
        }
        cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }.filter { it.isNotBlank() }
}

private val PurplePrimary = Color(0xFF6B4EFF)
private val LightScreenBg = Color(0xFFEFEBDD)
private val DarkScreenBg = Color(0xFF141414)
private val ButtonCream = Color(0xFFF0EADC)
private val ButtonBlack = Color(0xFF151515)
private val LimeAccent = Color(0xFFC8DB6A)

@Composable
fun UpdateRequiredScreen(
    update: UpdateResult.UpdateAvailable,
    isDark: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier,
    onError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Using remember instead of rememberSaveable to avoid SaveableStateRegistry crashes on custom sealed class
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var canInstallPermission by remember {
        mutableStateOf(UpdateDownloader.canRequestPackageInstalls(context))
    }
    var waitingForInstallPermission by remember { mutableStateOf(false) }

    fun triggerInstall(file: File) {
        val hasPermission = UpdateDownloader.canRequestPackageInstalls(context)
        canInstallPermission = hasPermission
        if (!hasPermission) {
            waitingForInstallPermission = true
        } else {
            waitingForInstallPermission = false
            val launched = UpdateDownloader.installApk(context, file)
            if (!launched) {
                AppToastController.show("Could not launch installer", ToastType.Error)
            }
        }
    }

    LaunchedEffect(update) {
        try {
            if (update.versionCode <= 0L) {
                onError?.invoke()
                return@LaunchedEffect
            }
            // Check if already downloaded and valid
            val existingFile = UpdateDownloader.getUpdateFile(context)
            if (existingFile.exists() && existingFile.length() > 0L) {
                val validationErr = UpdateDownloader.validateApk(context, existingFile, update.versionCode)
                if (validationErr == null) {
                    downloadState = DownloadState.Downloaded(existingFile)
                    canInstallPermission = UpdateDownloader.canRequestPackageInstalls(context)
                }
            }
        } catch (_: Throwable) {
            onError?.invoke()
        }
    }

    // Monitor onResume to re-check install permission and proceed without extra taps
    DisposableEffect(lifecycleOwner, downloadState, waitingForInstallPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = UpdateDownloader.canRequestPackageInstalls(context)
                canInstallPermission = hasPermission
                val currentState = downloadState
                if (currentState is DownloadState.Downloaded) {
                    if (hasPermission && waitingForInstallPermission) {
                        waitingForInstallPermission = false
                        UpdateDownloader.installApk(context, currentState.file)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Prevent closing via system back button; minimize app instead
    BackHandler(enabled = true) {
        try {
            activity?.moveTaskToBack(true)
        } catch (_: Throwable) {
            // Ignore
        }
    }

    val screenBg = if (isDark) DarkScreenBg else LightScreenBg
    val textPrimary = if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515)
    val textSecondary = if (isDark) Color(0xFFA8A39A) else Color(0xFF6B6B6B)

    val chipInstalledBg = if (isDark) Color(0xFF242424) else Color.White
    val chipInstalledBorder = if (isDark) Color(0xFF383838) else Color(0xFFDCD8CB)

    val chipNewBg = if (isDark) Color(0xFF2C243B) else Color(0xFFEDE8FD)
    val chipNewBorder = PurplePrimary
    val chipNewText = if (isDark) Color(0xFFD4C8FF) else PurplePrimary

    val buttonBg = if (isDark) ButtonCream else ButtonBlack
    val buttonTextColor = if (isDark) Color(0xFF151515) else Color.White

    val releaseNotesList = remember(update.notes) {
        try {
            parseReleaseNotes(update.notes)
        } catch (_: Throwable) {
            listOf("Bug fixes and improvements")
        }
    }

    fun startDownload() {
        try {
            downloadState = DownloadState.Downloading(0)
            scope.launch {
                try {
                    val result = UpdateDownloader.downloadApk(
                        context = context,
                        expectedVersionCode = update.versionCode
                    ) { percent ->
                        try {
                            downloadState = DownloadState.Downloading(percent)
                        } catch (_: Throwable) {}
                    }

                    if (result.isSuccess) {
                        val file = result.getOrThrow()
                        downloadState = DownloadState.Downloaded(file)
                        triggerInstall(file)
                    } else {
                        val reason = result.exceptionOrNull()?.message ?: "Unknown error"
                        downloadState = DownloadState.Failed(reason)
                    }
                } catch (e: Throwable) {
                    val reason = e.message ?: e.javaClass.simpleName
                    downloadState = DownloadState.Failed(reason)
                }
            }
        } catch (e: Throwable) {
            val reason = e.message ?: e.javaClass.simpleName
            downloadState = DownloadState.Failed(reason)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("update_required_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))

                // Large bold uppercase title
                Text(
                    text = "UPDATE REQUIRED",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.5).sp,
                    color = textPrimary,
                    modifier = Modifier.testTag("update_required_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "A new version of Tiko is ready",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    color = textSecondary,
                    modifier = Modifier.testTag("update_required_subtitle")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Two small chips: Installed and New
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Installed chip
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(chipInstalledBg)
                            .border(1.dp, chipInstalledBorder, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("update_installed_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Installed ${try { BuildConfig.VERSION_NAME } catch (_: Throwable) { "1.0.0" }}",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }

                    // New chip
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(chipNewBg)
                            .border(1.dp, chipNewBorder, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("update_new_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "New ${try { update.versionName } catch (_: Throwable) { "Update" }}",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = chipNewText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Purple card with 28dp radius
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(PurplePrimary)
                        .padding(24.dp)
                        .testTag("update_card_purple")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // White pill labelled "What's new"
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("update_whats_new_pill"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "What's new",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF151515)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))


                        // Release notes inside purple card (max about 6 lines, scrollable)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState())
                                .testTag("update_release_notes")
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                releaseNotesList.forEach { note ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 7.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(LimeAccent)
                                        )
                                        Text(
                                            text = note,
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom action area based on DownloadState
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = downloadState) {
                    is DownloadState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(CircleShape)
                                .background(buttonBg)
                                .clickable { startDownload() }
                                .testTag("update_now_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Update now",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = buttonTextColor
                            )
                        }
                    }
                    is DownloadState.Downloading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF242424) else Color(0xFFE0DBD0))
                                .testTag("update_downloading_box"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val progressFraction = (state.progressPercent / 100f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(56.dp)
                                    .background(PurplePrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Downloading… ${state.progressPercent}%",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isDark) Color.White else Color(0xFF151515)
                                )
                            }
                        }
                    }
                    is DownloadState.Failed -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Download failed: ${state.reason}",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color(0xFFD9534A),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(CircleShape)
                                        .background(buttonBg)
                                        .clickable { startDownload() }
                                        .testTag("update_try_again_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Try again",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = buttonTextColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Open in browser",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = textSecondary,
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(UpdateDownloader.APK_URL)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Throwable) {
                                            AppToastController.show("Could not open browser", ToastType.Error)
                                        }
                                    }
                                    .testTag("update_open_browser_link")
                            )
                        }
                    }
                    is DownloadState.Downloaded -> {
                        if (!canInstallPermission) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(if (isDark) Color(0xFF242424) else Color.White)
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0xFF383838) else Color(0xFFDCD8CB),
                                        RoundedCornerShape(28.dp)
                                    )
                                    .padding(24.dp)
                                    .testTag("update_permission_card")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "Allow Tiko to install updates",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        lineHeight = 24.sp,
                                        color = textPrimary,
                                        modifier = Modifier.testTag("update_permission_title")
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "To install this update, please grant permission to install apps from this source in settings.",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = textSecondary,
                                        modifier = Modifier.testTag("update_permission_description")
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .clip(CircleShape)
                                            .background(PurplePrimary)
                                            .clickable {
                                                waitingForInstallPermission = true
                                                UpdateDownloader.openUnknownAppSourcesSettings(context)
                                            }
                                            .testTag("update_open_settings_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Open settings",
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(CircleShape)
                                    .background(buttonBg)
                                    .clickable {
                                        val launched = UpdateDownloader.installApk(context, state.file)
                                        if (!launched) {
                                            AppToastController.show("Could not launch installer", ToastType.Error)
                                        }
                                    }
                                    .testTag("update_install_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Install",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = buttonTextColor
                                )
                            }
                        }
                    }
                }
            }
        }

        AppOverlayHost()
    }
}
