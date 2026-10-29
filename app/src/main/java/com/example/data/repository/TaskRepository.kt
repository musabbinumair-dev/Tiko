package com.example.data.repository

import com.example.data.local.TaskDao
import com.example.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class TaskRepository(private val taskDao: TaskDao) {

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
        finalTask.id
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        taskDao.updateTask(task.copy(updatedAt = now, synced = false))
    }

    suspend fun toggleTaskDone(task: TaskEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val newDone = !task.done
        val completedAt = if (newDone) now else null
        taskDao.setTaskDone(task.id, newDone, completedAt, now)
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
    }

    suspend fun deleteTaskById(id: String) = withContext(Dispatchers.IO) {
        taskDao.softDeleteById(id, System.currentTimeMillis())
    }

    suspend fun deleteCompletedTasks(): Int = withContext(Dispatchers.IO) {
        taskDao.softDeleteCompletedTasks(System.currentTimeMillis())
    }

    suspend fun clearAllTasks() = withContext(Dispatchers.IO) {
        taskDao.clearAll()
    }
}
