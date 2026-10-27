package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryIcons
import com.example.ui.components.AppDialogController
import com.example.ui.components.AppToastController
import com.example.ui.components.RoundIconButton
import com.example.ui.components.ToastType
import com.example.ui.components.screenTopPadding
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel

private val DetailCreamBg = Color(0xFFEFEBDD)
private val DetailBlack = Color(0xFF151515)
private val DetailPurple = Color(0xFF6B4EFF)

@Composable
fun TaskDetailScreen(
    taskId: Long,
    viewModel: TikoViewModel,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val tasks by viewModel.allTasks.collectAsState()
    val task = tasks.find { it.id == taskId }

    var isBookmarked by remember { mutableStateOf(false) }

    // No sample subtasks

    if (task == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DetailCreamBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Task not found",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DetailBlack
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(DetailBlack)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Go back",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
        return
    }

    // Category icon
    val categoryIcon: ImageVector = CategoryIcons.getIcon(task.category)

    // Date display
    val dateDisplay = when {
        task.dueDate.isBlank() || task.dueDate.lowercase().contains("today") -> "Due today"
        else -> task.dueDate
    }
    val timeDisplay = if (task.dueTime.isNotBlank()) task.dueTime else "10:00 AM"
    val priorityDisplay = if (task.priority.isNotBlank()) task.priority else "High"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DetailCreamBg)
    ) {
        // Background gradient: #FFCF4D at the top fading to cream #EFEBDD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFCF4D).copy(alpha = 0.90f),
                            DetailCreamBg
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("task_detail_scroll"),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // TOP ROW: Back button (left) and Actions (Delete + Bookmark) (right)
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
                        onClick = onBack,
                        contentDescription = "Back",
                        testTag = "task_detail_back_button"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                        viewModel.deleteTask(task.id)
                                        AppToastController.show("Task deleted", ToastType.Info)
                                    }
                                )
                            },
                            contentDescription = "Delete task",
                            testTag = "task_detail_delete_button"
                        )

                        RoundIconButton(
                            icon = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            onClick = {
                                isBookmarked = !isBookmarked
                                if (isBookmarked) {
                                    AppToastController.show("Task bookmarked", ToastType.Info)
                                } else {
                                    AppToastController.show("Bookmark removed", ToastType.Info)
                                }
                            },
                            contentDescription = "Bookmark",
                            testTag = "task_detail_bookmark_button"
                        )
                    }
                }
            }

            // CENTERED 64dp PURPLE CIRCLE WITH WHITE BRIEFCASE ICON
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(DetailPurple)
                            .testTag("task_detail_icon"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = task.category,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Task title: Outfit ExtraBold 34sp, centered, black
                    Text(
                        text = task.title,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        color = DetailBlack,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // White pill with category icon + name, Outfit Medium 16sp
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFD6D2C4), CircleShape)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = DetailBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = task.category.ifBlank { "Work" },
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = DetailBlack
                            )
                        }
                    }
                }
            }

            // ROW OF 3 WHITE PILLS: Due date, time, priority
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Due Date Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, DetailBlack, CircleShape)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = DetailBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dateDisplay,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = DetailBlack,
                                maxLines = 1
                            )
                        }
                    }

                    // Time Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, DetailBlack, CircleShape)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = DetailBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeDisplay,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = DetailBlack,
                                maxLines = 1
                            )
                        }
                    }

                    // Priority Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, DetailBlack, CircleShape)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = DetailBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = priorityDisplay,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = DetailBlack,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // SUBTASKS SECTION
            item {
                Spacer(modifier = Modifier.height(20.dp))
                // White pill label "Subtasks" (1dp black outline, Outfit Bold 18sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, DetailBlack, CircleShape)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Subtasks",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DetailBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // White card, 28dp radius, rows separated by thin #E8E8E8 lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (task.subtasks.isEmpty()) {
                            Text(
                                text = "No subtasks yet",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = Color(0xFF8E8E8E)
                            )
                        } else {
                            task.subtasks.forEachIndexed { index, subtask ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                viewModel.toggleSubtask(task.id, subtask.id)
                                            }
                                            .then(
                                                if (subtask.done) {
                                                    Modifier.background(DetailBlack)
                                                } else {
                                                    Modifier
                                                        .background(Color.White)
                                                        .border(2.dp, DetailBlack, CircleShape)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (subtask.done) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Text(
                                        text = subtask.title,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 17.sp,
                                        color = if (subtask.done) Color(0xFF8E8E8E) else DetailBlack,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.toggleSubtask(task.id, subtask.id)
                                            }
                                    )

                                    // Delete subtask button
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                viewModel.removeSubtask(task.id, subtask.id)
                                                AppToastController.show("Subtask deleted", ToastType.Info)
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

                                if (index < task.subtasks.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0xFFE8E8E8))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // Add Subtask input row
                        Spacer(modifier = Modifier.height(16.dp))
                        var newSubtaskInput by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DetailCreamBg.copy(alpha = 0.5f))
                                .border(1.dp, Color(0xFFDCDAD4), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = newSubtaskInput,
                                onValueChange = { newSubtaskInput = it },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = DetailBlack
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (newSubtaskInput.isEmpty()) {
                                        Text(
                                            text = "Add subtask...",
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
                                    .background(if (newSubtaskInput.isNotBlank()) DetailBlack else DetailBlack.copy(alpha = 0.2f))
                                    .clickable(enabled = newSubtaskInput.isNotBlank()) {
                                        viewModel.addSubtask(task.id, newSubtaskInput.trim())
                                        AppToastController.show("Subtask added", ToastType.Success)
                                        newSubtaskInput = ""
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
            }

            // NOTES SECTION
            item {
                Spacer(modifier = Modifier.height(20.dp))
                // White pill label "Notes"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, DetailBlack, CircleShape)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Notes",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DetailBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // White card, 28dp radius, with notes text or gray hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = if (task.notes.isNotBlank()) task.notes else "No notes",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = if (task.notes.isNotBlank()) Color(0xFF3A3A3A) else Color(0xFF8E8E8E),
                        lineHeight = 24.sp
                    )
                }
            }

            // FULL-WIDTH BLACK PILL BUTTON "Mark as done"
            item {
                Spacer(modifier = Modifier.height(26.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(DetailBlack)
                        .clickable {
                            val willBeDone = !task.done
                            viewModel.toggleTaskDone(task)
                            if (willBeDone) {
                                AppToastController.show("Task completed!", ToastType.Success)
                            } else {
                                AppToastController.show("Task marked as pending", ToastType.Info)
                            }
                        }
                        .testTag("task_detail_toggle_done"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (task.done) "Mark as pending" else "Mark as done",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            // FULL-WIDTH DELETE BUTTON
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
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
                                    viewModel.deleteTask(task.id)
                                    AppToastController.show("Task deleted", ToastType.Info)
                                }
                            )
                        }
                        .testTag("task_detail_bottom_delete_button"),
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
        }
    }
}
