package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCheckbox
import com.example.ui.components.NeoDoodleGrid
import com.example.ui.components.NeoDoodleQuarterCircle
import com.example.ui.components.NeoDoodleSquiggle
import com.example.ui.components.NeoDoodleTriangle
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BricolageGrotesque
import com.example.ui.theme.DMSans
import com.example.ui.theme.TikoCoral
import com.example.ui.theme.TikoMint
import com.example.ui.theme.TikoMustard
import com.example.ui.theme.TikoOutline
import com.example.ui.theme.TikoPeach
import com.example.ui.theme.TikoSurface
import com.example.ui.theme.TikoText
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel

@Composable
fun TaskDetailScreen(
    taskId: Long,
    viewModel: TikoViewModel,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsState()
    val task = tasks.find { it.id == taskId }

    var showMenu by remember { mutableStateOf(false) }
    var newSubtaskText by remember { mutableStateOf("") }
    var isAddingSubtask by remember { mutableStateOf(false) }

    if (task == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Task not found", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                NeoButton(onClick = onBack, text = "Go back", isPrimary = true)
            }
        }
        return
    }

    val categoryColor = getCategoryColor(task.category)
    val categoryIcon = getCategoryIcon(task.category)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Top App Bar: Back Button & Overflow Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TikoText,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(TikoOutline, CircleShape)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(TikoSurface)
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Task", style = MaterialTheme.typography.titleSmall) },
                        onClick = {
                            showMenu = false
                            onNavigate(Screen.EditTask(task.id))
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete Task",
                                style = MaterialTheme.typography.titleSmall.copy(color = TikoCoral)
                            )
                        },
                        onClick = {
                            showMenu = false
                            viewModel.deleteTask(task.id)
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Hero Task Banner Card (Mustard Yellow in Mockup)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(categoryColor)
                        .padding(18.dp)
                ) {
                    // Decorative neo-brutalist doodles
                    NeoDoodleGrid(
                        modifier = Modifier
                            .size(70.dp)
                            .align(Alignment.TopEnd)
                    )
                    NeoDoodleTriangle(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        // Category Icon box
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = task.category,
                            tint = TikoText,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Title
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                                color = TikoText
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtitle
                        Text(
                            text = "${task.category} • Due today, ${task.dueTime}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFC7432D) // Accent dark coral text matching mockup
                            )
                        )
                    }
                }
            }

            item {
                // Three Metric Boxes in a row (Priority, Reminder, Status)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                ) {
                    // Priority Box (Coral)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(TikoCoral)
                            .border(1.dp, TikoOutline)
                            .padding(12.dp)
                    ) {
                        NeoDoodleTriangle(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                        )
                        Column {
                            Text(
                                text = "Priority",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = DMSans,
                                    fontSize = 13.sp,
                                    color = TikoText
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.priority,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = BricolageGrotesque,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = TikoText
                                )
                            )
                        }
                    }

                    // Reminder Box (Mint)
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .background(TikoMint)
                            .border(1.dp, TikoOutline)
                            .padding(12.dp)
                    ) {
                        NeoDoodleSquiggle(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                        )
                        Column {
                            Text(
                                text = "Reminder",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = DMSans,
                                    fontSize = 13.sp,
                                    color = TikoText
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.reminder,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = BricolageGrotesque,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = TikoText
                                )
                            )
                        }
                    }

                    // Status Box (Peach)
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .background(TikoPeach)
                            .border(1.dp, TikoOutline)
                            .padding(12.dp)
                    ) {
                        NeoDoodleQuarterCircle(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                        )
                        Column {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = DMSans,
                                    fontSize = 13.sp,
                                    color = TikoText
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (task.done) "Completed" else "In progress",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = BricolageGrotesque,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = TikoText
                                )
                            )
                        }
                    }
                }
            }

            item {
                // Notes Section
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = TikoText
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(TikoSurface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (task.notes.isNotBlank()) task.notes else "No notes added for this task.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = DMSans,
                            lineHeight = 22.sp,
                            color = TikoText
                        )
                    )
                }
            }

            item {
                // Subtasks Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subtasks",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = TikoText
                        )
                    )

                    Text(
                        text = "+ Add",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Bold,
                            color = TikoText
                        ),
                        modifier = Modifier
                            .clickable { isAddingSubtask = true }
                            .padding(4.dp)
                    )
                }

                // Add Subtask Input Field (if active)
                if (isAddingSubtask) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = { Text("Subtask description...", style = MaterialTheme.typography.bodyMedium) },
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
                        IconButton(
                            onClick = {
                                if (newSubtaskText.isNotBlank()) {
                                    viewModel.addSubtask(task.id, newSubtaskText)
                                    newSubtaskText = ""
                                    isAddingSubtask = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add subtask", tint = TikoText)
                        }
                        IconButton(
                            onClick = { isAddingSubtask = false }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TikoCoral)
                        }
                    }
                }

                // Subtask Items List Box
                if (task.subtasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                            .background(TikoSurface)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "No subtasks yet. Tap '+ Add' to break this task down.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DMSans)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                    ) {
                        task.subtasks.forEachIndexed { index, subtask ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(TikoSurface)
                                    .border(
                                        width = if (index > 0) 1.dp else 0.dp,
                                        color = TikoOutline
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeoCheckbox(
                                    checked = subtask.done,
                                    onCheckedChange = { viewModel.toggleSubtask(task.id, subtask.id) }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = subtask.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = DMSans,
                                        fontWeight = FontWeight.Medium,
                                        color = if (subtask.done) TikoText.copy(alpha = 0.5f) else TikoText,
                                        textDecoration = if (subtask.done) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeSubtask(task.id, subtask.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove subtask",
                                        tint = TikoText.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Buttons: Edit (white) and Mark done / Mark in progress (black)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoButton(
                onClick = { onNavigate(Screen.EditTask(task.id)) },
                text = "Edit",
                isPrimary = false,
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_task_button")
            )

            NeoButton(
                onClick = { viewModel.toggleTaskDone(task) },
                text = if (task.done) "Mark pending" else "Mark done",
                isPrimary = true,
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("toggle_done_button")
            )
        }
    }
}
