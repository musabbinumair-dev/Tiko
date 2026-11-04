package com.musab.tiko

import android.app.Application
import com.example.data.FirebaseInitializer
import com.example.data.reminder.ReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseInitializer.init(this)
        } catch (_: Throwable) {}
        try {
            ReminderReceiver.createChannel(this)
        } catch (_: Throwable) {}
        try {
            com.example.data.update.UpdateDownloader.cleanUpIfUpdated(this)
        } catch (_: Throwable) {}
        try {
            com.example.data.sync.SyncCoordinator.getInstance(this).startListening()
        } catch (_: Throwable) {}
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = com.example.data.local.TikoDatabase.getInstance(this@MainApplication)
                    val tasks = db.taskDao().getAllTasksRaw().filter { !it.done && !it.deleted }
                    com.example.data.reminder.ReminderScheduler.rescheduleAllFutureReminders(this@MainApplication, tasks)
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }
}
