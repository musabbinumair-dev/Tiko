package com.example.data.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.TikoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("TikoReminder", "BootReceiver received action: $action")

        val isScheduleExactAlarmChanged = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        } else {
            action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        }

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET" ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.TIMEZONE_CHANGED" ||
            isScheduleExactAlarmChanged
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = TikoDatabase.getInstance(context)
                    val tasks = database.taskDao().getAllTasksRaw()
                    val activeTasks = tasks.filter { !it.done && !it.deleted }
                    ReminderScheduler.rescheduleAllFutureReminders(context, activeTasks)
                    StreakReminderScheduler.reschedule(context)
                    Log.d("TikoReminder", "BootReceiver rescheduled ${activeTasks.size} active tasks and streak reminder.")
                } catch (e: Exception) {
                    Log.w("TikoReminder", "Error rescheduling in BootReceiver: ${e.message}")
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}
