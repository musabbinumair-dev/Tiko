package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TikoDatabase
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskCategories
import com.example.data.model.TaskPriorities
import com.example.ui.components.NeoButton
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BricolageGrotesque
import com.example.ui.theme.DMSans
import com.example.ui.theme.TikoCoral
import com.example.ui.theme.TikoOutline
import com.example.ui.theme.TikoSurface
import com.example.ui.theme.TikoText
import com.example.ui.viewmodel.TikoViewModel

@Composable
fun AddEditTaskScreen(
    taskId: Long?,
    viewModel: TikoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsState()
    val existingTask = remember(taskId, tasks) {
        if (taskId != null) tasks.find { it.id == taskId } else null
    }

    var title by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember(existingTask) { mutableStateOf(existingTask?.notes ?: "") }
    var category by remember(existingTask) { mutableStateOf(existingTask?.category ?: "Work") }
    var priority by remember(existingTask) { mutableStateOf(existingTask?.priority ?: "Medium") }
    var dueDate by remember(existingTask) { mutableStateOf(existingTask?.dueDate ?: TikoDatabase.getTodayDateString()) }
    var dueTime by remember(existingTask) { mutableStateOf(existingTask?.dueTime ?: "10:00 AM") }
    var reminder by remember(existingTask) { mutableStateOf(existingTask?.reminder ?: "30 min before") }
    val subtasks = remember(existingTask) {
        mutableStateListOf<SubtaskItem>().apply {
            if (existingTask != null) {
                addAll(existingTask.subtasks)
            }
        }
    }
    var newSubtaskInput by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    val quickTimes = listOf("7:00 AM", "10:00 AM", "2:00 PM", "4:00 PM", "6:00 PM", "9:00 PM")
    val reminderOptions = listOf("None", "15 min before", "30 min before", "1 hour before", "1 day before")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TikoText,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = if (existingTask != null) "Edit Task" else "New Task",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = TikoText
                )
            )

            if (existingTask != null) {
                IconButton(
                    onClick = { viewModel.deleteTask(existingTask.id) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TikoCoral,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Title Input
            item {
                Column {
                    Text(
                        text = "Title *",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Bold,
                            color = TikoText
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        placeholder = { Text("What needs to be done?", style = MaterialTheme.typography.bodyMedium) },
                        isError = titleError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        shape = RoundedCornerShape(0.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TikoOutline,
                            unfocusedBorderColor = if (titleError) TikoCoral else TikoOutline,
                            focusedContainerColor = TikoSurface,
                            unfocusedContainerColor = TikoSurface
                        )
                    )
                    if (titleError) {
                        Text(
                            text = "Please enter a task title.",
                            color = TikoCoral,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Category Picker
            item {
                Column {
                    Text(
                        text = "Category (List)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Bold,
                            color = TikoText
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskCategories.ALL.forEach { cat ->
                            val isSelected = category.equals(cat, ignoreCase = true)
                            val catColor = getCategoryColor(cat)

                            Box(
                                modifier = Modifier
                                    .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                                    .background(if (isSelected) TikoOutline else catColor)
                                    .clickable { category = cat }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else TikoText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = DMSans,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else TikoText
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Priority Selector
            item {
                Column {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Bold,
                            color = TikoText
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskPriorities.ALL.forEach { p ->
                            val isSelected = priority.equals(p, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                                    .background(if (isSelected) TikoOutline else TikoSurface)
                                    .clickable { priority = p }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = DMSans,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TikoText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Due Time Quick Options
            item {
                Column {
                    Text(
                        text = "Due Time",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Bold,
                            color = TikoText
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickTimes.forEach { time ->
                            val isSelected = dueTime == time
                            Box(
                                modifier = Modifier
                                    .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                                    .background(if (isSelected) TikoOutline else TikoSurface)
                                    .clickable { dueTime = time }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = DMSans,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TikoText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Reminder Selector
            item {
                Column {
                    Text(
                        text = "Reminder",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Bold,
                            color = TikoText
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reminderOptions.forEach { opt ->
                            val isSelected = reminder == opt
                            Box(
                                modifier = Modifier
                                    .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                                    .background(if (isSelected) TikoOutline else TikoSurface)
                                    .clickable { reminder = opt }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = DMSans,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TikoText
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Notes Multiline Input
            item {
                Column {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Bold,
                            color = TikoText
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Add any details, links, or objectives...", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("task_notes_input"),
                        shape = RoundedCornerShape(0.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TikoOutline,
                            unfocusedBorderColor = TikoOutline,
                            focusedContainerColor = TikoSurface,
                            unfocusedContainerColor = TikoSurface
                        )
                    )
                }
            }

            // Subtasks Builder
            item {
                Column {
                    Text(
                        text = "Subtasks",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Bold,
                            color = TikoText
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Input to add subtask
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubtaskInput,
                            onValueChange = { newSubtaskInput = it },
                            placeholder = { Text("Add subtask item...", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(0.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TikoOutline,
                                unfocusedBorderColor = TikoOutline,
                                focusedContainerColor = TikoSurface,
                                unfocusedContainerColor = TikoSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NeoButton(
                            onClick = {
                                if (newSubtaskInput.isNotBlank()) {
                                    subtasks.add(SubtaskItem(title = newSubtaskInput.trim(), done = false))
                                    newSubtaskInput = ""
                                }
                            },
                            text = "+ Add",
                            isPrimary = true,
                            height = 54.dp
                        )
                    }

                    if (subtasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        ) {
                            subtasks.forEachIndexed { idx, sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(TikoSurface)
                                        .border(
                                            width = if (idx > 0) 1.dp else 0.dp,
                                            color = TikoOutline
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${sub.title}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DMSans),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { subtasks.removeAt(idx) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove subtask",
                                            tint = TikoCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Save Button
        NeoButton(
            onClick = {
                if (title.isBlank()) {
                    titleError = true
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
                    }
                }
            },
            text = if (existingTask != null) "Update Task" else "Save Task",
            isPrimary = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("save_task_button")
        )
    }
}
