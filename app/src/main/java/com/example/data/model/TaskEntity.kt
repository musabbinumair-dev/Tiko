package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val category: String = "Work",
    val priority: String = "Medium", // "Low", "Medium", "High"
    val dueDate: String = "", // e.g. "2026-10-15"
    val dueTime: String = "10:00 AM",
    val reminder: String = "30 min before", // "None", "15 min before", "30 min before", "1 hour before"
    val done: Boolean = false,
    val completedAt: Long? = null,
    val subtasksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val synced: Boolean = false
) {
    val subtasks: List<SubtaskItem>
        get() = parseSubtasks(subtasksJson)

    fun copyWithToggledDone(): TaskEntity {
        val newDone = !done
        val now = System.currentTimeMillis()
        return copy(
            done = newDone,
            completedAt = if (newDone) now else null,
            updatedAt = now,
            synced = false
        )
    }

    fun copyWithToggledSubtask(subtaskId: String): TaskEntity {
        val updated = subtasks.map {
            if (it.id == subtaskId) it.copy(done = !it.done) else it
        }
        return copy(
            subtasksJson = serializeSubtasks(updated),
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
    }

    fun copyWithAddedSubtask(title: String): TaskEntity {
        val updated = subtasks + SubtaskItem(title = title, done = false)
        return copy(
            subtasksJson = serializeSubtasks(updated),
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
    }

    fun copyWithRemovedSubtask(subtaskId: String): TaskEntity {
        val updated = subtasks.filterNot { it.id == subtaskId }
        return copy(
            subtasksJson = serializeSubtasks(updated),
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
    }

    fun toFirestoreMap(): Map<String, Any?> {
        val subtaskList = subtasks.map { subtask ->
            mapOf(
                "id" to subtask.id,
                "title" to subtask.title,
                "done" to subtask.done
            )
        }
        return mapOf(
            "id" to id,
            "title" to title,
            "notes" to notes,
            "category" to category,
            "priority" to priority,
            "dueDate" to dueDate,
            "dueTime" to dueTime,
            "reminder" to reminder,
            "done" to done,
            "completedAt" to completedAt,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "deleted" to deleted,
            "subtasks" to subtaskList
        )
    }

    companion object {
        fun fromFirestoreMap(data: Map<String, Any?>, documentId: String): TaskEntity {
            val rawSubtasks = data["subtasks"] as? List<*>
            val subtaskList = mutableListOf<SubtaskItem>()
            if (rawSubtasks != null) {
                for (item in rawSubtasks) {
                    if (item is Map<*, *>) {
                        val subId = item["id"] as? String ?: UUID.randomUUID().toString()
                        val subTitle = item["title"] as? String ?: ""
                        val subDone = item["done"] as? Boolean ?: false
                        subtaskList.add(SubtaskItem(id = subId, title = subTitle, done = subDone))
                    }
                }
            }

            return TaskEntity(
                id = data["id"] as? String ?: documentId,
                title = data["title"] as? String ?: "",
                notes = data["notes"] as? String ?: "",
                category = data["category"] as? String ?: "Work",
                priority = data["priority"] as? String ?: "Medium",
                dueDate = data["dueDate"] as? String ?: "",
                dueTime = data["dueTime"] as? String ?: "10:00 AM",
                reminder = data["reminder"] as? String ?: "30 min before",
                done = data["done"] as? Boolean ?: false,
                completedAt = (data["completedAt"] as? Number)?.toLong(),
                subtasksJson = serializeSubtasks(subtaskList),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                deleted = data["deleted"] as? Boolean ?: false,
                synced = true
            )
        }

        fun parseSubtasks(json: String?): List<SubtaskItem> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<SubtaskItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SubtaskItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            done = obj.optBoolean("done", false)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeSubtasks(items: List<SubtaskItem>): String {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("done", item.done)
                array.put(obj)
            }
            return array.toString()
        }
    }
}

data class SubtaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean = false
)

object TaskCategories {
    val ALL = listOf("Work", "Personal", "Health", "Shopping", "Study", "Ideas")
}

object TaskPriorities {
    val ALL = listOf("Low", "Medium", "High")
}
