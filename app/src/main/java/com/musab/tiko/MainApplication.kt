package com.musab.tiko

import android.app.Application
import com.example.data.FirebaseInitializer

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseInitializer.init(this)
    }
}
