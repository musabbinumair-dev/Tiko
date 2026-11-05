package com.example.data.repository

import android.content.Context
import com.example.data.local.TaskDao
import com.example.data.model.TaskEntity
import com.example.data.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class TaskRepository(
    private val context: Context,
    private val taskDao: TaskDao
) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTaskById(id: String): Flow<TaskEntity?> = taskDao.getTaskById(id)

    suspend fun getTaskByIdOnce(id: String): TaskEntity? = withContext(Dispatchers.IO) {
        taskDao.getTaskByIdOnce(id)
    }

    fun getTasksByCategory(category: String): Flow<List<TaskEntity>> =
        taskDao.getTasksByCategory(category)

    fun getTasksByDate(date: String): Flow<List<TaskEntity>> =
        taskDao.getTasksByDate(date)

    suspend fun insertTask(task: TaskEntity): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val completedAt = if (task.done) (task.completedAt ?: now) else null
        val finalTask = if (task.id.isBlank()) {
            task.copy(id = UUID.randomUUID().toString(), completedAt = completedAt, updatedAt = now, synced = false)
        } else {
            task.copy(completedAt = completedAt, updatedAt = now, synced = false)
        }
        taskDao.insertTask(finalTask)
        if (!finalTask.done && !finalTask.deleted) {
            ReminderScheduler.schedule(context, finalTask)
        } else {
            ReminderScheduler.cancel(context, finalTask.id)
        }
        if (finalTask.done) {
            com.example.data.streak.StreakManager.recordTaskCompletion(context, preferences, taskDao, finalTask.id)
        } else {
            com.example.data.streak.StreakManager.recordTaskUndoneOrDeleted(context, preferences, taskDao, finalTask.id)
        }
        com.example.data.streak.StreakReminderScheduler.schedule(context, preferences)
        finalTask.id
    }

    private val preferences: com.example.data.local.UserPreferences by lazy {
        com.example.data.local.UserPreferences(context)
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val completedAt = if (task.done) (task.completedAt ?: now) else null
        val updated = task.copy(completedAt = completedAt, updatedAt = now, synced = false)
        taskDao.updateTask(updated)
        if (!updated.done && !updated.deleted) {
            ReminderScheduler.schedule(context, updated)
        } else {
            ReminderScheduler.cancel(context, updated.id)
        }
        if (updated.done) {
            com.example.data.streak.StreakManager.recordTaskCompletion(context, preferences, taskDao, updated.id)
        } else {
            com.example.data.streak.StreakManager.recordTaskUndoneOrDeleted(context, preferences, taskDao, updated.id)
        }
        com.example.data.streak.StreakReminderScheduler.schedule(context, preferences)
    }

    suspend fun toggleTaskDone(task: TaskEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val newDone = !task.done
        val completedAt = if (newDone) now else null
        taskDao.setTaskDone(task.id, newDone, completedAt, now)
        if (newDone) {
            ReminderScheduler.cancel(context, task.id)
            com.example.data.streak.StreakManager.recordTaskCompletion(context, preferences, taskDao, task.id)
        } else {
            val uncompleted = task.copy(done = false, completedAt = null, updatedAt = now)
            ReminderScheduler.schedule(context, uncompleted)
            com.example.data.streak.StreakManager.recordTaskUndoneOrDeleted(context, preferences, taskDao, task.id)
        }
        com.example.data.streak.StreakReminderScheduler.schedule(context, preferences)
    }

    suspend fun completeTask(taskId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        taskDao.setTaskDone(taskId, true, now, now)
        ReminderScheduler.cancel(context, taskId)
        com.example.data.streak.StreakManager.recordTaskCompletion(context, preferences, taskDao, taskId)
        com.example.data.streak.StreakReminderScheduler.schedule(context, preferences)
    }

    suspend fun toggleSubtask(taskId: String, subtaskId: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskByIdOnce(taskId) ?: return@withContext
        val updated = task.copyWithToggledSubtask(subtaskId)
        taskDao.updateTask(updated)
    }

    suspend fun addSubtask(taskId: String, title: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskByIdOnce(taskId) ?: return@withContext
        val updated = task.copyWithAddedSubtask(title)
        taskDao.updateTask(updated)
    }

    suspend fun removeSubtask(taskId: String, subtaskId: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskByIdOnce(taskId) ?: return@withContext
        val updated = task.copyWithRemovedSubtask(subtaskId)
        taskDao.updateTask(updated)
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.softDeleteById(task.id, System.currentTimeMillis())
        ReminderScheduler.cancel(context, task.id)
        com.example.data.streak.StreakManager.recordTaskUndoneOrDeleted(context, preferences, taskDao, task.id)
        com.example.data.streak.StreakReminderScheduler.schedule(context, preferences)
    }

    suspend fun deleteTaskById(id: String) = withContext(Dispatchers.IO) {
        taskDao.softDeleteById(id, System.currentTimeMillis())
        ReminderScheduler.cancel(context, id)
        com.example.data.streak.StreakManager.recordTaskUndoneOrDeleted(context, preferences, taskDao, id)
        com.example.data.streak.StreakReminderScheduler.schedule(context, preferences)
    }

    suspend fun deleteCompletedTasks(): Int = withContext(Dispatchers.IO) {
        val count = taskDao.softDeleteCompletedTasks(System.currentTimeMillis())
        com.example.data.streak.StreakManager.recordTaskUndoneOrDeleted(context, preferences, taskDao, "delete-completed")
        com.example.data.streak.StreakReminderScheduler.schedule(context, preferences)
        count
    }

    suspend fun clearAllTasks() = withContext(Dispatchers.IO) {
        val allTasks = taskDao.getAllTasksRaw()
        taskDao.clearAll()
        ReminderScheduler.cancelAll(context, allTasks)
    }
}
