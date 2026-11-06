package com.example.data.repository

import android.content.Context
import com.example.data.local.TaskDao
import com.example.data.model.TaskEntity
import com.example.data.reminder.ReminderScheduler
import com.example.data.reminder.StreakReminderScheduler
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
        val finalTask = if (task.id.isBlank()) {
            task.copy(id = UUID.randomUUID().toString(), updatedAt = now, synced = false)
        } else {
            task.copy(updatedAt = now, synced = false)
        }
        taskDao.insertTask(finalTask)
        if (!finalTask.done && !finalTask.deleted) {
            ReminderScheduler.schedule(context, finalTask)
        } else {
            ReminderScheduler.cancel(context, finalTask.id)
        }
        StreakReminderScheduler.reschedule(context)
        finalTask.id
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updated = task.copy(updatedAt = now, synced = false)
        taskDao.updateTask(updated)
        if (!updated.done && !updated.deleted) {
            ReminderScheduler.schedule(context, updated)
        } else {
            ReminderScheduler.cancel(context, updated.id)
        }
        StreakReminderScheduler.reschedule(context)
    }

    suspend fun toggleTaskDone(task: TaskEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val newDone = !task.done
        val completedAt = if (newDone) now else null
        taskDao.setTaskDone(task.id, newDone, completedAt, now)
        if (newDone) {
            ReminderScheduler.cancel(context, task.id)
        } else {
            val uncompleted = task.copy(done = false, completedAt = null, updatedAt = now)
            ReminderScheduler.schedule(context, uncompleted)
        }
        StreakReminderScheduler.reschedule(context)
    }

    suspend fun completeTask(taskId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        taskDao.setTaskDone(taskId, true, now, now)
        ReminderScheduler.cancel(context, taskId)
        StreakReminderScheduler.reschedule(context)
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
        StreakReminderScheduler.reschedule(context)
    }

    suspend fun deleteTaskById(id: String) = withContext(Dispatchers.IO) {
        taskDao.softDeleteById(id, System.currentTimeMillis())
        ReminderScheduler.cancel(context, id)
        StreakReminderScheduler.reschedule(context)
    }

    suspend fun deleteCompletedTasks(): Int = withContext(Dispatchers.IO) {
        val count = taskDao.softDeleteCompletedTasks(System.currentTimeMillis())
        StreakReminderScheduler.reschedule(context)
        count
    }

    suspend fun clearAllTasks() = withContext(Dispatchers.IO) {
        val allTasks = taskDao.getAllTasksRaw()
        taskDao.clearAll()
        ReminderScheduler.cancelAll(context, allTasks)
        StreakReminderScheduler.cancel(context)
    }
}
