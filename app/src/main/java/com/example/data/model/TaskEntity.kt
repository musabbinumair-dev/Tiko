package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val category: String = "Work",
    val priority: String = "Medium", // "Low", "Medium", "High"
    val dueDate: String = "", // e.g. "2026-10-15"
    val dueTime: String = "10:00 AM",
    val reminder: String = "30 min before", // "None", "15 min before", "30 min before", "1 hour before"
    val done: Boolean = false,
    val subtasksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
) {
    val subtasks: List<SubtaskItem>
        get() = parseSubtasks(subtasksJson)

    fun copyWithToggledDone(): TaskEntity {
        return copy(done = !done)
    }

    fun copyWithToggledSubtask(subtaskId: String): TaskEntity {
        val updated = subtasks.map {
            if (it.id == subtaskId) it.copy(done = !it.done) else it
        }
        return copy(subtasksJson = serializeSubtasks(updated))
    }

    fun copyWithAddedSubtask(title: String): TaskEntity {
        val updated = subtasks + SubtaskItem(title = title, done = false)
        return copy(subtasksJson = serializeSubtasks(updated))
    }

    fun copyWithRemovedSubtask(subtaskId: String): TaskEntity {
        val updated = subtasks.filterNot { it.id == subtaskId }
        return copy(subtasksJson = serializeSubtasks(updated))
    }

    companion object {
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
