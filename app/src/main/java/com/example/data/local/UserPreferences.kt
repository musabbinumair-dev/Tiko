package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CategoryModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tiko_settings_prefs", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notif_enabled", false))
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

    private val _isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _hasSeenWelcome = MutableStateFlow(prefs.getBoolean("has_seen_welcome", false))
    val hasSeenWelcome: StateFlow<Boolean> = _hasSeenWelcome.asStateFlow()

    private val _hasRequestedNotificationPermission = MutableStateFlow(prefs.getBoolean("has_requested_notif_perm", false))
    val hasRequestedNotificationPermission: StateFlow<Boolean> = _hasRequestedNotificationPermission.asStateFlow()

    private val _hasShownExactAlarmNotice = MutableStateFlow(prefs.getBoolean("has_shown_exact_alarm_notice", false))
    val hasShownExactAlarmNotice: StateFlow<Boolean> = _hasShownExactAlarmNotice.asStateFlow()

    private val _darkModeEnabled = MutableStateFlow(
        if (prefs.contains("dark_mode_enabled")) prefs.getBoolean("dark_mode_enabled", false) else null
    )
    val darkModeEnabled: StateFlow<Boolean?> = _darkModeEnabled.asStateFlow()

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
        _darkModeEnabled.value = enabled
    }

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

    fun setGuestMode(isGuest: Boolean) {
        prefs.edit().putBoolean("is_guest_mode", isGuest).apply()
        _isGuestMode.value = isGuest
    }

    fun setHasSeenWelcome(seen: Boolean) {
        prefs.edit().putBoolean("has_seen_welcome", seen).apply()
        _hasSeenWelcome.value = seen
    }

    fun setHasRequestedNotificationPermission(requested: Boolean) {
        prefs.edit().putBoolean("has_requested_notif_perm", requested).apply()
        _hasRequestedNotificationPermission.value = requested
    }

    fun setHasShownExactAlarmNotice(shown: Boolean) {
        prefs.edit().putBoolean("has_shown_exact_alarm_notice", shown).apply()
        _hasShownExactAlarmNotice.value = shown
    }

    fun clearUser() {
        prefs.edit()
            .remove("user_name")
            .remove("user_email")
            .remove("is_guest_mode")
            .apply()
        _userName.value = ""
        _userEmail.value = ""
        _isGuestMode.value = false
    }

    private val _customCategories = MutableStateFlow(loadCustomCategories())
    val customCategories: StateFlow<List<CategoryModel>> = _customCategories.asStateFlow()

    private fun loadCustomCategories(): List<CategoryModel> {
        val json = prefs.getString("custom_categories_json", "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<CategoryModel>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CategoryModel(
                        name = obj.getString("name"),
                        iconKey = obj.optString("iconKey", "folder"),
                        colorHex = obj.optLong("colorHex", 0xFF6B4EFF)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustomCategories(categories: List<CategoryModel>) {
        val array = JSONArray()
        for (cat in categories) {
            val obj = JSONObject()
            obj.put("name", cat.name)
            obj.put("iconKey", cat.iconKey)
            obj.put("colorHex", cat.colorHex)
            array.put(obj)
        }
        prefs.edit().putString("custom_categories_json", array.toString()).apply()
    }

    fun addCategory(category: CategoryModel) {
        val current = _customCategories.value
        if (current.none { it.name.equals(category.name, ignoreCase = true) }) {
            val updated = current + category
            saveCustomCategories(updated)
            _customCategories.value = updated
        }
    }

    fun deleteCategory(name: String) {
        val updated = _customCategories.value.filterNot { it.name.equals(name, ignoreCase = true) }
        saveCustomCategories(updated)
        _customCategories.value = updated
    }
}
