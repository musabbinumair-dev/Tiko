package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY createdAt DESC, id ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND deleted = 0")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskByIdIncludingDeleted(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id AND deleted = 0")
    suspend fun getTaskByIdOnce(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE category = :category AND deleted = 0 ORDER BY done ASC, id DESC")
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND deleted = 0 ORDER BY done ASC, id DESC")
    fun getTasksByDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE synced = 0")
    fun getUnsyncedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE synced = 0")
    suspend fun getUnsyncedTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksRaw(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun hardDeleteById(id: String)

    @Query("UPDATE tasks SET deleted = 1, updatedAt = :updatedAt, synced = 0 WHERE id = :id")
    suspend fun softDeleteById(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET done = :done, completedAt = :completedAt, updatedAt = :updatedAt, synced = 0 WHERE id = :id")
    suspend fun setTaskDone(
        id: String,
        done: Boolean,
        completedAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE tasks SET synced = :synced WHERE id = :id")
    suspend fun markSynced(id: String, synced: Boolean = true)

    @Query("UPDATE tasks SET deleted = 1, updatedAt = :updatedAt, synced = 0 WHERE done = 1 AND deleted = 0")
    suspend fun softDeleteCompletedTasks(updatedAt: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM tasks WHERE done = 1")
    suspend fun deleteCompletedTasks(): Int

    @Query("DELETE FROM tasks WHERE deleted = 1 AND synced = 1")
    suspend fun deleteSyncedTombstones(): Int

    @Query("DELETE FROM tasks")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM tasks WHERE deleted = 0")
    suspend fun count(): Int
}
