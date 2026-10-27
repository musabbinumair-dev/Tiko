package com.example.data.repository

import com.example.data.local.TaskDao
import com.example.data.local.TikoDatabase
import com.example.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTaskById(id: Long): Flow<TaskEntity?> = taskDao.getTaskById(id)

    suspend fun getTaskByIdOnce(id: Long): TaskEntity? = withContext(Dispatchers.IO) {
        taskDao.getTaskByIdOnce(id)
    }

    fun getTasksByCategory(category: String): Flow<List<TaskEntity>> =
        taskDao.getTasksByCategory(category)

    fun getTasksByDate(date: String): Flow<List<TaskEntity>> =
        taskDao.getTasksByDate(date)

    suspend fun insertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
    }

    suspend fun toggleTaskDone(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.setTaskDone(task.id, !task.done)
    }

    suspend fun toggleSubtask(taskId: Long, subtaskId: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskByIdOnce(taskId) ?: return@withContext
        val updated = task.copyWithToggledSubtask(subtaskId)
        taskDao.updateTask(updated)
    }

    suspend fun addSubtask(taskId: Long, title: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskByIdOnce(taskId) ?: return@withContext
        val updated = task.copyWithAddedSubtask(title)
        taskDao.updateTask(updated)
    }

    suspend fun removeSubtask(taskId: Long, subtaskId: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskByIdOnce(taskId) ?: return@withContext
        val updated = task.copyWithRemovedSubtask(subtaskId)
        taskDao.updateTask(updated)
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) = withContext(Dispatchers.IO) {
        taskDao.deleteById(id)
    }

    suspend fun deleteCompletedTasks(): Int = withContext(Dispatchers.IO) {
        taskDao.deleteCompletedTasks()
    }

    suspend fun clearAllTasks() = withContext(Dispatchers.IO) {
        taskDao.clearAll()
    }
}
