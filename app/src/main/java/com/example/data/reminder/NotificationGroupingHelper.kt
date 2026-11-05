package com.example.data.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.musab.tiko.R
import org.json.JSONArray
import org.json.JSONObject

object NotificationGroupingHelper {

    const val GROUP_KEY_TASKS = "tiko_task_group"
    const val SUMMARY_NOTIFICATION_ID = 99999
    private const val WINDOW_MILLIS = 5 * 60 * 1000L // 5 minutes
    private const val PREFS_NAME = "tiko_notification_grouping"
    private const val KEY_RECENT = "recent_reminders_json"

    data class FiredRecord(val taskId: String, val title: String, val timestamp: Long)

    @Synchronized
    fun recordAndCheckGroupSummary(
        context: Context,
        taskId: String,
        taskTitle: String,
        notificationManager: NotificationManager
    ) {
        val now = System.currentTimeMillis()
        val records = loadRecords(context).toMutableList()

        // Remove old records outside 5 minutes and duplicates for this task
        records.removeAll { it.taskId == taskId || (now - it.timestamp) > WINDOW_MILLIS }

        val safeTitle = NotificationCopy.sanitizeTitle(taskTitle)
        records.add(FiredRecord(taskId, safeTitle, now))
        saveRecords(context, records)

        if (records.size >= 3) {
            postGroupSummary(context, records, notificationManager)
        }
    }

    @Synchronized
    fun onTaskDismissedOrDone(context: Context, taskId: String, notificationManager: NotificationManager) {
        val records = loadRecords(context).toMutableList()
        val removed = records.removeAll { it.taskId == taskId }
        if (removed) {
            saveRecords(context, records)
            if (records.size < 3) {
                notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
            } else {
                postGroupSummary(context, records, notificationManager)
            }
        }
    }

    private fun postGroupSummary(
        context: Context,
        records: List<FiredRecord>,
        notificationManager: NotificationManager
    ) {
        val count = records.size
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            SUMMARY_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("You have $count things coming up 🗓️")

        for (item in records.take(3)) {
            inboxStyle.addLine(item.title)
        }
        if (count > 3) {
            inboxStyle.setSummaryText("+${count - 3} more")
        }

        val summaryNotification = NotificationCompat.Builder(context, ReminderReceiver.CHANNEL_ID_V2)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(Color.parseColor("#6B4EFF"))
            .setContentTitle("You have $count things coming up 🗓️")
            .setContentText("You have $count tasks coming up")
            .setStyle(inboxStyle)
            .setGroup(GROUP_KEY_TASKS)
            .setGroupSummary(true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

    private fun loadRecords(context: Context): List<FiredRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RECENT, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<FiredRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FiredRecord(
                        taskId = obj.getString("taskId"),
                        title = obj.getString("title"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveRecords(context: Context, records: List<FiredRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (r in records) {
            val obj = JSONObject().apply {
                put("taskId", r.taskId)
                put("title", r.title)
                put("timestamp", r.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_RECENT, arr.toString()).apply()
    }
}
