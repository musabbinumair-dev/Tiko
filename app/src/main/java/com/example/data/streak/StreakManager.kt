package com.example.data.streak

import android.content.Context
import android.util.Log
import com.example.data.local.TaskDao
import com.example.data.local.UserPreferences
import com.example.data.model.TaskEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Immutable representation of the user's streak state.
 */
data class StreakData(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDay: String = "", // "yyyy-MM-dd"
    val streakDays: Set<String> = emptySet() // pruned to last 60 days
)

data class WeeklyBarData(
    val day: String,
    val count: Int,
    val isHighlight: Boolean = false
)

/**
 * Single source of truth for streak calculation, persistence, and synchronization in Tiko.
 * Pure logic functions are exposed for unit testing.
 */
object StreakManager {

    private const val TAG = "TikoStreak"
    private const val MAX_HISTORY_DAYS = 60L
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    private val _streakState = MutableStateFlow(StreakData())
    val streakState: StateFlow<StreakData> = _streakState.asStateFlow()

    // -------------------------------------------------------------
    // Pure Date & Calculation Helpers (testable without Android SDK)
    // -------------------------------------------------------------

    fun dateToDateKey(date: LocalDate): String = date.format(DATE_FORMATTER)

    fun timestampToDateKey(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().format(DATE_FORMATTER)
    }

    fun todayDateKey(zoneId: ZoneId = ZoneId.systemDefault()): String {
        return LocalDate.now(zoneId).format(DATE_FORMATTER)
    }

    fun yesterdayDateKey(zoneId: ZoneId = ZoneId.systemDefault()): String {
        return LocalDate.now(zoneId).minusDays(1).format(DATE_FORMATTER)
    }

    fun taskToDateKey(task: TaskEntity, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        if (!task.done || task.deleted) return null
        return if (task.completedAt != null && task.completedAt > 0) {
            timestampToDateKey(task.completedAt, zoneId)
        } else if (task.dueDate.isNotBlank()) {
            task.dueDate
        } else {
            timestampToDateKey(task.updatedAt, zoneId)
        }
    }

    /**
     * Core pure function to recompute StreakData from an arbitrary set of completed date keys.
     * Rules:
     * - Prunes streakDays to the last 60 days from [todayKey].
     * - Finds the latest date in streakDays as [lastCompletedDay].
     * - If streakDays is empty, currentStreak = 0.
     * - If [todayKey] is in streakDays, count backward consecutively from today.
     * - Else if [yesterdayKey] is in streakDays, count backward consecutively from yesterday.
     * - Else, currentStreak = 0 (streak is broken).
     * - longestStreak = max(existingLongest, currentStreak).
     */
    fun computeStreak(
        streakDays: Set<String>,
        todayKey: String,
        existingLongest: Int = 0
    ): StreakData {
        val today = try {
            LocalDate.parse(todayKey, DATE_FORMATTER)
        } catch (_: Exception) {
            LocalDate.now()
        }
        val cutoffDate = today.minusDays(MAX_HISTORY_DAYS)

        // Clean & prune to last 60 days
        val validDates = mutableSetOf<LocalDate>()
        for (key in streakDays) {
            if (key.isBlank()) continue
            try {
                val parsed = LocalDate.parse(key, DATE_FORMATTER)
                if (!parsed.isBefore(cutoffDate)) {
                    validDates.add(parsed)
                }
            } catch (_: Exception) {
                // Ignore malformed keys
            }
        }

        if (validDates.isEmpty()) {
            return StreakData(
                currentStreak = 0,
                longestStreak = existingLongest,
                lastCompletedDay = "",
                streakDays = emptySet()
            )
        }

        val prunedKeys = validDates.map { dateToDateKey(it) }.toSet()
        val latestDate = validDates.maxOrNull()!!
        val lastCompletedDay = dateToDateKey(latestDate)

        val yesterday = today.minusDays(1)
        val currentStreak: Int

        if (validDates.contains(today)) {
            // Count backward consecutively from today
            var count = 0
            var checkDate = today
            while (validDates.contains(checkDate)) {
                count++
                checkDate = checkDate.minusDays(1)
            }
            currentStreak = count
        } else if (validDates.contains(yesterday)) {
            // Count backward consecutively from yesterday (user has today to extend)
            var count = 0
            var checkDate = yesterday
            while (validDates.contains(checkDate)) {
                count++
                checkDate = checkDate.minusDays(1)
            }
            currentStreak = count
        } else {
            // Neither today nor yesterday was completed -> streak broken
            currentStreak = 0
        }

        val longest = maxOf(existingLongest, currentStreak)

        return StreakData(
            currentStreak = currentStreak,
            longestStreak = longest,
            lastCompletedDay = lastCompletedDay,
            streakDays = prunedKeys
        )
    }

    /**
     * Pure function for when a task is completed on [completionDateKey].
     */
    fun onTaskCompleted(
        currentData: StreakData,
        completionDateKey: String,
        todayKey: String = completionDateKey
    ): StreakData {
        val updatedDays = currentData.streakDays + completionDateKey
        return computeStreak(
            streakDays = updatedDays,
            todayKey = todayKey,
            existingLongest = currentData.longestStreak
        )
    }

    /**
     * Pure function for when a task is undone or deleted.
     * Takes the list of remaining done tasks in the system to determine remaining valid completion days.
     */
    fun onTaskUndoneOrDeleted(
        currentData: StreakData,
        remainingDoneTasks: List<TaskEntity>,
        todayKey: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakData {
        val activeDoneDays = remainingDoneTasks.mapNotNull { taskToDateKey(it, zoneId) }.toSet()
        val updatedDays = currentData.streakDays.filter { it in activeDoneDays }.toSet()
        return computeStreak(
            streakDays = updatedDays,
            todayKey = todayKey,
            existingLongest = currentData.longestStreak
        )
    }

    /**
     * Pure function for app launch / resume checks:
     * If lastCompletedDay is not today and not yesterday, currentStreak becomes 0.
     */
    fun checkAndRecalculateOnLaunch(
        currentData: StreakData,
        todayKey: String
    ): StreakData {
        return computeStreak(
            streakDays = currentData.streakDays,
            todayKey = todayKey,
            existingLongest = currentData.longestStreak
        )
    }

    /**
     * Pure function for merging remote Firestore streak data.
     * Takes union of streakDays, max of longestStreak, and recomputes currentStreak.
     */
    fun mergeRemoteStreak(
        local: StreakData,
        remote: StreakData,
        todayKey: String
    ): StreakData {
        val mergedDays = local.streakDays + remote.streakDays
        val mergedLongest = maxOf(local.longestStreak, remote.longestStreak)
        return computeStreak(
            streakDays = mergedDays,
            todayKey = todayKey,
            existingLongest = mergedLongest
        )
    }

    /**
     * Computes weekly Monday-Sunday completion chart data for the current calendar week.
     */
    fun computeWeeklyData(
        streakDays: Set<String>,
        tasks: List<TaskEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        referenceDate: LocalDate = LocalDate.now(zoneId)
    ): List<WeeklyBarData> {
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayOfWeekIndex = (referenceDate.dayOfWeek.value - 1).coerceIn(0, 6) // Mon=0 .. Sun=6
        val mondayDate = referenceDate.minusDays(dayOfWeekIndex.toLong())

        val counts = IntArray(7)

        for (i in 0..6) {
            val date = mondayDate.plusDays(i.toLong())
            val dateKey = dateToDateKey(date)
            // Count done tasks completed on this date
            val taskCount = tasks.count { task ->
                task.done && !task.deleted && taskToDateKey(task, zoneId) == dateKey
            }
            // If tasks were archived but streakDays recorded the day, show at least 1
            val effectiveCount = if (taskCount > 0) taskCount else if (streakDays.contains(dateKey)) 1 else 0
            counts[i] = effectiveCount
        }

        return dayNames.mapIndexed { index, name ->
            WeeklyBarData(
                day = name,
                count = counts[index],
                isHighlight = (index == dayOfWeekIndex)
            )
        }
    }

    // -------------------------------------------------------------
    // Android State, Persistence & Firestore Sync
    // -------------------------------------------------------------

    fun loadFromPreferences(preferences: UserPreferences): StreakData {
        val data = StreakData(
            currentStreak = preferences.currentStreak.value,
            longestStreak = preferences.longestStreak.value,
            lastCompletedDay = preferences.lastCompletedDay.value,
            streakDays = preferences.streakDays.value
        )
        _streakState.value = data
        return data
    }

    fun saveToPreferences(preferences: UserPreferences, data: StreakData) {
        preferences.setStreakData(
            current = data.currentStreak,
            longest = data.longestStreak,
            lastDay = data.lastCompletedDay,
            days = data.streakDays
        )
        _streakState.value = data
    }

    /**
     * Called whenever a task is completed.
     */
    fun recordTaskCompletion(
        context: Context,
        preferences: UserPreferences,
        taskDao: TaskDao,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakData {
        val current = loadFromPreferences(preferences)
        val today = todayDateKey(zoneId)
        val updated = onTaskCompleted(current, today, today)
        saveToPreferences(preferences, updated)
        syncToFirestoreAsync(preferences, updated)
        return updated
    }

    /**
     * Called whenever a task is uncompleted or deleted.
     */
    suspend fun recordTaskUndoneOrDeleted(
        context: Context,
        preferences: UserPreferences,
        taskDao: TaskDao,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakData {
        val current = loadFromPreferences(preferences)
        val today = todayDateKey(zoneId)
        val remainingTasks = taskDao.getAllTasksRaw().filter { it.done && !it.deleted }
        val updated = onTaskUndoneOrDeleted(current, remainingTasks, today, zoneId)
        saveToPreferences(preferences, updated)
        syncToFirestoreAsync(preferences, updated)
        return updated
    }

    /**
     * Called on app launch, activity onResume, or screen entry to verify streak validity.
     */
    suspend fun recalculateOnAppOpen(
        context: Context,
        preferences: UserPreferences,
        taskDao: TaskDao,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakData {
        val current = loadFromPreferences(preferences)
        val today = todayDateKey(zoneId)

        // Seed with remaining done tasks if preferences are empty
        val streakDays = if (current.streakDays.isEmpty()) {
            val tasks = taskDao.getAllTasksRaw().filter { it.done && !it.deleted }
            tasks.mapNotNull { taskToDateKey(it, zoneId) }.toSet()
        } else {
            current.streakDays
        }

        val updated = computeStreak(
            streakDays = streakDays,
            todayKey = today,
            existingLongest = current.longestStreak
        )

        saveToPreferences(preferences, updated)
        syncToFirestoreAsync(preferences, updated)
        return updated
    }

    /**
     * Non-blocking write to Firestore `users/{uid}`.
     */
    fun syncToFirestoreAsync(
        preferences: UserPreferences,
        streakData: StreakData,
        firestoreProvider: () -> FirebaseFirestore? = {
            try { FirebaseFirestore.getInstance() } catch (_: Throwable) { null }
        }
    ) {
        val uid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Throwable) {
            null
        } ?: return

        if (uid.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = firestoreProvider() ?: return@launch
                val userDocRef = firestore.collection("users").document(uid)
                val streakMap = mapOf(
                    "currentStreak" to streakData.currentStreak,
                    "longestStreak" to streakData.longestStreak,
                    "lastCompletedDay" to streakData.lastCompletedDay,
                    "streakDays" to streakData.streakDays.toList(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(mapOf("streak" to streakMap), SetOptions.merge()).await()
                Log.d(TAG, "Synced streak data to Firestore: $streakMap")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync streak to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Handles streak map received from remote Firestore doc snapshot.
     */
    fun mergeFromFirestore(
        preferences: UserPreferences,
        remoteMap: Map<String, Any?>?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StreakData {
        val local = loadFromPreferences(preferences)
        if (remoteMap == null) return local

        val remoteCurrent = (remoteMap["currentStreak"] as? Number)?.toInt() ?: 0
        val remoteLongest = (remoteMap["longestStreak"] as? Number)?.toInt() ?: 0
        val remoteLastDay = remoteMap["lastCompletedDay"] as? String ?: ""
        val remoteDaysList = (remoteMap["streakDays"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        val remoteData = StreakData(
            currentStreak = remoteCurrent,
            longestStreak = remoteLongest,
            lastCompletedDay = remoteLastDay,
            streakDays = remoteDaysList.toSet()
        )

        val merged = mergeRemoteStreak(local, remoteData, todayDateKey(zoneId))
        saveToPreferences(preferences, merged)
        return merged
    }
}
