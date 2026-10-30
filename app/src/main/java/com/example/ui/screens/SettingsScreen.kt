package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import com.example.ui.theme.LocalTikoThemeColors
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.local.UserPreferences
import com.example.ui.components.AvatarOption
import com.example.ui.components.PresetAvatars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.zIndex
import com.example.ui.components.AppDialogController
import com.example.ui.components.AppToastController
import com.example.ui.components.ToastType
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.reminder.NotificationPermissionHelper
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musab.tiko.BuildConfig
import com.musab.tiko.R
import com.example.ui.components.AppBlack
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppCreamBackground
import com.example.ui.components.ArrowOutwardIcon
import com.example.ui.components.CardLime
import com.example.ui.components.CardPurple
import com.example.ui.components.CardYellow
import com.example.ui.components.RoundIconButton
import com.example.ui.components.screenTopPadding
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel

private data class WeeklyBarData(
    val day: String,
    val value: Int,
    val isHighlight: Boolean = false
)

@Composable
fun SettingsScreen(
    viewModel: TikoViewModel,
    onNavigate: (Screen) -> Unit = {},
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tikoColors = com.example.ui.theme.LocalTikoThemeColors.current
    val context = LocalContext.current
    val authRepository = remember(context) { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val currentUser by authRepository.currentUser.collectAsState(initial = null)
    
    // Intercept hardware/gesture back press to return to Home
    BackHandler {
        viewModel.selectTab(NavigationTab.HOME)
    }

    val tasks by viewModel.allTasks.collectAsState()
    val preferences = viewModel.preferences

    val userName by preferences.userName.collectAsState()
    val userEmail by preferences.userEmail.collectAsState()
    val avatarId by preferences.avatarId.collectAsState()
    val notifEnabled by preferences.notificationsEnabled.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    var systemNotifGranted by remember { mutableStateOf(NotificationPermissionHelper.hasNotificationPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = NotificationPermissionHelper.hasNotificationPermission(context)
                systemNotifGranted = granted
                if (!granted && preferences.notificationsEnabled.value) {
                    preferences.setNotificationsEnabled(false)
                    viewModel.cancelAllReminders()
                } else if (granted && preferences.notificationsEnabled.value) {
                    viewModel.rescheduleAllReminders()
                    NotificationPermissionHelper.checkAndShowExactAlarmNotice(context, preferences)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationsOn = notifEnabled && systemNotifGranted

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        systemNotifGranted = isGranted
        if (isGranted) {
            preferences.setNotificationsEnabled(true)
            viewModel.rescheduleAllReminders()
            NotificationPermissionHelper.checkAndShowExactAlarmNotice(context, preferences)
            AppToastController.show("Notifications enabled", ToastType.Success)
        } else {
            preferences.setNotificationsEnabled(false)
            viewModel.cancelAllReminders()
        }
    }
    val isSystemDark = isSystemInDarkTheme()
    val darkModePref by preferences.darkModeEnabled.collectAsState()
    val darkModeOn = darkModePref ?: (if (isDark) true else isSystemDark)
    var selectedLanguage by remember { mutableStateOf("English") }

    var showAvatarSheet by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(userName) }
    var editEmailInput by remember { mutableStateOf(userEmail) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePasswordInput by remember { mutableStateOf("") }

    // Dynamic stats calculation from real data only
    val completedCount = remember(tasks) { tasks.count { it.done } }
    val doneCount = completedCount.toString()
    val streakText = remember(tasks) {
        val completedDates = tasks.filter { it.done && it.dueDate.isNotBlank() }.map { it.dueDate }.distinct()
        completedDates.size.toString()
    }
    val onTimeText = remember(tasks) {
        val total = tasks.size
        if (total > 0) {
            val completed = tasks.count { it.done }
            val pct = ((completed.toFloat() / total) * 100).toInt()
            "$pct%"
        } else {
            "0%"
        }
    }

    // Weekly completion distribution from real data
    val weeklyData = remember(tasks) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfWeek = cal.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, 7)
        val endOfWeek = cal.timeInMillis

        val counts = IntArray(7)
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (task in tasks) {
            if (task.done) {
                val taskTime = task.completedAt ?: try {
                    if (task.dueDate.isNotBlank()) sdfDate.parse(task.dueDate)?.time else null
                } catch (_: Exception) {
                    null
                }
                if (taskTime != null && taskTime >= startOfWeek && taskTime < endOfWeek) {
                    val taskCal = Calendar.getInstance().apply { timeInMillis = taskTime }
                    val dayOfWeek = taskCal.get(Calendar.DAY_OF_WEEK)
                    val index = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                    if (index in 0..6) {
                        counts[index]++
                    }
                }
            }
        }

        val todayCal = Calendar.getInstance()
        val todayDayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)
        val todayIndex = if (todayDayOfWeek == Calendar.SUNDAY) 6 else todayDayOfWeek - 2

        days.mapIndexed { index, day ->
            WeeklyBarData(day, counts[index], isHighlight = (index == todayIndex))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tikoColors.screenBg)
    ) {
        // Header Gradient: in dark mode golden-brown #4B3512 to #141414, in light mode #FFCF4D to cream
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(tikoColors.gradientStart, tikoColors.gradientEnd)
                    )
                )
        )

        // Main scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("profile_screen_scroll"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // TOP ROW: Back button (left), Edit pencil button (right)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .screenTopPadding()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundIconButton(
                        icon = Icons.Outlined.ChevronLeft,
                        onClick = { viewModel.selectTab(NavigationTab.HOME) },
                        contentDescription = "Back",
                        isDark = darkModeOn,
                        testTag = "profile_back_button"
                    )

                    RoundIconButton(
                        icon = Icons.Outlined.Edit,
                        onClick = {
                            editNameInput = userName
                            editEmailInput = userEmail
                            showEditProfileDialog = true
                        },
                        contentDescription = "Edit Profile",
                        isDark = darkModeOn,
                        testTag = "profile_edit_button"
                    )
                }
            }

            // AVATAR & USER INFO
            item {
                Spacer(modifier = Modifier.height(10.dp))
                val user = currentUser
                val displayName = user?.displayName ?: userName.ifBlank { "Guest" }
                val displayEmail = user?.email ?: userEmail.ifBlank { "user@example.com" }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered 110dp avatar with yellow #FFB92E fill and 36dp purple pencil badge
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .testTag("profile_avatar_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.ui.components.UserAvatar(
                            name = displayName,
                            size = 110.dp,
                            fontSize = 40.sp,
                            avatarId = avatarId,
                            onClick = {
                                showAvatarSheet = true
                            },
                            modifier = Modifier.testTag("profile_avatar")
                        )

                        // 36dp purple #6B4EFF circle with white pencil icon (Icons.Outlined.Edit) at bottom-right
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(CardPurple)
                                .border(2.dp, tikoColors.screenBg, CircleShape)
                                .clickable {
                                    showAvatarSheet = true
                                }
                                .testTag("profile_avatar_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Choose avatar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Name: Outfit ExtraBold 34sp
                    Text(
                        text = displayName,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        color = tikoColors.textPrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pill with email or "Log in" button for guest
                    if (user != null || userEmail.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(tikoColors.cardBg)
                                .border(1.dp, tikoColors.cardBorder, CircleShape)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayEmail,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = tikoColors.textPrimary
                            )
                        }
                        if (user != null) {
                            val syncStatus by viewModel.syncStatus.collectAsState()
                            if (syncStatus.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = syncStatus,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (darkModeOn) Color(0xFFA8A39A) else Color(0xFF5B5F66),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Guest mode: show "Log in" button
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(if (darkModeOn) Color(0xFFF5F1E6) else AppBlack)
                                .clickable {
                                    onNavigate(Screen.Login)
                                }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Log in",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (darkModeOn) Color(0xFF151515) else Color.White
                            )
                        }
                    }
                }
            }

            // 3 STAT CARDS IN A ROW: Purple, Yellow, Lime
            item {
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Purple #6B4EFF "Done"
                    ProfileStatCard(
                        number = doneCount,
                        label = "Done",
                        backgroundColor = CardPurple,
                        contentColor = Color.White,
                        icon = Icons.Outlined.Check,
                        testTag = "stat_card_done",
                        isDark = darkModeOn,
                        modifier = Modifier.weight(1f)
                    )

                    // Card 2: Yellow #FFB92E "Day Streak"
                    ProfileStatCard(
                        number = streakText,
                        label = "Day Streak",
                        backgroundColor = CardYellow,
                        contentColor = AppBlack,
                        icon = Icons.Outlined.LocalFireDepartment,
                        testTag = "stat_card_streak",
                        isDark = darkModeOn,
                        modifier = Modifier.weight(1f)
                    )

                    // Card 3: Lime #C8DB6A "On time"
                    ProfileStatCard(
                        number = onTimeText,
                        label = "On time",
                        backgroundColor = CardLime,
                        contentColor = AppBlack,
                        icon = Icons.Outlined.TrackChanges,
                        testTag = "stat_card_ontime",
                        isDark = darkModeOn,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // "This week" SECTION
            item {
                Spacer(modifier = Modifier.height(20.dp))
                // Pill label "This week"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(tikoColors.cardBg)
                            .border(1.dp, tikoColors.cardBorder, CircleShape)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This week",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = tikoColors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card, 28dp radius, 7-bar chart (Mon to Sun)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(tikoColors.cardBg)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    val maxValue = (weeklyData.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyData.forEach { item ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Value above bar in Outfit Bold 13sp
                                Text(
                                    text = item.value.toString(),
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (darkModeOn) Color(0xFFF5F1E6) else AppBlack
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Rounded vertical bar
                                val barHeight = ((item.value.toFloat() / maxValue) * 78f).coerceAtLeast(20f).dp
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                        .background(if (item.isHighlight) CardYellow else CardPurple)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Day label below bar
                                Text(
                                    text = item.day,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (darkModeOn) Color(0xFFF5F1E6) else AppBlack
                                )
                            }
                        }
                    }
                }
            }

            // "Settings" SECTION
            item {
                Spacer(modifier = Modifier.height(20.dp))
                // Pill label "Settings"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(tikoColors.cardBg)
                            .border(1.dp, tikoColors.cardBorder, CircleShape)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Settings",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = tikoColors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card, 28dp radius, 4 rows separated by thin lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(tikoColors.cardBg)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Notifications (purple toggle, on)
                        SettingsRow(
                            icon = Icons.Outlined.Notifications,
                            label = "Notifications",
                            isDark = darkModeOn,
                            trailing = {
                                ProfileSwitch(
                                    checked = notificationsOn,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            AppDialogController.confirm(
                                                title = "Allow reminders?",
                                                message = "Tiko needs notification access to remind you about tasks.",
                                                confirmText = "Allow",
                                                cancelText = "Not now",
                                                onConfirm = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        if (activity != null && NotificationPermissionHelper.isPermanentlyDenied(activity, preferences)) {
                                                            NotificationPermissionHelper.openNotificationSettings(context)
                                                        } else {
                                                            preferences.setHasRequestedNotificationPermission(true)
                                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        }
                                                    } else {
                                                        if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
                                                            NotificationPermissionHelper.openNotificationSettings(context)
                                                        } else {
                                                            preferences.setNotificationsEnabled(true)
                                                            systemNotifGranted = true
                                                            viewModel.rescheduleAllReminders()
                                                            NotificationPermissionHelper.checkAndShowExactAlarmNotice(context, preferences)
                                                            AppToastController.show("Notifications enabled", ToastType.Success)
                                                        }
                                                    }
                                                },
                                                onCancel = {}
                                            )
                                        } else {
                                            preferences.setNotificationsEnabled(false)
                                            viewModel.cancelAllReminders()
                                            AppToastController.show("Notifications disabled", ToastType.Info)
                                        }
                                    },
                                    isDark = darkModeOn,
                                    testTag = "switch_notifications"
                                )
                            }
                        )

                        SettingsDivider(isDark = darkModeOn)

                        // 2. Reminders (chevron)
                        SettingsRow(
                            icon = Icons.Outlined.CalendarToday,
                            label = "Reminders",
                            isDark = darkModeOn,
                            onClick = {
                                AppDialogController.alert(
                                    title = "Reminders & Alerts",
                                    message = "Task due date reminders are scheduled automatically 15 minutes before deadlines."
                                )
                            },
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open reminders",
                                    tint = if (darkModeOn) Color(0xFFF5F1E6) else AppBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )

                        SettingsDivider(isDark = darkModeOn)

                        // 3. Dark mode (gray toggle, off)
                        SettingsRow(
                            icon = Icons.Outlined.DarkMode,
                            label = "Dark mode",
                            isDark = darkModeOn,
                            trailing = {
                                ProfileSwitch(
                                    checked = darkModeOn,
                                    onCheckedChange = { checked ->
                                        preferences.setDarkModeEnabled(checked)
                                        AppToastController.show(if (checked) "Dark mode enabled" else "Dark mode disabled", ToastType.Success)
                                    },
                                    isDark = darkModeOn,
                                    testTag = "switch_dark_mode"
                                )
                            }
                        )

                        SettingsDivider(isDark = darkModeOn)

                        // 4. Language ("English" + chevron)
                        SettingsRow(
                            icon = Icons.Outlined.Language,
                            label = "Language",
                            isDark = darkModeOn,
                            onClick = { showLanguageDialog = true },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedLanguage,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = if (darkModeOn) Color(0xFFA8A39A) else Color(0xFF6B6B6B)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Choose language",
                                        tint = if (darkModeOn) Color(0xFFF5F1E6) else AppBlack,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // "Log out" BUTTON
            if (currentUser != null) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(if (darkModeOn) Color(0xFF242424) else Color(0xFF151515))
                            .border(1.dp, if (darkModeOn) Color(0xFF383838) else Color.Transparent, CircleShape)
                            .clickable {
                                AppDialogController.confirm(
                                    title = "Log Out",
                                    message = "Are you sure you want to log out of your account?",
                                    confirmText = "Log out",
                                    destructive = true,
                                    onConfirm = {
                                        scope.launch {
                                            val flushSuccess = viewModel.flushPendingWrites(5000L)
                                            if (flushSuccess) {
                                                authRepository.signOut()
                                                viewModel.logout()
                                                AppToastController.show("Logged out successfully", ToastType.Success)
                                                onNavigate(Screen.Welcome)
                                            } else {
                                                AppDialogController.confirm(
                                                    title = "Unsaved changes",
                                                    message = "Some changes haven't synced yet. Logging out now will lose them.",
                                                    confirmText = "Log out anyway",
                                                    cancelText = "Cancel",
                                                    destructive = true,
                                                    onConfirm = {
                                                        scope.launch {
                                                            authRepository.signOut()
                                                            viewModel.logout()
                                                            AppToastController.show("Logged out", ToastType.Info)
                                                            onNavigate(Screen.Welcome)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            .testTag("profile_logout_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log out",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (darkModeOn) Color(0xFFF5F1E6) else Color.White
                        )
                    }
                }

                // "Delete account" BUTTON (red)
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                            .clickable {
                                deletePasswordInput = ""
                                showDeleteDialog = true
                            }
                            .testTag("profile_delete_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Delete account",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = tikoColors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_app_version")
                )
            }
        }

        // Soft fade scrim behind the bar (90dp gradient, transparent to near-black or cream at 80%)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            (if (darkModeOn) Color(0xFF141414) else Color(0xFFEFEBDD)).copy(alpha = 0.80f)
                        )
                    )
                )
        )
    }

    // DIALOGS & BOTTOM SHEETS

    // 0. Choose Avatar Modal Bottom Sheet
    if (showAvatarSheet) {
        ChooseAvatarBottomSheet(
            selectedAvatarId = avatarId,
            onAvatarSelected = { newId ->
                // null means "reset to default" (Google photo or initials)
                preferences.setAvatarId(newId)
                showAvatarSheet = false
            },
            onDismiss = { showAvatarSheet = false }
        )
    }

    // 1. Edit Profile Custom Dialog
    if (showEditProfileDialog) {
        BackHandler { showEditProfileDialog = false }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f)
                .background(Color.Black.copy(alpha = 0.40f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showEditProfileDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Edit Profile",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = AppBlack
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Name", fontFamily = OutfitFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CardPurple,
                            focusedLabelColor = CardPurple
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editEmailInput,
                        onValueChange = { editEmailInput = it },
                        label = { Text("Email", fontFamily = OutfitFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CardPurple,
                            focusedLabelColor = CardPurple
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(AppBlack)
                            .clickable {
                                preferences.setUserName(editNameInput.trim())
                                preferences.setUserEmail(editEmailInput.trim())
                                showEditProfileDialog = false
                                AppToastController.show("Profile updated", ToastType.Success)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, AppBlack, CircleShape)
                            .clickable { showEditProfileDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = AppBlack,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // 2. Language Selection Custom Dialog
    if (showLanguageDialog) {
        val languages = listOf("English", "Spanish", "French", "German", "Japanese")
        BackHandler { showLanguageDialog = false }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f)
                .background(Color.Black.copy(alpha = 0.40f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showLanguageDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select Language",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = AppBlack
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedLanguage = lang
                                    showLanguageDialog = false
                                    AppToastController.show("Language set to $lang", ToastType.Success)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang,
                                fontFamily = OutfitFontFamily,
                                fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal,
                                color = if (lang == selectedLanguage) CardPurple else AppBlack,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, AppBlack, CircleShape)
                            .clickable { showLanguageDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = AppBlack,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // 3. Delete Account Modal Popup Dialog (true window modal)
    if (showDeleteDialog) {
        Dialog(
            onDismissRequest = {
                showDeleteDialog = false
                deletePasswordInput = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showDeleteDialog = false
                        deletePasswordInput = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume click */ }
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Warning badge
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEEEE))
                                .border(1.dp, Color(0xFFFF3B30), CircleShape)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Permanent Action",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFFF3B30)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Delete Account",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = AppBlack
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "This will permanently remove your account, profile, and all saved tasks. This cannot be undone.",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = Color(0xFF6B6B6B),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = deletePasswordInput,
                            onValueChange = { deletePasswordInput = it },
                            placeholder = { Text("Password (optional if signed in with Google)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppBlack,
                                unfocusedBorderColor = Color(0xFFCCCCCC)
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFEBDD))
                                    .border(1.dp, Color(0xFF151515), CircleShape)
                                    .clickable {
                                        showDeleteDialog = false
                                        deletePasswordInput = ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AppBlack
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                                    .clickable {
                                        scope.launch {
                                            val uid = currentUser?.uid
                                            val performAccountDeletion: suspend () -> Unit = {
                                                if (!uid.isNullOrBlank()) {
                                                    try {
                                                        viewModel.syncManager.deleteAllUserTasks(uid)
                                                    } catch (_: Exception) {
                                                        // continue to account deletion
                                                    }
                                                }
                                                when (val deleteResult = authRepository.deleteAccount()) {
                                                    is AuthResult.Success -> {
                                                        viewModel.logout()
                                                        AppToastController.show("Account deleted", ToastType.Success)
                                                        showDeleteDialog = false
                                                        onNavigate(Screen.Welcome)
                                                    }
                                                    is AuthResult.Error -> {
                                                        AppToastController.show(deleteResult.message, ToastType.Error)
                                                    }
                                                }
                                            }

                                            if (deletePasswordInput.isNotBlank()) {
                                                when (val reauth = authRepository.reauthenticate(deletePasswordInput)) {
                                                    is AuthResult.Success -> {
                                                        performAccountDeletion()
                                                    }
                                                    is AuthResult.Error -> {
                                                        // Fallback attempt direct delete (e.g. for Google users)
                                                        performAccountDeletion()
                                                    }
                                                }
                                            } else {
                                                performAccountDeletion()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Delete",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Square-ish Stat Card (Purple, Yellow, Lime):
 * White circle icon top-left, white arrow_outward circle top-right,
 * Number Outfit ExtraBold 34sp, Label Outfit Medium 15sp.
 */
@Composable
private fun ProfileStatCard(
    number: String,
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    icon: ImageVector,
    testTag: String,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val circleBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val circleIconColor = if (isDark) Color(0xFFF5F1E6) else AppBlack

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top row: left icon circle + right arrow circle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 30dp circle with icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(circleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = circleIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Right 30dp circle with diagonal arrow
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(circleBg),
                    contentAlignment = Alignment.Center
                ) {
                    ArrowOutwardIcon(
                        size = 14.dp,
                        color = circleIconColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Number: Outfit ExtraBold 34sp
            Text(
                text = number,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                color = contentColor,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Label: Outfit Medium 15sp
            Text(
                text = label,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = contentColor
            )
        }
    }
}

/**
 * Smooth custom toggle switch matching the mockup exactly:
 * Purple #6B4EFF when on, Gray #E0E0E0 (or #383838 in dark) when off, with white sliding circular thumb.
 */
@Composable
private fun ProfileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    testTag: String = "profile_switch"
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        label = "switch_thumb"
    )

    val targetTrackColor = if (checked) {
        CardPurple
    } else {
        if (isDark) Color(0xFF383838) else Color(0xFFE0E0E0)
    }
    val trackColor by animateColorAsState(
        targetValue = targetTrackColor,
        animationSpec = tween(durationMillis = 350),
        label = "switch_track_color"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 3.dp)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * Settings Row Component inside the 28dp card:
 * Icon (24dp outlined), Label (Outfit Medium 17sp), Trailing item (Switch or Chevron).
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
        .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val tikoColors = LocalTikoThemeColors.current
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tikoColors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = tikoColors.textPrimary
            )
        }

        trailing()
    }
}

/**
 * Horizontal divider between settings rows
 */
@Composable
private fun SettingsDivider(isDark: Boolean = false) {
    val tikoColors = LocalTikoThemeColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(tikoColors.divider)
    )
}

/**
 * Modal bottom sheet for choosing user avatar.
 * Displays a 4x3 grid of 12 preset avatars.
 * The currently selected avatar displays a 3dp purple ring.
 * Tapping an avatar selects it, saves its id to UserPreferences, and closes the sheet.
 * Pads bottom for the system navigation bar and is scrollable on smaller screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseAvatarBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    selectedAvatarId: String? = null,
    // null means "reset to default" (Google photo for Google users, initials for email users)
    onAvatarSelected: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tikoColors = LocalTikoThemeColors.current

    val userPrefs = remember(context) { UserPreferences(context) }
    val savedAvatarId by userPrefs.avatarId.collectAsState()
    val activeAvatarId = selectedAvatarId ?: savedAvatarId

    // Determine if the signed-in user has a Google profile photo
    val googlePhotoUrl: String? = remember {
        try {
            val raw = com.google.firebase.auth.FirebaseAuth.getInstance()
                .currentUser?.photoUrl?.toString()?.ifBlank { null }
            // Upgrade low-res Google URLs for display in the picker
            raw?.replace("=s96-c", "=s400-c")
        } catch (_: Throwable) {
            null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tikoColors.cardBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = tikoColors.textMuted.copy(alpha = 0.45f)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose your avatar",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = tikoColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Default options row ────────────────────────────────────────
            // Shown above the emoji grid. "null" avatarId means one of these
            // two default options is active.
            val isDefault = activeAvatarId == null
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Google photo — only shown for Google/OAuth users that have a photoUrl
                if (googlePhotoUrl != null) {
                    DefaultAvatarItem(
                        label = "Google photo",
                        isSelected = isDefault,
                        onClick = {
                            if (onAvatarSelected != null) onAvatarSelected(null)
                            else { userPrefs.setAvatarId(null); onDismiss() }
                        }
                    ) {
                        // Show actual Google photo in the circle
                        coil.compose.SubcomposeAsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(googlePhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Google photo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            loading = { InitialsCircle(context) },
                            error = { InitialsCircle(context) }
                        )
                    }
                } else {
                    // Email/password or guest user — show "My initials" as the only default
                    DefaultAvatarItem(
                        label = "My initials",
                        isSelected = isDefault,
                        onClick = {
                            if (onAvatarSelected != null) onAvatarSelected(null)
                            else { userPrefs.setAvatarId(null); onDismiss() }
                        }
                    ) {
                        InitialsCircle(context)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Emoji preset grid ──────────────────────────────────────────
            PresetAvatars.chunked(4).forEachIndexed { rowIndex, rowAvatars ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowAvatars.forEach { avatar ->
                        val isSelected = activeAvatarId == avatar.id
                        AvatarGridItem(
                            avatar = avatar,
                            isSelected = isSelected,
                            onClick = {
                                if (onAvatarSelected != null) {
                                    onAvatarSelected(avatar.id)
                                } else {
                                    userPrefs.setAvatarId(avatar.id)
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
                if (rowIndex < 2) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

/** Small circle showing the current user's first initial on yellow, used inside the default option tiles. */
@Composable
private fun InitialsCircle(context: android.content.Context) {
    val name: String = remember(context) {
        try {
            val fb = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            fb?.displayName?.ifBlank { null }
                ?: fb?.email?.substringBefore("@")?.ifBlank { null }
                ?: UserPreferences(context).userName.value
        } catch (_: Throwable) {
            UserPreferences(context).userName.value
        }
    }
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0xFFFFB92E)),
        contentAlignment = Alignment.Center
    ) {
        if (initial.isNotBlank()) {
            Text(
                text = initial,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = Color(0xFF151515)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = Color(0xFF151515),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * A labelled tile used for the "Google photo" and "My initials" default options.
 * [content] is the 64dp circle rendered inside.
 */
@Composable
private fun DefaultAvatarItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag("avatar_option_default_${label.lowercase().replace(' ', '_')}")
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .then(
                    if (isSelected) Modifier.border(3.dp, Color(0xFF6B4EFF), androidx.compose.foundation.shape.CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontFamily = OutfitFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF6B4EFF) else Color(0xFF6B6B6B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AvatarGridItem(
    avatar: AvatarOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .testTag("avatar_option_${avatar.id}")
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, Color(0xFF6B4EFF), CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(avatar.backgroundColor)
                .then(
                    if (avatar.borderColor != null) {
                        Modifier.border(1.dp, avatar.borderColor, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatar.emoji,
                fontSize = 30.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
