package com.example.data.reminder

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.local.UserPreferences
import com.example.ui.components.AppToastController
import com.example.ui.components.ToastType

object NotificationPermissionHelper {

    fun hasNotificationPermission(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isRealNotificationStateActive(context: Context, preferences: UserPreferences): Boolean {
        return preferences.notificationsEnabled.value && hasNotificationPermission(context)
    }

    fun isPermanentlyDenied(activity: Activity, preferences: UserPreferences): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (isGranted) return false

            val hasRequestedBefore = preferences.hasRequestedNotificationPermission.value
            val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
            return hasRequestedBefore && !shouldShow
        } else {
            return !NotificationManagerCompat.from(activity).areNotificationsEnabled()
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                openNotificationSettings(context)
            }
        }
    }

    fun checkAndShowExactAlarmNotice(context: Context, preferences: UserPreferences) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            if (!alarmManager.canScheduleExactAlarms() && !preferences.hasShownExactAlarmNotice.value) {
                preferences.setHasShownExactAlarmNotice(true)
                AppToastController.show(
                    message = "Exact alarms disabled. Tap to enable.",
                    type = ToastType.Info,
                    actionLabel = "Settings",
                    onAction = {
                        openExactAlarmSettings(context)
                    }
                )
            }
        }
    }
}

