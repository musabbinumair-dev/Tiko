package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.zIndex
import com.example.ui.components.AppDialogController
import com.example.ui.components.AppToastController
import com.example.ui.components.ToastType
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TikoDatabase
import com.example.data.model.CategoryColors
import com.example.data.model.CategoryIcons
import com.example.data.model.CategoryModel
import com.example.data.model.SubtaskItem
import com.example.ui.components.ArrowOutwardIcon
import com.example.ui.components.CreateCategoryDialog
import com.example.ui.components.RoundIconButton
import com.example.ui.components.screenTopPadding
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.TikoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val AddTaskYellow = Color(0xFFFFCF4D)
private val AddTaskCream = Color(0xFFEFEBDD)
private val AddTaskBlack = Color(0xFF151515)
private val CardPurple = Color(0xFF6B4EFF)
private val CardYellow = Color(0xFFFFB92E)
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
    taskId: Long?,
    viewModel: TikoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val existingTask = remember(taskId, tasks) {
        if (taskId != null) tasks.find { it.id == taskId } else null
    }

    var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember(existingTask) { mutableStateOf(existingTask?.notes ?: "") }
    var category by remember(existingTask) { mutableStateOf(existingTask?.category ?: "Work") }
    var priority by remember(existingTask) { mutableStateOf(existingTask?.priority ?: "Medium") }
    var dueDate by remember(existingTask) { mutableStateOf(existingTask?.dueDate ?: TikoDatabase.getTodayDateString()) }
    var dueTime by remember(existingTask) { mutableStateOf(existingTask?.dueTime ?: "9:00 AM") }
    var reminder by remember(existingTask) { mutableStateOf(existingTask?.reminder ?: "30 min before") }
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

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.parse(dueDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    )

    val timePickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = false
    )

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

    val backgroundGradient = Brush.verticalGradient(
        0.0f to AddTaskYellow,
        0.35f to AddTaskCream,
        1.0f to AddTaskCream
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
                    testTag = "add_task_back_button"
                )

                // Centered title "New task" Outfit Bold 20sp
                Text(
                    text = if (existingTask != null) "Edit task" else "New task",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AddTaskBlack
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
                            testTag = "add_task_top_delete_button"
                        )
                    }

                    // Menu button (three dots): 48dp white circle with 1dp #151515 outline
                    Box {
                        RoundIconButton(
                            icon = Icons.Default.MoreVert,
                            onClick = { showMenu = !showMenu },
                            contentDescription = "Menu",
                            testTag = "add_task_menu_button"
                        )

                        if (existingTask != null) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
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
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Heading "WHAT NEEDS DOING?" Outfit Black 44sp, uppercase, black, letter spacing -1sp, 2 lines
            Text(
                text = "WHAT NEEDS\nDOING?",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,
                lineHeight = 44.sp,
                letterSpacing = (-1).sp,
                color = AddTaskBlack
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Purple #6B4EFF card, 28dp radius, 16dp padding
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
                        // White pill tag "Task title" (32dp high, document icon, Outfit Medium 14sp)
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Article,
                                contentDescription = null,
                                tint = AddTaskBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Task title",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = AddTaskBlack
                            )
                        }

                        // 44dp white circle with arrow_outward icon at top right
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            ArrowOutwardIcon(size = 20.dp, color = AddTaskBlack)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title input below in Outfit Bold 28sp, white text, white placeholder "Type your task..."
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

            // 4. White pill label "Category" (1dp black outline, Outfit Bold 18sp)
            PillLabel(text = "Category")

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
                        .background(Color.White)
                        .border(1.5.dp, AddTaskBlack.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
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
                            tint = AddTaskBlack,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "New",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AddTaskBlack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. White pill label "Priority"
            PillLabel(text = "Priority")

            Spacer(modifier = Modifier.height(10.dp))

            // 3 equal pills, 48dp high, Outfit Medium 17sp: Low and Medium (white fill, 1dp black outline), selected one has black fill and white text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Low", "Medium", "High").forEach { p ->
                    val isSelected = priority.equals(p, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AddTaskBlack else Color.White)
                            .then(
                                if (!isSelected) Modifier.border(1.dp, AddTaskBlack, CircleShape) else Modifier
                            )
                            .clickable { priority = p }
                            .testTag("priority_${p.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = if (isSelected) Color.White else AddTaskBlack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Two cards side by side with 8dp gap, 28dp radius: Date (lime, black text) and Time (black, white text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date card: Lime, black text, icon left, label + value, 36dp white circle with right arrow
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
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = AddTaskBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Time card: Black, white text, icon left, label + value, 36dp white circle with right arrow
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(AddTaskBlack)
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
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = AddTaskBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7. White pill label "Notes" placed ABOVE the card (not overlapping it)
            PillLabel(text = "Notes")

            Spacer(modifier = Modifier.height(10.dp))

            // White card, 28dp radius, 120dp high, gray placeholder "Add any additional details..."
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    textStyle = TextStyle(
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = AddTaskBlack
                    ),
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
                                color = Color(0xFF8E8E93)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. White pill label "Subtasks"
            PillLabel(text = "Subtasks (${subtasks.size})")

            Spacer(modifier = Modifier.height(10.dp))

            // Subtasks card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFD6D2C4), RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (subtasks.isEmpty()) {
                        Text(
                            text = "No subtasks yet. Add subtasks to break down this task.",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = Color(0xFF8E8E93)
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
                                                Modifier.background(AddTaskBlack)
                                            } else {
                                                Modifier
                                                    .background(Color.White)
                                                    .border(2.dp, AddTaskBlack, CircleShape)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (subtask.done) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Done",
                                            tint = Color.White,
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
                                    color = if (subtask.done) Color(0xFF8E8E93) else AddTaskBlack,
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
                                        .background(Color(0xFFE8E8E8))
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
                            .background(AddTaskCream.copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFFDCDAD4), RoundedCornerShape(16.dp))
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
                                color = AddTaskBlack
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (newSubtaskText.isEmpty()) {
                                    Text(
                                        text = "Add a subtask...",
                                        fontFamily = OutfitFontFamily,
                                        color = Color(0xFF8E8E8E),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(if (newSubtaskText.isNotBlank()) AddTaskBlack else AddTaskBlack.copy(alpha = 0.2f))
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
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 9. Full-width black pill button "Save task" / "Update task", 56dp high, Outfit Bold 18sp, white text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(AddTaskBlack)
                    .clickable {
                        if (title.isBlank()) {
                            titleError = true
                            AppToastController.show("Please enter a task title", ToastType.Error)
                        } else {
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
                                    done = existingTask.done
                                )
                                AppToastController.show("Task updated successfully", ToastType.Success)
                            } else {
                                viewModel.saveTask(
                                    title = title,
                                    notes = notes,
                                    category = category,
                                    priority = priority,
                                    dueDate = dueDate,
                                    dueTime = dueTime,
                                    reminder = reminder,
                                    subtasks = subtasks.toList()
                                )
                                AppToastController.show("Task saved successfully", ToastType.Success)
                            }
                            onBack()
                        }
                    }
                    .testTag("save_task_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (existingTask != null) "Update task" else "Save task",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            if (existingTask != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(Color.White)
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

        // Custom Date Picker Modal
        if (showDatePickerOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showDatePickerOverlay = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                BackHandler { showDatePickerOverlay = false }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .border(1.dp, AddTaskBlack.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume clicks */ }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DatePicker(
                            state = datePickerState,
                            colors = DatePickerDefaults.colors(
                                containerColor = Color.White,
                                titleContentColor = AddTaskBlack,
                                headlineContentColor = AddTaskBlack,
                                selectedDayContainerColor = AddTaskBlack,
                                selectedDayContentColor = Color.White,
                                todayDateBorderColor = CardPurple,
                                todayContentColor = CardPurple
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, AddTaskBlack, CircleShape)
                                    .clickable { showDatePickerOverlay = false }
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AddTaskBlack
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(AddTaskBlack)
                                    .clickable {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            val cal = Calendar.getInstance()
                                            cal.timeInMillis = millis
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            dueDate = sdf.format(cal.time)
                                        }
                                        showDatePickerOverlay = false
                                    }
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Confirm",
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

        // Custom Time Picker Modal
        if (showTimePickerOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showTimePickerOverlay = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                BackHandler { showTimePickerOverlay = false }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .border(1.dp, AddTaskBlack.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume clicks */ }
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SELECT TIME",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = AddTaskBlack
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = Color(0xFFF6F6F6),
                                selectorColor = CardPurple,
                                containerColor = Color.White,
                                timeSelectorSelectedContainerColor = AddTaskBlack,
                                timeSelectorSelectedContentColor = Color.White,
                                timeSelectorUnselectedContainerColor = Color(0xFFF1EFEA),
                                timeSelectorUnselectedContentColor = AddTaskBlack
                            )
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, AddTaskBlack, CircleShape)
                                    .clickable { showTimePickerOverlay = false }
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AddTaskBlack
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(AddTaskBlack)
                                    .clickable {
                                        val hour = timePickerState.hour
                                        val minute = timePickerState.minute
                                        val isPm = hour >= 12
                                        val displayHour = when {
                                            hour == 0 -> 12
                                            hour > 12 -> hour - 12
                                            else -> hour
                                        }
                                        val amPm = if (isPm) "PM" else "AM"
                                        dueTime = String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm)
                                        showTimePickerOverlay = false
                                    }
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Confirm",
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

        if (showNewCategoryDialog) {
            CreateCategoryDialog(
                onDismiss = { showNewCategoryDialog = false },
                onCreateCategory = { name, iconKey, colorHex ->
                    viewModel.addCategory(name, iconKey, colorHex)
                    category = name
                    showNewCategoryDialog = false
                    AppToastController.show("Category '$name' created", ToastType.Success)
                }
            )
        }
    }
}

@Composable
private fun PillLabel(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, AddTaskBlack, CircleShape)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = AddTaskBlack
        )
    }
}
