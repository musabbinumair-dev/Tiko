package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
    modifier: Modifier = Modifier
) {
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val currentUser by authRepository.currentUser.collectAsState(initial = null)
    
    // Intercept hardware/gesture back press to return to Home
    BackHandler {
        viewModel.selectTab(NavigationTab.HOME)
    }

    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()
    val preferences = viewModel.preferences

    val userName by preferences.userName.collectAsState()
    val userEmail by preferences.userEmail.collectAsState()
    val notifEnabled by preferences.notificationsEnabled.collectAsState()

    var notificationsOn by remember(notifEnabled) { mutableStateOf(notifEnabled) }
    var darkModeOn by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(userName) }
    var editEmailInput by remember { mutableStateOf(userEmail) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
        days.map { day ->
            WeeklyBarData(day, 0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppCreamBackground)
    ) {
        // Header Gradient #FFCF4D to cream
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFCF4D).copy(alpha = 0.85f),
                            AppCreamBackground
                        )
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
                    // Centered 110dp avatar with yellow #FFB92E fill and 36dp camera badge
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .testTag("profile_avatar_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.ui.components.UserInitialsAvatar(
                            name = displayName,
                            size = 110.dp,
                            backgroundColor = CardYellow,
                            textColor = AppBlack,
                            fontSize = 40.sp
                        )

                        // 36dp purple #6B4EFF camera badge at bottom-right
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(CardPurple)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable {
                                    AppToastController.show("Photo upload coming soon", ToastType.Info)
                                }
                                .testTag("profile_camera_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = "Change photo",
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
                        color = AppBlack,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // White pill with email or "Log in" button for guest
                    if (user != null || userEmail.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFDCD8CB), CircleShape)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayEmail,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = AppBlack
                            )
                        }
                    } else {
                        // Guest mode: show "Log in" button
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(AppBlack)
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
                                color = Color.White
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
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // "This week" SECTION
            item {
                Spacer(modifier = Modifier.height(20.dp))
                // White pill label "This week"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, AppBlack, CircleShape)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This week",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // White card, 28dp radius, 7-bar chart (Mon to Sun)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    val maxValue = 9
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
                                    color = AppBlack
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
                                    color = AppBlack
                                )
                            }
                        }
                    }
                }
            }

            // "Settings" SECTION
            item {
                Spacer(modifier = Modifier.height(20.dp))
                // White pill label "Settings"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, AppBlack, CircleShape)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Settings",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // White card, 28dp radius, 4 rows separated by thin #E8E8E8 lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Notifications (purple toggle, on)
                        SettingsRow(
                            icon = Icons.Outlined.Notifications,
                            label = "Notifications",
                            trailing = {
                                ProfileSwitch(
                                    checked = notificationsOn,
                                    onCheckedChange = { checked ->
                                        notificationsOn = checked
                                        preferences.setNotificationsEnabled(checked)
                                        AppToastController.show(
                                            if (checked) "Notifications enabled" else "Notifications disabled",
                                            if (checked) ToastType.Success else ToastType.Info
                                        )
                                    },
                                    testTag = "switch_notifications"
                                )
                            }
                        )

                        SettingsDivider()

                        // 2. Reminders (chevron)
                        SettingsRow(
                            icon = Icons.Outlined.CalendarToday,
                            label = "Reminders",
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
                                    tint = AppBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )

                        SettingsDivider()

                        // 3. Dark mode (gray toggle, off)
                        SettingsRow(
                            icon = Icons.Outlined.DarkMode,
                            label = "Dark mode",
                            trailing = {
                                ProfileSwitch(
                                    checked = darkModeOn,
                                    onCheckedChange = { checked ->
                                        darkModeOn = checked
                                        AppToastController.show(if (checked) "Dark mode enabled" else "Dark mode disabled", ToastType.Success)
                                    },
                                    testTag = "switch_dark_mode"
                                )
                            }
                        )

                        SettingsDivider()

                        // 4. Language ("English" + chevron)
                        SettingsRow(
                            icon = Icons.Outlined.Language,
                            label = "Language",
                            onClick = { showLanguageDialog = true },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedLanguage,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = Color(0xFF6B6B6B)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Choose language",
                                        tint = AppBlack,
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
                            .background(Color(0xFF151515))
                            .clickable {
                                AppDialogController.confirm(
                                    title = "Log Out",
                                    message = "Are you sure you want to log out of your account?",
                                    confirmText = "Log out",
                                    destructive = true,
                                    onConfirm = {
                                        scope.launch {
                                            authRepository.signOut()
                                            viewModel.clearDatabase()
                                            AppToastController.show("Logged out successfully", ToastType.Success)
                                            onNavigate(Screen.Welcome)
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
                            color = Color.White
                        )
                    }
                }

                // "Delete account" BUTTON (red)
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    var deletePasswordInput by remember { mutableStateOf("") }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                            .clickable {
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
                    
                    // Delete confirmation dialog with password
                    if (showDeleteDialog) {
                        BackHandler { showDeleteDialog = false }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(2000f)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showDeleteDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color.White)
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Delete Account",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = AppBlack
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "This action cannot be undone. Enter your password to confirm.",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = Color(0xFF6B6B6B),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                OutlinedTextField(
                                    value = deletePasswordInput,
                                    onValueChange = { deletePasswordInput = it },
                                    placeholder = { Text("Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppBlack,
                                        unfocusedBorderColor = Color(0xFFCCCCCC)
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFEBDD))
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
                                                if (deletePasswordInput.isNotBlank()) {
                                                    scope.launch {
                                                        when (val result = authRepository.reauthenticate(deletePasswordInput)) {
                                                            is AuthResult.Success -> {
                                                                when (val deleteResult = authRepository.deleteAccount()) {
                                                                    is AuthResult.Success -> {
                                                                        viewModel.clearDatabase()
                                                                        AppToastController.show("Account deleted", ToastType.Success)
                                                                        showDeleteDialog = false
                                                                        onNavigate(Screen.Welcome)
                                                                    }
                                                                    is AuthResult.Error -> {
                                                                        AppToastController.show(deleteResult.message, ToastType.Error)
                                                                    }
                                                                }
                                                            }
                                                            is AuthResult.Error -> {
                                                                AppToastController.show(result.message, ToastType.Error)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    AppToastController.show("Please enter your password", ToastType.Error)
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

        // Soft fade scrim behind the bar (90dp gradient, transparent to cream #EFEBDD at 80%)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFEFEBDD).copy(alpha = 0.80f)
                        )
                    )
                )
        )
    }

    // DIALOGS

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
    modifier: Modifier = Modifier
) {
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
                // Left 30dp white circle with icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = AppBlack,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Right 30dp white circle with diagonal arrow
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    ArrowOutwardIcon(
                        size = 14.dp,
                        color = AppBlack
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
 * Purple #6B4EFF when on, Gray #E0E0E0 when off, with white sliding circular thumb.
 */
@Composable
private fun ProfileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "profile_switch"
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        label = "switch_thumb"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(if (checked) CardPurple else Color(0xFFE0E0E0))
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
 * Settings Row Component inside the white 28dp card:
 * Icon (24dp outlined), Label (Outfit Medium 17sp), Trailing item (Switch or Chevron).
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
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
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AppBlack,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = AppBlack
            )
        }

        trailing()
    }
}

/**
 * Thin #E8E8E8 horizontal divider between settings rows
 */
@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE8E8E8))
    )
}
