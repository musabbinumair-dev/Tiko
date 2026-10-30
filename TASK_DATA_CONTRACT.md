# Task Data Contract

This document defines the definitive data contract for tasks in Tiko across the Room SQLite database, Firestore documents, and Jetpack Compose UI screens.

---

## 1. Field Specification Table

| # | Room Entity Field | Firestore Document Key | Kotlin Type | Firestore / JSON Type | Allowed Values / Format | Default Value | Notes / Description |
|---|---|---|---|---|---|---|---|
| 1 | `id` | `id` | `String` | `string` | Non-empty UUID v4 string (e.g. `"550e8400-e29b-41d4-a716-446655440000"`) | `UUID.randomUUID().toString()` | Primary key in Room; document ID in `users/{uid}/tasks/{taskId}`. Repaired at startup if blank. |
| 2 | `title` | `title` | `String` | `string` | Non-empty string (trimmed) | `""` | User-visible task title. Required for save. |
| 3 | `notes` | `notes` | `String` | `string` | Free-form text (multiline allowed) | `""` | Optional additional notes / description. |
| 4 | `category` | `category` | `String` | `string` | Exact case-sensitive string: `"Work"`, `"Personal"`, `"Health"`, `"Shopping"`, `"Study"`, `"Ideas"` or custom category name | `"Work"` | Standard default is `"Work"`. UI maps via `CategoryIcons` and `CategoryColors`. |
| 5 | `priority` | `priority` | `String` | `string` | Exact case-sensitive string: `"Low"`, `"Medium"`, `"High"` | `"Medium"` | Controls priority chips, sorting, and indicators. |
| 6 | `dueDate` | `dueDate` | `String` | `string` | ISO date `"yyyy-MM-dd"` (e.g. `"2026-10-30"`) or `""` | `""` (or today's date on create) | Used for calendar mapping, overdue checks, and date filters. |
| 7 | `dueTime` | `dueTime` | `String` | `string` | Standard 12-hour format: `"<h>:<mm> AM/PM"` (e.g. `"9:00 AM"`, `"12:00 PM"`) | `"9:00 AM"` (`TaskEntity.DEFAULT_DUE_TIME`) | Task scheduled time of day. |
| 8 | `reminder` | `reminder` | `String` | `string` | Exact string: `"None"`, `"15 min before"`, `"30 min before"`, `"1 hour before"` | `"30 min before"` (`TaskEntity.DEFAULT_REMINDER`) | Exact alarm reminder trigger offset before due date/time. |
| 9 | `done` | `done` | `Boolean` | `boolean` | `true` or `false` | `false` | Task completion state. When toggled, updates `completedAt` and triggers sync. |
| 10 | `completedAt` | `completedAt` | `Long?` | `number` / `null` | Unix epoch millisecond timestamp (`Long`) or `null` | `null` | Set to `System.currentTimeMillis()` when `done = true`; cleared to `null` when `done = false`. |
| 11 | `subtasksJson` (Room) / `subtasks` (Model) | `subtasks` | `String` (JSON) / `List<SubtaskItem>` | `array of map` | Array of maps with keys `id` (`string`), `title` (`string`), `done` (`boolean`) | `"[]"` (Empty list) | Serialized as JSON in SQLite; stored as structured array of maps in Firestore. |
| 12 | `createdAt` | `createdAt` | `Long` | `number` | Unix epoch millisecond timestamp (`Long`) | `System.currentTimeMillis()` | Task creation timestamp. Preserved across updates. |
| 13 | `updatedAt` | `updatedAt` | `Long` | `number` | Unix epoch millisecond timestamp (`Long`) | `System.currentTimeMillis()` | Monotonically updated on every edit, status toggle, or soft delete. Used for LWW (Last-Write-Wins). |
| 14 | `deleted` | `deleted` | `Boolean` | `boolean` | `true` or `false` | `false` | Soft-delete tombstone flag for bidirectional sync. Synced tombstones are purged by Room. |
| 15 | `synced` | *(Not stored in Firestore)* | `Boolean` | N/A | `true` or `false` | `false` | Room-local dirty tracking flag. `false` indicates local pending changes needing push to Firestore. |

---

## 2. Stored Values & Comparison Map

### Categories
- **Stored Exact Strings**: `"Work"`, `"Personal"`, `"Health"`, `"Shopping"`, `"Study"`, `"Ideas"` (or custom user-created category names).
- **Comparison & Usage**:
  - **Home Sections**: Maps `task.category.lowercase()` to `CategoryIcons.getIcon(category)` and color palette.
  - **Lists Screen (`ListsScreen.kt`)**: 
    - Category total tasks count: `tasks.filter { it.category.equals(cat.name, ignoreCase = true) }.size`
    - Category completed count: `tasks.filter { it.category.equals(cat.name, ignoreCase = true) && it.done }.size`
    - Next pending task: `tasks.firstOrNull { it.category.equals(cat.name, ignoreCase = true) && !it.done }`
  - **Calendar Dots (`CalendarScreen.kt`)**: Dot color chosen based on `task.category.lowercase()` (`"work"` -> Purple `#6B4EFF`, `"health"` -> Yellow `#FFB92E`, `"study"` -> Lime `#C8DB6A`, other -> Coral Red `#FF5A5F`).
  - **New Task Preselection**: Passed from Lists category sheet into `AddEditTaskScreen`.

### Priorities
- **Stored Exact Strings**: `"Low"`, `"Medium"`, `"High"`.
- **Comparison & Usage**:
  - `TaskEntity.priority` defaults to `"Medium"`.
  - Displayed on `TaskDetailScreen` priority badge with flame icon.
  - Rendered in `exportTasksFormatted()` string export.
  - Selected in `AddEditTaskScreen` segmented pill selector.

### Reminders
- **Stored Exact Strings**: `"None"`, `"15 min before"`, `"30 min before"`, `"1 hour before"`.
- **Comparison & Usage**:
  - `ReminderScheduler.kt`: Parses offset in minutes (`"None"` -> 0, `"15 min before"` -> 15, `"30 min before"` -> 30, `"1 hour before"` -> 60).
  - Calculates trigger time: `dueDateTimeEpochMillis - offsetMillis`.
  - If `reminder == "None"`, no alarm is scheduled.

### Stats, Counts & Charts
- **Home Stat Pills (`HomeScreen.kt`)**:
  - `done`: `tasks.count { it.done }`
  - `pending`: `tasks.count { !it.done }`
- **Profile Stats (`SettingsScreen.kt`)**:
  - `doneCount`: `tasks.count { it.done }`
  - `streakText`: Distinct dates of completed tasks (`tasks.filter { it.done && it.dueDate.isNotBlank() }.map { it.dueDate }.distinct().size`)
  - `onTimeText`: Percentage of completed tasks vs total (`(completed.toFloat() / total * 100).toInt()%`)
  - `weeklyData`: Weekly completion distribution: 7-bar chart (Mon to Sun) grouped by the day of the week of `completedAt` (or `dueDate` for completed tasks in the current calendar week).

---

## 3. Serialization Consistency (Entity <-> Firestore)

Both `TaskEntity.toFirestoreMap()` and `TaskEntity.fromFirestoreMap()` enforce matching key names and defaults:

```kotlin
// In TaskEntity.kt:
fun toFirestoreMap(): Map<String, Any?> = mapOf(
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
    "subtasks" to subtasks.map { mapOf("id" to it.id, "title" to it.title, "done" to it.done) }
)

companion object {
    fun fromFirestoreMap(data: Map<String, Any?>, documentId: String): TaskEntity {
        // Reads exactly the same keys with exact defaults
        // Sets synced = true
    }
}
```

Every field survives a full entity -> Firestore map -> entity round trip without data loss, truncation, or unexpected default replacement.
