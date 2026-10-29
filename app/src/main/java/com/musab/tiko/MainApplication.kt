package com.musab.tiko

import android.app.Application
import com.example.data.FirebaseInitializer
import com.example.data.reminder.ReminderReceiver

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.init(this)
        ReminderReceiver.createChannel(this)
    }
}
