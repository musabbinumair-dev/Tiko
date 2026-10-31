package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.local.TikoDatabase
import com.example.data.model.CategoryColors
import com.example.data.model.CategoryIcons
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskEntity
import com.example.data.reminder.NotificationPermissionHelper
import com.example.ui.components.AppDialogController
import com.example.ui.components.AppToastController
import com.example.ui.components.ArrowOutwardIcon
import com.example.ui.components.CreateCategoryDialog
import com.example.ui.components.RoundIconButton
import com.example.ui.components.ToastType
import com.example.ui.components.screenTopPadding
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val AddTaskCream = Color(0xFFEFEBDD)
private val AddTaskBlack = Color(0xFF151515)
private val CardPurple = Color(0xFF6B4EFF)
private val CardLime = Color(0xFFC8DB6A)
private val CardRed = Color(0xFFD9534A)

private fun formatDateForDisplay(dateStr: String): String {
    return try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdfInput.parse(dateStr) ?: Date()
        val todayStr = sdfInput.format(Date())
        val displayMonthDay = SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        if (dateStr == todayStr) {
            "Today, $displayMonthDay"
        } else {
            displayMonthDay
        }
    } catch (e: Exception) {
        dateStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    taskId: String?,
    viewModel: TikoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val existingTask = remember(taskId, tasks) {
        if (taskId != null) tasks.find { it.id == taskId } else null
    }

    var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember(existingTask) { mutableStateOf(existingTask?.notes ?: "") }
    var category by remember(existingTask) { mutableStateOf(existingTask?.category ?: viewModel.preselectedCategoryForNewTask ?: "Work") }
    var priority by remember(existingTask) { mutableStateOf(existingTask?.priority ?: "Medium") }
    var dueDate by remember(existingTask) { mutableStateOf(existingTask?.dueDate ?: TikoDatabase.getTodayDateString()) }
    var dueTime by remember(existingTask) { mutableStateOf(existingTask?.dueTime ?: TaskEntity.DEFAULT_DUE_TIME) }
    var reminder by remember(existingTask) { mutableStateOf(existingTask?.reminder ?: TaskEntity.DEFAULT_REMINDER) }
    val subtasks = remember(existingTask) {
        mutableStateListOf<SubtaskItem>().apply {
            if (existingTask != null) {
                addAll(existingTask.subtasks)
            }
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var showDatePickerOverlay by remember { mutableStateOf(false) }
    var showTimePickerOverlay by remember { mutableStateOf(false) }

    val handleBack = {
        if (title.isNotBlank() && existingTask == null) {
            AppDialogController.confirm(
                title = "Discard task?",
                message = "You have unsaved changes. Are you sure you want to discard them?",
                confirmText = "Discard",
                cancelText = "Keep Editing",
                destructive = true,
                onConfirm = { onBack() }
            )
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    val activity = context as? Activity
    val preferences = viewModel.preferences
    val notifPrefEnabled by preferences.notificationsEnabled.collectAsState()
    val isRealNotificationActive = notifPrefEnabled && NotificationPermissionHelper.hasNotificationPermission(context)

    val executeActualSave = {
        if (title.isBlank()) {
            titleError = true
            AppToastController.show("Please enter a task title", ToastType.Error)
        } else {
            try {
                if (existingTask != null) {
                    viewModel.updateTask(
                        id = existingTask.id,
                        title = title,
                        notes = notes,
                        category = category,
                        priority = priority,
                        dueDate = dueDate,
                        dueTime = dueTime,
                        reminder = reminder,
                        subtasks = subtasks.toList(),
                        done = existingTask.done,
                        onSuccess = {
                            AppToastController.show("Task updated successfully", ToastType.Success)
                        },
                        onError = {
                            viewModel.navigateTo(Screen.ErrorSaveFailed)
                        }
                    )
                } else {
                    viewModel.saveTask(
                        title = title,
                        notes = notes,
                        category = category,
                        priority = priority,
                        dueDate = dueDate,
                        dueTime = dueTime,
                        reminder = reminder,
                        subtasks = subtasks.toList(),
                        onSuccess = {
                            AppToastController.show("Task saved successfully", ToastType.Success)
                        },
                        onError = {
                            viewModel.navigateTo(Screen.ErrorSaveFailed)
                        }
                    )
                }
            } catch (e: Throwable) {
                viewModel.navigateTo(Screen.ErrorSaveFailed)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            preferences.setNotificationsEnabled(true)
            viewModel.rescheduleAllReminders()
            NotificationPermissionHelper.checkAndShowExactAlarmNotice(context, preferences)
        }
        executeActualSave()
    }

    val requestRemindersThenSave = {
        AppDialogController.confirm(
            title = "Allow reminders?",
            message = "Tiko needs notification access to remind you about tasks.",
            confirmText = "Allow",
            cancelText = "Not now",
            onConfirm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (activity != null && NotificationPermissionHelper.isPermanentlyDenied(activity, preferences)) {
                        preferences.setNotificationsEnabled(true)
                        NotificationPermissionHelper.openNotificationSettings(context)
                        executeActualSave()
                    } else {
                        preferences.setHasRequestedNotificationPermission(true)
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
                        preferences.setNotificationsEnabled(true)
                        NotificationPermissionHelper.openNotificationSettings(context)
                    } else {
                        preferences.setNotificationsEnabled(true)
                        viewModel.rescheduleAllReminders()
                        NotificationPermissionHelper.checkAndShowExactAlarmNotice(context, preferences)
                    }
                    executeActualSave()
                }
            },
            onCancel = {
                executeActualSave()
            }
        )
    }

    val performSave = {
        if (title.isBlank()) {
            titleError = true
            AppToastController.show("Please enter a task title", ToastType.Error)
        } else {
            val hasReminder = !reminder.equals("None", ignoreCase = true) && reminder.isNotBlank()
            if (hasReminder && !isRealNotificationActive) {
                requestRemindersThenSave()
            } else {
                executeActualSave()
            }
        }
    }

    LaunchedEffect(viewModel.retrySaveRequested) {
        if (viewModel.retrySaveRequested) {
            viewModel.retrySaveRequested = false
            performSave()
        }
    }

    val tikoColors = com.example.ui.theme.LocalTikoThemeColors.current
    val effectiveDark = isDark || tikoColors.isDark

    val backgroundGradient = Brush.verticalGradient(
        0.0f to tikoColors.gradientStart,
        0.35f to tikoColors.gradientEnd,
        1.0f to tikoColors.gradientEnd
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // 1. Top Row: Back button, centered title "New task", Menu button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .screenTopPadding()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = handleBack,
                    contentDescription = "Back",
                    testTag = "add_task_back_button",
                    isDark = effectiveDark
                )

                Text(
                    text = if (existingTask != null) "Edit task" else "New task",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = tikoColors.textPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existingTask != null) {
                        RoundIconButton(
                            icon = Icons.Outlined.DeleteOutline,
                            onClick = {
                                AppDialogController.confirm(
                                    title = "Delete task?",
                                    message = "Are you sure you want to delete this task? This cannot be undone.",
                                    confirmText = "Delete",
                                    cancelText = "Cancel",
                                    destructive = true,
                                    onConfirm = {
                                        viewModel.deleteTask(existingTask.id)
                                        AppToastController.show("Task deleted", ToastType.Success)
                                        onBack()
                                    }
                                )
                            },
                            contentDescription = "Delete task",
                            testTag = "add_task_top_delete_button",
                            isDark = effectiveDark
                        )
                    }

                    Box {
                        RoundIconButton(
                            icon = Icons.Default.MoreVert,
                            onClick = { showMenu = !showMenu },
                            contentDescription = "Menu",
                            testTag = "add_task_menu_button",
                            isDark = effectiveDark
                        )

                        if (existingTask != null) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(if (effectiveDark) Color(0xFF242424) else Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete task", color = CardRed) },
                                    onClick = {
                                        showMenu = false
                                        AppDialogController.confirm(
                                            title = "Delete task?",
                                            message = "Are you sure you want to delete this task? This cannot be undone.",
                                            confirmText = "Delete",
                                            cancelText = "Cancel",
                                            destructive = true,
                                            onConfirm = {
                                                viewModel.deleteTask(existingTask.id)
                                                AppToastController.show("Task deleted", ToastType.Success)
                                                onBack()
                                            }
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = CardRed
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Heading "WHAT NEEDS DOING?"
            Text(
                text = "WHAT NEEDS\nDOING?",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,
                lineHeight = 44.sp,
                letterSpacing = (-1).sp,
                color = tikoColors.textPrimary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Purple card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardPurple)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pill tag "Task title"
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(if (effectiveDark) Color(0xFF1E1E1E) else Color.White)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Article,
                                contentDescription = null,
                                tint = if (effectiveDark) Color(0xFFF5F1E6) else AddTaskBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Task title",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (effectiveDark) Color(0xFFF5F1E6) else AddTaskBlack
                            )
                        }

                        // Arrow outward circle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (effectiveDark) Color(0xFF1E1E1E) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            ArrowOutwardIcon(size = 20.dp, color = if (effectiveDark) Color(0xFFF5F1E6) else AddTaskBlack)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title input
                    BasicTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        textStyle = TextStyle(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = Color.White
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = "Type your task...",
                                    style = TextStyle(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            if (titleError) {
                Text(
                    text = "Please enter a task title.",
                    color = CardRed,
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Pill label "Category"
            PillLabel(text = "Category", isDark = effectiveDark)

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable row of category cards + "+ New" category button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = category.equals(cat.name, ignoreCase = true)
                    val catColor = Color(cat.colorHex)
                    val textColor = CategoryColors.getContentColor(cat.colorHex)
                    val catIcon = CategoryIcons.getIcon(cat.iconKey)

                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(104.dp)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .border(3.dp, catColor, RoundedCornerShape(28.dp))
                                        .padding(4.dp)
                                } else {
                                    Modifier
                                }
                            )
                            .clip(RoundedCornerShape(if (isSelected) 22.dp else 28.dp))
                            .background(catColor)
                            .clickable { category = cat.name }
                            .testTag("category_${cat.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = cat.name,
                                tint = textColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cat.name,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                // "+ New" category card
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(104.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(tikoColors.cardBg)
                        .border(
                            1.5.dp,
                            if (effectiveDark) tikoColors.cardBorder else AddTaskBlack.copy(alpha = 0.3f),
                            RoundedCornerShape(28.dp)
                        )
                        .clickable { showNewCategoryDialog = true }
                        .testTag("category_add_new"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "New Category",
                            tint = tikoColors.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "New",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = tikoColors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Pill label "Priority"
            PillLabel(text = "Priority", isDark = effectiveDark)

            Spacer(modifier = Modifier.height(10.dp))

            // 3 equal pills: Low, Medium, High
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Low", "Medium", "High").forEach { p ->
                    val isSelected = priority.equals(p, ignoreCase = true)
                    val pBg = if (isSelected) {
                        if (effectiveDark) Color(0xFFF5F1E6) else AddTaskBlack
                    } else {
                        tikoColors.cardBg
                    }
                    val pText = if (isSelected) {
                        if (effectiveDark) Color(0xFF151515) else Color.White
                    } else {
                        tikoColors.textPrimary
                    }
                    val pBorder = if (isSelected) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, if (effectiveDark) tikoColors.cardBorder else AddTaskBlack, CircleShape)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(pBg)
                            .then(pBorder)
                            .clickable { priority = p }
                            .testTag("priority_${p.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = pText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Date & Time Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date card: Lime accent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(CardLime)
                        .clickable { showDatePickerOverlay = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("date_picker_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Date",
                                tint = AddTaskBlack,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Date",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = AddTaskBlack.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = formatDateForDisplay(dueDate),
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AddTaskBlack,
                                    maxLines = 1
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (effectiveDark) Color(0xFF1E1E1E) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (effectiveDark) Color(0xFFF5F1E6) else AddTaskBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Time card: Dark mode styled
                val timeCardBg = if (effectiveDark) Color(0xFF242424) else AddTaskBlack
                val timeCardBorder = if (effectiveDark) Modifier.border(1.dp, Color(0xFF383838), RoundedCornerShape(28.dp)) else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(timeCardBg)
                        .then(timeCardBorder)
                        .clickable { showTimePickerOverlay = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("time_picker_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = "Time",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Time",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = dueTime,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (effectiveDark) Color(0xFF1E1E1E) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (effectiveDark) Color(0xFFF5F1E6) else AddTaskBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7. Pill label "Notes"
            PillLabel(text = "Notes", isDark = effectiveDark)

            Spacer(modifier = Modifier.height(10.dp))

            // Notes card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(tikoColors.cardBg)
                    .then(
                        if (effectiveDark) Modifier.border(1.dp, tikoColors.cardBorder, RoundedCornerShape(28.dp)) else Modifier
                    )
                    .padding(20.dp)
            ) {
                BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    textStyle = TextStyle(
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = tikoColors.textPrimary
                    ),
                    cursorBrush = SolidColor(tikoColors.textPrimary),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("task_notes_input"),
                    decorationBox = { innerTextField ->
                        if (notes.isEmpty()) {
                            Text(
                                text = "Add any additional details...",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = tikoColors.textMuted
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Pill label "Subtasks"
            PillLabel(text = "Subtasks (${subtasks.size})", isDark = effectiveDark)

            Spacer(modifier = Modifier.height(10.dp))

            // Subtasks card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(tikoColors.cardBg)
                    .border(1.dp, tikoColors.cardBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (subtasks.isEmpty()) {
                        Text(
                            text = "No subtasks yet. Add subtasks to break down this task.",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = tikoColors.textMuted
                        )
                    } else {
                        subtasks.forEachIndexed { index, subtask ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Checkbox
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            subtasks[index] = subtask.copy(done = !subtask.done)
                                        }
                                        .then(
                                            if (subtask.done) {
                                                Modifier.background(if (effectiveDark) Color(0xFFF5F1E6) else AddTaskBlack)
                                            } else {
                                                Modifier
                                                    .background(tikoColors.cardBg)
                                                    .border(2.dp, if (effectiveDark) tikoColors.textMuted else AddTaskBlack, CircleShape)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (subtask.done) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Done",
                                            tint = if (effectiveDark) Color(0xFF151515) else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = subtask.title,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 16.sp,
                                    color = if (subtask.done) tikoColors.textMuted else tikoColors.textPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            subtasks[index] = subtask.copy(done = !subtask.done)
                                        }
                                )

                                // Delete subtask button
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                    .clickable {
                                        subtasks.removeAt(index)
                                        AppToastController.show("Subtask removed", ToastType.Info)
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Delete subtask",
                                        tint = Color(0xFFD9534A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (index < subtasks.size - 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(tikoColors.divider)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    // Add Subtask input row inside card
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (effectiveDark) Color(0xFF1E1E1E) else AddTaskCream.copy(alpha = 0.5f))
                            .border(1.dp, tikoColors.cardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = tikoColors.textPrimary
                            ),
                            cursorBrush = SolidColor(tikoColors.textPrimary),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (newSubtaskText.isEmpty()) {
                                    Text(
                                        text = "Add a subtask...",
                                        fontFamily = OutfitFontFamily,
                                        color = tikoColors.textMuted,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        val addBtnBg = if (newSubtaskText.isNotBlank()) {
                            if (effectiveDark) Color(0xFFF5F1E6) else AddTaskBlack
                        } else {
                            if (effectiveDark) Color(0xFFF5F1E6).copy(alpha = 0.2f) else AddTaskBlack.copy(alpha = 0.2f)
                        }
                        val addBtnText = if (effectiveDark) Color(0xFF151515) else Color.White

                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(addBtnBg)
                                .clickable(enabled = newSubtaskText.isNotBlank()) {
                                    subtasks.add(SubtaskItem(title = newSubtaskText.trim(), done = false))
                                    newSubtaskText = ""
                                    AppToastController.show("Subtask added", ToastType.Success)
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = addBtnText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 9. Pill button "Save task" / "Update task"
            val saveBtnBg = if (effectiveDark) Color(0xFFF5F1E6) else AddTaskBlack
            val saveBtnText = if (effectiveDark) Color(0xFF151515) else Color.White

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(saveBtnBg)
                    .clickable(onClick = { performSave() })
                    .testTag("save_task_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (existingTask != null) "Update task" else "Save task",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = saveBtnText
                )
            }

            if (existingTask != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(tikoColors.cardBg)
                        .border(1.5.dp, Color(0xFFD9534A), CircleShape)
                        .clickable {
                            AppDialogController.confirm(
                                title = "Delete task?",
                                message = "Are you sure you want to delete this task? This cannot be undone.",
                                confirmText = "Delete",
                                cancelText = "Cancel",
                                destructive = true,
                                onConfirm = {
                                    viewModel.deleteTask(existingTask.id)
                                    AppToastController.show("Task deleted", ToastType.Success)
                                    onBack()
                                }
                            )
                        }
                        .testTag("delete_task_bottom_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete task",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFD9534A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Dim backdrop — fades in/out independently of the sheet slide animation
        AnimatedVisibility(
            visible = showDatePickerOverlay,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier.fillMaxSize().zIndex(99f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* only drag-down closes; backdrop tap does nothing */ }
            )
        }

        // Custom Foodpanda-style Floating Date Picker Modal (100% Mockup match without dots)
        AnimatedVisibility(
            visible = showDatePickerOverlay,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize().zIndex(100f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                TikoFloatingDatePickerModal(
                    initialDate = dueDate,
                    onDateSelected = { dueDate = it },
                    onDismiss = { showDatePickerOverlay = false },
                    isDark = effectiveDark
                )
            }
        }

        // Custom Foodpanda-style Floating Time Picker Modal (100% Mockup match)
        AnimatedVisibility(
            visible = showTimePickerOverlay,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize().zIndex(100f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showTimePickerOverlay = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                TikoFloatingTimePickerModal(
                    initialTime = dueTime,
                    onTimeSelected = { dueTime = it },
                    onDismiss = { showTimePickerOverlay = false },
                    isDark = effectiveDark
                )
            }
        }

        if (showNewCategoryDialog) {
            CreateCategoryDialog(
                onDismiss = { showNewCategoryDialog = false },
                onCreateCategory = { name, iconKey, colorHex ->
                    viewModel.addCategory(name, iconKey, colorHex)
                    category = name
                    showNewCategoryDialog = false
                    AppToastController.show("Category '$name' created", ToastType.Success)
                },
                isDark = effectiveDark
            )
        }
    }
}

@Composable
private fun PillLabel(text: String, isDark: Boolean = false) {
    val tikoColors = com.example.ui.theme.LocalTikoThemeColors.current
    val effectiveDark = isDark || tikoColors.isDark
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(tikoColors.cardBg)
            .border(1.dp, if (effectiveDark) tikoColors.cardBorder else AddTaskBlack, CircleShape)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = tikoColors.textPrimary
        )
    }
}

private data class TikoCalendarDay(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val dateString: String
)

@Composable
fun TikoFloatingDatePickerModal(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean = false
) {
    var tempSelectedDate by remember(initialDate) { mutableStateOf(initialDate) }

    var displayedCalendar by remember(initialDate) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(initialDate)
            if (d != null) cal.time = d
        } catch (_: Exception) {}
        mutableStateOf(cal)
    }

    val monthTitle = remember(displayedCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(displayedCalendar.time)
    }

    // Build a flat list of 42 cells (6 rows × 7) — always the same count so the
    // card never changes height when switching between 4-week and 6-week months.
    val gridWeeks = remember(displayedCalendar) {
        val weeks = mutableListOf<List<TikoCalendarDay>>()
        val cal = (displayedCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevDaysCount = (firstDayOfWeek + 5) % 7

        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val allCells = mutableListOf<TikoCalendarDay>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (i in (daysInPrevMonth - prevDaysCount + 1)..daysInPrevMonth) {
            val dCal = (prevCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, i) }
            allCells.add(TikoCalendarDay(i, false, sdf.format(dCal.time)))
        }
        for (i in 1..daysInCurrentMonth) {
            val dCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, i) }
            allCells.add(TikoCalendarDay(i, true, sdf.format(dCal.time)))
        }
        // Always pad to exactly 42 cells (6 rows) so height is constant
        val nextCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        var nextDay = 1
        while (allCells.size < 42) {
            val dCal = (nextCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, nextDay++) }
            allCells.add(TikoCalendarDay(nextDay - 1, false, sdf.format(dCal.time)))
        }

        allCells.chunked(7).forEach { weeks.add(it) }
        weeks
    }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    BackHandler { onDismiss() }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { dragOffsetY = 0f },
                    onDragCancel = { dragOffsetY = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0) {
                            dragOffsetY += dragAmount
                            if (dragOffsetY > 120f) onDismiss()
                        } else {
                            dragOffsetY = 0f
                        }
                    }
                )
            }
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(if (isDark) Color(0xFF1F1F1F) else AddTaskCream)
            .then(
                if (isDark) Modifier.border(1.dp, Color(0xFF333333), RoundedCornerShape(32.dp))
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* intercept taps so they don't reach the backdrop */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF484848) else Color(0xFFD4CEBD))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select date",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = if (isDark) Color(0xFFF5F1E6) else AddTaskBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Month navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = {
                        displayedCalendar = (displayedCalendar.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                    },
                    contentDescription = "Previous month",
                    testTag = "date_picker_prev_month",
                    isDark = isDark,
                    modifier = Modifier.size(44.dp)
                )

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF2C2C2C) else Color.White)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = monthTitle,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDark) Color(0xFFF5F1E6) else AddTaskBlack
                    )
                }

                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        displayedCalendar = (displayedCalendar.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                    },
                    contentDescription = "Next month",
                    testTag = "date_picker_next_month",
                    isDark = isDark,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar grid card — fixed height so it never resizes between months
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isDark) Color(0xFF282828) else Color.White)
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Day-of-week header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (isDark) Color(0xFFF5F1E6) else AddTaskBlack,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 6 fixed-height rows — height never changes
                    gridWeeks.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            week.forEach { cell ->
                                val isSelected = cell.dateString == tempSelectedDate
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            tempSelectedDate = cell.dateString
                                            if (!cell.isCurrentMonth) {
                                                try {
                                                    val target = Calendar.getInstance()
                                                    target.time = sdf.parse(cell.dateString) ?: target.time
                                                    displayedCalendar = target
                                                } catch (_: Exception) {}
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDark) Color(0xFFF5F1E6) else AddTaskBlack
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cell.dayNumber.toString(),
                                                color = if (isDark) AddTaskBlack else Color.White,
                                                fontFamily = OutfitFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = cell.dayNumber.toString(),
                                            color = if (cell.isCurrentMonth) {
                                                if (isDark) Color(0xFFF5F1E6) else AddTaskBlack
                                            } else {
                                                if (isDark) Color(0xFF666666) else Color(0xFFB0B0B0)
                                            },
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = if (cell.isCurrentMonth) FontWeight.Bold
                                                         else FontWeight.Medium,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Done button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFFF5F1E6) else AddTaskBlack)
                    .clickable {
                        onDateSelected(tempSelectedDate)
                        onDismiss()
                    }
                    .testTag("date_picker_done_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isDark) AddTaskBlack else Color.White
                )
            }
        }
    }
}
private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other

@Composable
fun TikoAmPmToggle(
    isPm: Boolean,
    onToggle: (Boolean) -> Unit,
    isDark: Boolean = false
) {
    val containerBg = if (isDark) Color(0xFF2B2B2B) else Color(0xFFE5E0CE)
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(44.dp)
            .clip(CircleShape)
            .background(containerBg)
            .padding(4.dp)
    ) {
        val selectedOffset by animateDpAsState(
            targetValue = if (isPm) 86.dp else 0.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
        Box(
            modifier = Modifier
                .offset(x = selectedOffset)
                .width(86.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(if (isDark) Color(0xFFF5F1E6) else AddTaskBlack),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPm) "PM" else "AM",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDark) AddTaskBlack else Color.White
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle(false) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AM",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (!isPm) (if (isDark) AddTaskBlack else Color.White) else (if (isDark) Color(0xFFF5F1E6) else AddTaskBlack)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PM",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isPm) (if (isDark) AddTaskBlack else Color.White) else (if (isDark) Color(0xFFF5F1E6) else AddTaskBlack)
                )
            }
        }
    }
}

@Composable
fun TikoFloatingTimePickerModal(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean = false
) {
    var parsedHour = 9
    var parsedMinute = 0
    var parsedIsPm = false
    try {
        val clean = initialTime.trim()
        val isPmStr = clean.endsWith("PM", ignoreCase = true)
        val isAmStr = clean.endsWith("AM", ignoreCase = true)
        parsedIsPm = isPmStr
        val digitsOnly = clean.replace("(?i)am|pm".toRegex(), "").trim()
        val parts = digitsOnly.split(":")
        if (parts.size >= 2) {
            var h = parts[0].trim().toIntOrNull() ?: 9
            val m = parts[1].trim().toIntOrNull() ?: 0
            if (!isAmStr && !isPmStr) {
                if (h >= 12) {
                    parsedIsPm = true
                    if (h > 12) h -= 12
                } else if (h == 0) {
                    h = 12
                }
            }
            if (h in 1..12) parsedHour = h
            if (m in 0..59) parsedMinute = m
        }
    } catch (_: Exception) {}

    var selectedHour by remember { mutableIntStateOf(parsedHour) }
    var selectedMinute by remember { mutableIntStateOf(parsedMinute) }
    var isPm by remember { mutableStateOf(parsedIsPm) }

    val formattedTime = remember(selectedHour, selectedMinute, isPm) {
        String.format(Locale.US, "%d:%02d %s", selectedHour, selectedMinute, if (isPm) "PM" else "AM")
    }

    var hourDragAccumulator by remember { mutableFloatStateOf(0f) }
    var minuteDragAccumulator by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val stepThreshold = 36f

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0) {
                            dragOffsetY += dragAmount
                            if (dragOffsetY > 100f) {
                                onDismiss()
                            }
                        }
                    }
                )
            }
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(if (isDark) Color(0xFF1F1F1F) else AddTaskCream)
            .then(
                if (isDark) Modifier.border(1.dp, Color(0xFF333333), RoundedCornerShape(32.dp)) else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Intercept tap */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Foodpanda style top drag handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF484848) else Color(0xFFD4CEBD))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title "Select time" centered (no cross icon)
            Text(
                text = "Select time",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = if (isDark) Color(0xFFF5F1E6) else AddTaskBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AM/PM Pill Toggle (Mockup match with smooth animation)
            TikoAmPmToggle(
                isPm = isPm,
                onToggle = { isPm = it },
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(18.dp))

            // White Roller Wheel Card with 5 rows
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isDark) Color(0xFF282828) else Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { hourDragAccumulator = 0f },
                                    onVerticalDrag = { _, dragAmount ->
                                        hourDragAccumulator += dragAmount
                                        val steps = (hourDragAccumulator / stepThreshold).toInt()
                                        if (steps != 0) {
                                            selectedHour = ((selectedHour - 1 - steps).floorMod(12)) + 1
                                            hourDragAccumulator -= steps * stepThreshold
                                        }
                                    }
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf(-2, -1, 0, 1, 2).forEach { offset ->
                            val h = ((selectedHour - 1 + offset).floorMod(12)) + 1
                            if (offset == 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) CardPurple.copy(alpha = 0.35f) else Color(0xFFEBE6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = h.toString(),
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = if (isDark) Color.White else Color(0xFF151515)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            selectedHour = h
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = h.toString(),
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 17.sp,
                                        color = if (isDark) Color(0xFF666666) else Color(0xFFA0A0A0)
                                    )
                                }
                            }
                        }
                    }

                    // Center colon ":"
                    Text(
                        text = ":",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = if (isDark) Color.White else Color(0xFF151515),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    // Minute column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { minuteDragAccumulator = 0f },
                                    onVerticalDrag = { _, dragAmount ->
                                        minuteDragAccumulator += dragAmount
                                        val steps = (minuteDragAccumulator / stepThreshold).toInt()
                                        if (steps != 0) {
                                            selectedMinute = (selectedMinute - steps).floorMod(60)
                                            minuteDragAccumulator -= steps * stepThreshold
                                        }
                                    }
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf(-2, -1, 0, 1, 2).forEach { offset ->
                            val m = (selectedMinute + offset).floorMod(60)
                            val mText = String.format(Locale.US, "%02d", m)
                            if (offset == 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) CardPurple.copy(alpha = 0.35f) else Color(0xFFEBE6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mText,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp,
                                        color = if (isDark) Color.White else Color(0xFF151515)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            selectedMinute = m
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mText,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 17.sp,
                                        color = if (isDark) Color(0xFF666666) else Color(0xFFA0A0A0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons row: [Cancel] [Done]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF2B2B2B) else Color.White)
                        .border(1.dp, if (isDark) Color(0xFF444444) else Color(0xFFD4CEBD), CircleShape)
                        .clickable { onDismiss() }
                        .testTag("time_picker_cancel_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isDark) Color(0xFFF5F1E6) else AddTaskBlack
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(CardPurple)
                        .clickable {
                            onTimeSelected(formattedTime)
                            onDismiss()
                        }
                        .testTag("time_picker_done_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Done",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}


