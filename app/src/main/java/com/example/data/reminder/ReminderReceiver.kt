package com.example.data.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.TikoDatabase
import com.example.data.model.TaskEntity
import com.musab.tiko.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == com.example.data.streak.StreakReminderScheduler.ACTION_STREAK_REMINDER) {
            handleStreakReminder(context)
            return
        }

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, -1L)
        val now = System.currentTimeMillis()
        val delay = if (scheduledTime > 0) now - scheduledTime else 0L

        Log.d("TikoReminder", "ReminderReceiver fired: taskId=$taskId, delay=${delay}ms, scheduled=$scheduledTime, now=$now")
        ReminderScheduler.recordFiredDelay(context, delay)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
                    return@launch
                }

                // Query Room database with a short timeout so work stays fast
                val taskFromDb = withTimeoutOrNull(2000L) {
                    try {
                        val database = TikoDatabase.getInstance(context)
                        database.taskDao().getTaskByIdOnce(taskId)
                    } catch (e: Exception) {
                        Log.w("TikoReminder", "Failed to query task from DB: ${e.message}")
                        null
                    }
                }

                if (taskFromDb != null) {
                    if (taskFromDb.done || taskFromDb.deleted) {
                        Log.d("TikoReminder", "Task $taskId is done or deleted; dropping reminder notification.")
                        return@launch
                    }
                    showNotification(context, taskFromDb, scheduledTime)
                } else {
                    // Fallback to extras if Room lookup is slow or fails
                    val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: ""
                    if (title.isNotBlank()) {
                        Log.d("TikoReminder", "Room lookup timed out or failed; posting notification from extras for taskId=$taskId")
                        val fallbackTask = TaskEntity(
                            id = taskId,
                            title = title,
                            category = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: "Work",
                            notes = intent.getStringExtra(EXTRA_TASK_NOTES) ?: "",
                            dueTime = intent.getStringExtra(EXTRA_TASK_DUE_TIME) ?: TaskEntity.DEFAULT_DUE_TIME
                        )
                        showNotification(context, fallbackTask, scheduledTime)
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        const val CHANNEL_ID_V2 = "task_reminders_v2"
        const val CHANNEL_ID = CHANNEL_ID_V2
        const val CHANNEL_NAME = "Task reminders"
        const val OLD_CHANNEL_ID = "task_reminders"

        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_TASK_CATEGORY = "task_category"
        const val EXTRA_TASK_NOTES = "task_notes"
        const val EXTRA_TASK_DUE_TIME = "task_due_time"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        private var testCategoryIndex = 0

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                // Delete the old channel if it exists
                try {
                    notificationManager.deleteNotificationChannel(OLD_CHANNEL_ID)
                } catch (_: Exception) {}

                val existing = notificationManager.getNotificationChannel(CHANNEL_ID_V2)
                if (existing == null) {
                    val soundUri = resolveSoundUri(context)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()

                    val channel = NotificationChannel(
                        CHANNEL_ID_V2,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Reminders for your tasks"
                        enableLights(true)
                        lightColor = Color.parseColor("#6B4EFF")
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 250, 250, 250)
                        setShowBadge(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setSound(soundUri, audioAttributes)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }

        fun resolveSoundUri(context: Context): Uri {
            val resId = try {
                context.resources.getIdentifier("tiko_chime", "raw", context.packageName)
            } catch (_: Exception) {
                0
            }
            return if (resId != 0) {
                Uri.parse("android.resource://${context.packageName}/$resId")
            } else {
                Log.w("TikoSound", "tiko_chime missing, using default")
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: Uri.parse("content://settings/system/notification_sound")
            }
        }

        fun showNotification(context: Context, task: TaskEntity, scheduledTime: Long = -1L) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            createChannel(context)

            val safeTitle = NotificationCopy.sanitizeTitle(task.title)
            val isAtDueTime = task.reminder.isBlank() ||
                    task.reminder.equals("At time", ignoreCase = true) ||
                    task.reminder.equals("at due time", ignoreCase = true) ||
                    task.reminder.equals("0 min before", ignoreCase = true)

            val title = NotificationCopy.getNotificationTitle(
                category = task.category,
                reminderOffset = task.reminder,
                isAtDueTime = isAtDueTime
            )

            val bodyText = NotificationCopy.buildNotificationBody(
                title = safeTitle,
                category = task.category,
                dueTime = task.dueTime
            )

            val expandedText = NotificationCopy.buildExpandedNotesText(
                title = safeTitle,
                category = task.category,
                dueTime = task.dueTime,
                notes = task.notes
            )

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("task_id", task.id)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                task.id.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationId = task.id.hashCode()

            // Action: Done
            val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_MARK_DONE
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_TASK_TITLE, safeTitle)
                putExtra(EXTRA_TASK_CATEGORY, task.category)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action: Snooze (10 min)
            val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_SNOOZE
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_TASK_TITLE, safeTitle)
                putExtra(EXTRA_TASK_CATEGORY, task.category)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val largeIconBitmap = NotificationIconHelper.createCategoryLargeIcon(context, task.category)
            val categoryColor = NotificationIconHelper.getCategoryColor(task.category)
            val triggerWhen = if (scheduledTime > 0) scheduledTime else System.currentTimeMillis()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_V2)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setLargeIcon(largeIconBitmap)
                .setColor(categoryColor)
                .setContentTitle(title)
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
                .setWhen(triggerWhen)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(false)
                .setGroup(NotificationGroupingHelper.GROUP_KEY_TASKS)
                .addAction(0, "Done", donePendingIntent)
                .addAction(0, "Snooze", snoozePendingIntent)
                .build()

            notificationManager.notify(notificationId, notification)

            // Check grouping if 3 or more reminders fire within 5 minutes
            NotificationGroupingHelper.recordAndCheckGroupSummary(
                context = context,
                taskId = task.id,
                taskTitle = safeTitle,
                notificationManager = notificationManager
            )
        }

        fun sendTestReminder(context: Context) {
            val categories = listOf("Work", "Health", "Study", "Personal", "Streak")
            val selectedCategory = categories[testCategoryIndex % categories.size]
            testCategoryIndex++

            val sampleTitles = mapOf(
                "Work" to "Prepare weekly sprint demo",
                "Health" to "Afternoon stretch and water break",
                "Study" to "Review system architecture notes",
                "Personal" to "Plan dinner with close friends",
                "Streak" to "Complete 15-minute daily focus"
            )

            val sampleNotes = mapOf(
                "Work" to "Check key slides and ensure demo build is running smoothly.",
                "Health" to "Step away from the screen for 5 minutes.",
                "Study" to "Go through chapter 3 on indexing and concurrency.",
                "Personal" to "Reserve table at favorite Italian bistro.",
                "Streak" to "Keep your 7-day task completion streak going strong!"
            )

            val testTask = TaskEntity(
                id = "test-reminder-${System.currentTimeMillis()}",
                title = sampleTitles[selectedCategory] ?: "Sample Task",
                category = selectedCategory,
                notes = sampleNotes[selectedCategory] ?: "Test notification notes.",
                dueDate = "today",
                dueTime = "3:00 PM",
                reminder = "15 min before"
            )

            showNotification(context, testTask, System.currentTimeMillis())
        }

        fun handleStreakReminder(context: Context) {
            val preferences = com.example.data.local.UserPreferences(context)
            val enabled = preferences.streakRemindersEnabled.value
            val streak = preferences.currentStreak.value
            val lastCompleted = preferences.lastCompletedDay.value
            val todayKey = com.example.data.streak.StreakManager.todayDateKey()

            if (!enabled || streak < 2 || lastCompleted == todayKey) {
                Log.d("TikoStreakReminder", "Streak reminder dropped at fire time (enabled=$enabled, streak=$streak, lastCompleted=$lastCompleted, today=$todayKey)")
                if (enabled && streak >= 2) {
                    com.example.data.streak.StreakReminderScheduler.schedule(context, preferences)
                }
                return
            }

            if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
                return
            }

            createChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val intent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                999992,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val largeIcon = NotificationIconHelper.createCategoryLargeIcon(context, "Streak")
            val colorStreak = NotificationIconHelper.getCategoryColor("Streak")

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_V2)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setLargeIcon(largeIcon)
                .setColor(colorStreak)
                .setContentTitle("Protect your streak")
                .setContentText("Your $streak-day streak is waiting 🔥 Finish one task to keep it going.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Your $streak-day streak is waiting 🔥 Finish one task to keep it going."))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setOnlyAlertOnce(false)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .build()

            notificationManager.notify(999991, notification)
            Log.d("TikoStreakReminder", "Posted streak reminder notification for streak=$streak")

            com.example.data.streak.StreakReminderScheduler.schedule(context, preferences)
        }
    }
}
