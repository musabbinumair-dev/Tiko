package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tiko_settings_prefs", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notif_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _soundHapticsEnabled = MutableStateFlow(prefs.getBoolean("sound_haptics_enabled", true))
    val soundHapticsEnabled: StateFlow<Boolean> = _soundHapticsEnabled.asStateFlow()

    private val _dailyBriefingEnabled = MutableStateFlow(prefs.getBoolean("daily_briefing_enabled", true))
    val dailyBriefingEnabled: StateFlow<Boolean> = _dailyBriefingEnabled.asStateFlow()

    private val _defaultPriority = MutableStateFlow(prefs.getString("default_priority", "Medium") ?: "Medium")
    val defaultPriority: StateFlow<String> = _defaultPriority.asStateFlow()

    private val _defaultCategory = MutableStateFlow(prefs.getString("default_category", "Work") ?: "Work")
    val defaultCategory: StateFlow<String> = _defaultCategory.asStateFlow()

    private val _defaultReminder = MutableStateFlow(prefs.getString("default_reminder", "30 min before") ?: "30 min before")
    val defaultReminder: StateFlow<String> = _defaultReminder.asStateFlow()

    private val _firstDayOfWeek = MutableStateFlow(prefs.getString("first_day_of_week", "Monday") ?: "Monday")
    val firstDayOfWeek: StateFlow<String> = _firstDayOfWeek.asStateFlow()

    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        _userName.value = name
    }

    fun setUserEmail(email: String) {
        prefs.edit().putString("user_email", email).apply()
        _userEmail.value = email
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notif_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setSoundHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_haptics_enabled", enabled).apply()
        _soundHapticsEnabled.value = enabled
    }

    fun setDailyBriefingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("daily_briefing_enabled", enabled).apply()
        _dailyBriefingEnabled.value = enabled
    }

    fun setDefaultPriority(priority: String) {
        prefs.edit().putString("default_priority", priority).apply()
        _defaultPriority.value = priority
    }

    fun setDefaultCategory(category: String) {
        prefs.edit().putString("default_category", category).apply()
        _defaultCategory.value = category
    }

    fun setDefaultReminder(reminder: String) {
        prefs.edit().putString("default_reminder", reminder).apply()
        _defaultReminder.value = reminder
    }

    fun setFirstDayOfWeek(day: String) {
        prefs.edit().putString("first_day_of_week", day).apply()
        _firstDayOfWeek.value = day
    }

    fun clearUser() {
        prefs.edit().remove("user_name").remove("user_email").apply()
        _userName.value = ""
        _userEmail.value = ""
    }
}
