package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TaskCategories
import com.example.data.model.TaskPriorities
import com.example.ui.components.NeoButton
import com.example.ui.theme.BricolageGrotesque
import com.example.ui.theme.DMSans
import com.example.ui.theme.TikoCardFill
import com.example.ui.theme.TikoCoral
import com.example.ui.theme.TikoGreen
import com.example.ui.theme.TikoMustard
import com.example.ui.theme.TikoOutline
import com.example.ui.theme.TikoPeach
import com.example.ui.theme.TikoText
import com.example.ui.viewmodel.TikoViewModel

@Composable
fun SettingsScreen(
    viewModel: TikoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()
    val preferences = viewModel.preferences

    val userName by preferences.userName.collectAsState()
    val userEmail by preferences.userEmail.collectAsState()
    val notifEnabled by preferences.notificationsEnabled.collectAsState()
    val soundHapticsEnabled by preferences.soundHapticsEnabled.collectAsState()
    val dailyBriefingEnabled by preferences.dailyBriefingEnabled.collectAsState()
    val defaultPriority by preferences.defaultPriority.collectAsState()
    val defaultCategory by preferences.defaultCategory.collectAsState()
    val defaultReminder by preferences.defaultReminder.collectAsState()
    val firstDayOfWeek by preferences.firstDayOfWeek.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(userName) }

    var showClearCompletedDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showResetSampleDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showDayOfWeekDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Screen Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                color = TikoText
            ),
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        Text(
            text = "Account, preferences & offline storage",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = DMSans,
                color = TikoText.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. User Profile Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(TikoCardFill)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar with circular 2dp border
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, TikoOutline, CircleShape)
                                .background(TikoMustard),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.tiko_avatar),
                                contentDescription = "Profile Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = BricolageGrotesque,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = TikoText
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .border(1.5.dp, TikoOutline, RoundedCornerShape(0.dp))
                                        .background(TikoGreen)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LOCAL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = DMSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = TikoText
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = DMSans,
                                    color = TikoText.copy(alpha = 0.7f)
                                )
                            )
                        }

                        IconButton(
                            onClick = {
                                editNameInput = userName
                                showEditProfileDialog = true
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = TikoText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Notifications & Alerts Section
            item {
                SettingsSectionHeader(title = "Notifications & Sound", icon = Icons.Outlined.Notifications)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(TikoCardFill)
                ) {
                    SettingToggleRow(
                        title = "Task Reminders",
                        subtitle = "Notify when a task reaches its scheduled due time",
                        checked = notifEnabled,
                        onCheckedChange = { preferences.setNotificationsEnabled(it) }
                    )
                    SettingDivider()
                    SettingToggleRow(
                        title = "Haptic & Vibration",
                        subtitle = "Vibrate device when checking off completed tasks",
                        checked = soundHapticsEnabled,
                        onCheckedChange = { preferences.setSoundHapticsEnabled(it) }
                    )
                    SettingDivider()
                    SettingToggleRow(
                        title = "Morning Briefing",
                        subtitle = "Daily summary notification at 8:00 AM with today's tasks",
                        checked = dailyBriefingEnabled,
                        onCheckedChange = { preferences.setDailyBriefingEnabled(it) }
                    )
                    SettingDivider()
                    SettingOptionRow(
                        title = "Default Reminder",
                        value = defaultReminder,
                        onClick = { showReminderDialog = true }
                    )
                }
            }

            // 3. Task & Workflow Defaults
            item {
                SettingsSectionHeader(title = "Task Preferences", icon = Icons.Outlined.Tune)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(TikoCardFill)
                ) {
                    SettingOptionRow(
                        title = "Default Priority",
                        value = defaultPriority,
                        onClick = { showPriorityDialog = true }
                    )
                    SettingDivider()
                    SettingOptionRow(
                        title = "Default List",
                        value = defaultCategory,
                        onClick = { showCategoryDialog = true }
                    )
                    SettingDivider()
                    SettingOptionRow(
                        title = "First Day of Week",
                        value = firstDayOfWeek,
                        onClick = { showDayOfWeekDialog = true }
                    )
                }
            }

            // 4. Data Management & Backup
            item {
                SettingsSectionHeader(title = "Data Management", icon = Icons.Outlined.Restore)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(TikoCardFill)
                ) {
                    // Export Tasks
                    SettingActionRow(
                        title = "Export Tasks Backup",
                        subtitle = "Share, save, or copy tasks as readable backup text",
                        icon = Icons.Outlined.Share,
                        iconTint = TikoMustard,
                        onClick = {
                            val backup = viewModel.exportTasksFormatted()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, backup)
                                putExtra(Intent.EXTRA_SUBJECT, "Tiko Tasks Backup")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Export Tiko Tasks")
                            context.startActivity(shareIntent)
                        }
                    )
                    SettingDivider()
                    // Clear Completed Tasks
                    SettingActionRow(
                        title = "Clear Completed Tasks",
                        subtitle = "Delete all checked tasks to free up space",
                        icon = Icons.Outlined.CleaningServices,
                        iconTint = TikoGreen,
                        onClick = { showClearCompletedDialog = true }
                    )
                    SettingDivider()
                    // Reset to Sample Data
                    SettingActionRow(
                        title = "Restore Default Sample Data",
                        subtitle = "Reset database to the initial mockup task dataset",
                        icon = Icons.Outlined.Restore,
                        iconTint = TikoCoral,
                        onClick = { showResetSampleDialog = true }
                    )
                    SettingDivider()
                    // Clear All Tasks
                    SettingActionRow(
                        title = "Wipe All Data",
                        subtitle = "Permanently remove all tasks and subtasks from device",
                        icon = Icons.Outlined.DeleteForever,
                        iconTint = TikoCoral,
                        onClick = { showClearAllDialog = true }
                    )
                }
            }

            // 5. About & Privacy
            item {
                SettingsSectionHeader(title = "About Tiko", icon = Icons.Outlined.Info)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(TikoCardFill)
                ) {
                    SettingOptionRow(
                        title = "App Version",
                        value = "1.0.0 (Build 1)",
                        onClick = {}
                    )
                    SettingDivider()
                    SettingOptionRow(
                        title = "Database Engine",
                        value = "SQLite Room (Offline)",
                        onClick = {}
                    )
                    SettingDivider()
                    SettingActionRow(
                        title = "Privacy & Data Storage",
                        subtitle = "100% on-device local storage. Zero analytics, zero cloud tracking.",
                        icon = Icons.Outlined.Info,
                        iconTint = TikoText,
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // Edit Name Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    "Edit Profile Name",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = BricolageGrotesque)
                )
            },
            text = {
                OutlinedTextField(
                    value = editNameInput,
                    onValueChange = { editNameInput = it },
                    singleLine = true,
                    shape = RoundedCornerShape(0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TikoOutline,
                        unfocusedBorderColor = TikoOutline,
                        focusedContainerColor = TikoCardFill,
                        unfocusedContainerColor = TikoCardFill
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editNameInput.isNotBlank()) {
                            preferences.setUserName(editNameInput.trim())
                        }
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = TikoOutline)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TikoOutline)
                }
            },
            shape = RoundedCornerShape(0.dp),
            containerColor = TikoCardFill
        )
    }

    // Clear Completed Dialog
    if (showClearCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCompletedDialog = false },
            title = {
                Text(
                    "Clear Completed Tasks?",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = BricolageGrotesque)
                )
            },
            text = {
                Text(
                    "This will permanently delete all finished tasks from your database.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DMSans)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCompletedTasks { count ->
                            Toast.makeText(context, "$count completed task(s) cleared", Toast.LENGTH_SHORT).show()
                        }
                        showClearCompletedDialog = false
                    }
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold, color = TikoCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedDialog = false }) {
                    Text("Cancel", color = TikoOutline)
                }
            },
            shape = RoundedCornerShape(0.dp),
            containerColor = TikoCardFill
        )
    }

    // Wipe All Data Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    "Wipe All Tasks?",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = BricolageGrotesque, color = TikoCoral)
                )
            },
            text = {
                Text(
                    "Are you sure? This will remove all tasks and subtasks from your device storage.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DMSans)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllTasks()
                        Toast.makeText(context, "All tasks cleared", Toast.LENGTH_SHORT).show()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Wipe All", fontWeight = FontWeight.Bold, color = TikoCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = TikoOutline)
                }
            },
            shape = RoundedCornerShape(0.dp),
            containerColor = TikoCardFill
        )
    }

    // Reset Sample Data Dialog
    if (showResetSampleDialog) {
        AlertDialog(
            onDismissRequest = { showResetSampleDialog = false },
            title = {
                Text(
                    "Restore Sample Tasks?",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = BricolageGrotesque)
                )
            },
            text = {
                Text(
                    "This will restore the 12 Done, 5 Pending tasks from the original mockup.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DMSans)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        Toast.makeText(context, "Sample tasks restored!", Toast.LENGTH_SHORT).show()
                        showResetSampleDialog = false
                    }
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold, color = TikoOutline)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSampleDialog = false }) {
                    Text("Cancel", color = TikoOutline)
                }
            },
            shape = RoundedCornerShape(0.dp),
            containerColor = TikoCardFill
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(
                    "Privacy & Offline Storage",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = BricolageGrotesque)
                )
            },
            text = {
                Text(
                    "Tiko is built offline-first. All your task titles, notes, dates, and categories are saved directly in an encrypted SQLite database on your physical device.\n\nNo accounts required, no tracking cookies, and no data leaves your phone.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DMSans, lineHeight = 20.sp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Got it", fontWeight = FontWeight.Bold, color = TikoOutline)
                }
            },
            shape = RoundedCornerShape(0.dp),
            containerColor = TikoCardFill
        )
    }

    // Select Priority Dialog
    if (showPriorityDialog) {
        SelectionDialog(
            title = "Default Task Priority",
            options = TaskPriorities.ALL,
            selected = defaultPriority,
            onSelect = {
                preferences.setDefaultPriority(it)
                showPriorityDialog = false
            },
            onDismiss = { showPriorityDialog = false }
        )
    }

    // Select Category Dialog
    if (showCategoryDialog) {
        SelectionDialog(
            title = "Default Task List",
            options = TaskCategories.ALL,
            selected = defaultCategory,
            onSelect = {
                preferences.setDefaultCategory(it)
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false }
        )
    }

    // Select Reminder Dialog
    if (showReminderDialog) {
        SelectionDialog(
            title = "Default Reminder",
            options = listOf("None", "15 min before", "30 min before", "1 hour before", "1 day before"),
            selected = defaultReminder,
            onSelect = {
                preferences.setDefaultReminder(it)
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false }
        )
    }

    // Select Day of Week Dialog
    if (showDayOfWeekDialog) {
        SelectionDialog(
            title = "First Day of Week",
            options = listOf("Monday", "Sunday", "Saturday"),
            selected = firstDayOfWeek,
            onSelect = {
                preferences.setFirstDayOfWeek(it)
                showDayOfWeekDialog = false
            },
            onDismiss = { showDayOfWeekDialog = false }
        )
    }
}

// --- SETTINGS UI COMPONENTS ---

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TikoText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TikoText
            )
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TikoText
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = DMSans,
                    color = TikoText.copy(alpha = 0.7f)
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        NeoSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingOptionRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TikoText
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = DMSans,
                    fontWeight = FontWeight.Bold,
                    color = TikoMustard
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TikoText.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconTint.copy(alpha = 0.25f))
                .border(1.5.dp, TikoOutline, RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TikoText,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TikoText
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = DMSans,
                    color = TikoText.copy(alpha = 0.7f)
                )
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TikoText.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(TikoOutline.copy(alpha = 0.35f))
    )
}

// Neo-Brutalist Custom Switch matching 2dp black border, 0dp corner
@Composable
fun NeoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(52.dp)
            .height(28.dp)
            .background(if (checked) TikoOutline else TikoCardFill, RoundedCornerShape(0.dp))
            .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(if (checked) TikoCardFill else TikoOutline, RoundedCornerShape(0.dp))
        )
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontFamily = BricolageGrotesque))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { opt ->
                    val isSel = opt == selected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, TikoOutline, RoundedCornerShape(0.dp))
                            .background(if (isSel) TikoOutline else TikoCardFill)
                            .clickable { onSelect(opt) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = DMSans,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else TikoText
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TikoOutline)
            }
        },
        shape = RoundedCornerShape(0.dp),
        containerColor = TikoCardFill
    )
}
