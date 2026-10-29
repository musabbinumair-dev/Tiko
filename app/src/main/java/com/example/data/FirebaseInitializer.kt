package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitializer {
    @Volatile
    private var initialized = false

    fun init(context: Context): FirebaseApp? {
        if (initialized) {
            return try {
                FirebaseApp.getInstance()
            } catch (e: Throwable) {
                null
            }
        }

        return synchronized(this) {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    initialized = true
                    return FirebaseApp.getInstance()
                }

                // 1. Try standard auto-configuration
                try {
                    val app = FirebaseApp.initializeApp(context.applicationContext)
                    if (app != null) {
                        initialized = true
                        return app
                    }
                } catch (_: Throwable) { }

                // 2. Explicit options fallback (resolves applicationId != namespace mismatch)
                val res = context.resources
                fun getResString(key: String, fallback: String): String {
                    for (pkg in listOf("com.musab.tiko", "com.example", context.packageName)) {
                        val id = res.getIdentifier(key, "string", pkg)
                        if (id != 0) {
                            return res.getString(id)
                        }
                    }
                    return fallback
                }

                val appId = getResString("google_app_id", "1:542946903408:android:468b3a90ad7ce4056ebbb5")
                val apiKey = getResString("google_api_key", "AIzaSyA60kwBJwa9D5_HBXmXpq-cif6iix0-UI4")
                val projectId = getResString("project_id", "tiko-de3d6")
                val storageBucket = getResString("google_storage_bucket", "tiko-de3d6.firebasestorage.app")
                val senderId = getResString("gcm_defaultSenderId", "542946903408")

                val options = FirebaseOptions.Builder()
                    .setApplicationId(appId)
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setStorageBucket(storageBucket)
                    .setGcmSenderId(senderId)
                    .build()

                val app = FirebaseApp.initializeApp(context.applicationContext, options)
                initialized = true
                app
            } catch (_: Throwable) {
                null
            }
        }
    }
}
