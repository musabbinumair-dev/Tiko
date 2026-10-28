package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TikoDatabase
import com.example.data.model.CategoryModel
import com.example.data.model.DefaultCategories
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskEntity
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class Screen {
    object Welcome : Screen()
    object Login : Screen()
    object SignUp : Screen()
    data class Main(val tab: NavigationTab = NavigationTab.HOME) : Screen()
    data class TaskDetail(val taskId: Long) : Screen()
    object AddTask : Screen()
    data class EditTask(val taskId: Long) : Screen()
    object ErrorSaveFailed : Screen() {
        const val ROUTE = "error_save_failed"
    }
    object NotFound : Screen() {
        const val ROUTE = "error_not_found"
    }
}

enum class NavigationTab {
    HOME, LISTS, CALENDAR, SETTINGS
}

data class TodayStats(
    val done: Int = 0,
    val pending: Int = 0,
    val streak: Int = 0
)

class TikoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TikoDatabase.getInstance(application)
    private val repository = TaskRepository(database.taskDao())
    val preferences = com.example.data.local.UserPreferences(application)

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Welcome))
    val screenStack: StateFlow<List<Screen>> = _screenStack.asStateFlow()
    val currentScreen: StateFlow<Screen> = _screenStack
        .combine(MutableStateFlow(Unit)) { stack, _ -> stack.lastOrNull() ?: Screen.Welcome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Screen.Welcome)

    private val _selectedTab = MutableStateFlow(NavigationTab.HOME)
    val selectedTab: StateFlow<NavigationTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>("All")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    var preselectedCategoryForNewTask: String? = null
    var retrySaveRequested: Boolean = false

    val categories: StateFlow<List<CategoryModel>> = preferences.customCategories
        .combine(allTasks) { custom, tasks ->
            val allList = DefaultCategories.toMutableList()
            for (c in custom) {
                if (allList.none { it.name.equals(c.name, ignoreCase = true) }) {
                    allList.add(c)
                }
            }
            val taskCategories = tasks.map { it.category }.distinct()
            for (tc in taskCategories) {
                if (tc.isNotBlank() && allList.none { it.name.equals(tc, ignoreCase = true) }) {
                    allList.add(CategoryModel(tc, "folder", 0xFF6B4EFF))
                }
            }
            allList
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultCategories)

    fun addCategory(name: String, iconKey: String = "folder", colorHex: Long = 0xFF6B4EFF) {
        if (name.isBlank()) return
        preferences.addCategory(CategoryModel(name.trim(), iconKey, colorHex))
    }

    fun deleteCategory(name: String) {
        preferences.deleteCategory(name.trim())
    }

    private val _selectedCalendarDate = MutableStateFlow(TikoDatabase.getTodayDateString())
    val selectedCalendarDate: StateFlow<String> = _selectedCalendarDate.asStateFlow()

    val todayStats: StateFlow<TodayStats> = allTasks.combine(MutableStateFlow(Unit)) { tasks, _ ->
        val doneCount = tasks.count { it.done }
        val pendingCount = tasks.count { !it.done }
        val completedDates = tasks.filter { it.done && it.dueDate.isNotBlank() }
            .map { it.dueDate }
            .distinct()
        val streak = if (doneCount > 0) completedDates.size.coerceAtLeast(1) else 0
        TodayStats(
            done = doneCount,
            pending = pendingCount,
            streak = streak
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayStats())

    fun selectTab(tab: NavigationTab) {
        _selectedTab.value = tab
        _screenStack.value = listOf(Screen.Main(tab))
    }

    fun navigateTo(screen: Screen) {
        _screenStack.value = _screenStack.value + screen
    }

    fun goBack(): Boolean {
        if (_screenStack.value.size > 1) {
            _screenStack.value = _screenStack.value.dropLast(1)
            return true
        }
        return false
    }

    fun navigateToRoute(route: String) {
        val screen = when (route) {
            "welcome" -> Screen.Welcome
            "login" -> Screen.Login
            "signup" -> Screen.SignUp
            "home" -> Screen.Main(NavigationTab.HOME)
            "lists" -> Screen.Main(NavigationTab.LISTS)
            "calendar" -> Screen.Main(NavigationTab.CALENDAR)
            "settings" -> Screen.Main(NavigationTab.SETTINGS)
            "add_task" -> Screen.AddTask
            Screen.ErrorSaveFailed.ROUTE -> Screen.ErrorSaveFailed
            Screen.NotFound.ROUTE -> Screen.NotFound
            else -> {
                if (route.startsWith("task_detail/")) {
                    val id = route.substringAfter("task_detail/").toLongOrNull()
                    if (id != null && allTasks.value.any { it.id == id }) {
                        Screen.TaskDetail(id)
                    } else {
                        Screen.NotFound
                    }
                } else if (route.startsWith("edit_task/")) {
                    val id = route.substringAfter("edit_task/").toLongOrNull()
                    if (id != null && allTasks.value.any { it.id == id }) {
                        Screen.EditTask(id)
                    } else {
                        Screen.NotFound
                    }
                } else {
                    Screen.NotFound
                }
            }
        }
        navigateTo(screen)
    }

    fun handleTaskNotificationIntent(taskId: Long) {
        if (allTasks.value.any { it.id == taskId }) {
            navigateTo(Screen.TaskDetail(taskId))
        } else {
            navigateTo(Screen.NotFound)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun selectCalendarDate(date: String) {
        _selectedCalendarDate.value = date
    }

    fun toggleTaskDone(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskDone(task)
        }
    }

    fun toggleSubtask(taskId: Long, subtaskId: String) {
        viewModelScope.launch {
            repository.toggleSubtask(taskId, subtaskId)
        }
    }

    fun addSubtask(taskId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addSubtask(taskId, title.trim())
        }
    }

    fun removeSubtask(taskId: Long, subtaskId: String) {
        viewModelScope.launch {
            repository.removeSubtask(taskId, subtaskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTaskById(taskId)
            goBack()
        }
    }

    fun logout() {
        preferences.clearUser()
        _screenStack.value = listOf(Screen.Welcome)
    }

    fun saveUserProfile(name: String, email: String) {
        preferences.setUserName(name.trim())
        preferences.setUserEmail(email.trim())
    }


    fun saveTask(
        title: String,
        notes: String,
        category: String,
        priority: String,
        dueDate: String,
        dueTime: String,
        reminder: String,
        subtasks: List<SubtaskItem>,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val task = TaskEntity(
                    title = title.trim(),
                    notes = notes.trim(),
                    category = category,
                    priority = priority,
                    dueDate = if (dueDate.isBlank()) TikoDatabase.getTodayDateString() else dueDate,
                    dueTime = if (dueTime.isBlank()) "10:00 AM" else dueTime,
                    reminder = reminder,
                    done = false,
                    subtasksJson = TaskEntity.serializeSubtasks(subtasks)
                )
                repository.insertTask(task)
                onSuccess()
                goBack()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun createQuickTask(title: String, category: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                notes = "",
                category = category,
                priority = "Medium",
                dueDate = TikoDatabase.getTodayDateString(),
                dueTime = "12:00 PM",
                reminder = "None",
                done = false,
                subtasksJson = "[]"
            )
            repository.insertTask(task)
        }
    }

    fun updateTask(
        id: Long,
        title: String,
        notes: String,
        category: String,
        priority: String,
        dueDate: String,
        dueTime: String,
        reminder: String,
        subtasks: List<SubtaskItem>,
        done: Boolean,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val task = TaskEntity(
                    id = id,
                    title = title.trim(),
                    notes = notes.trim(),
                    category = category,
                    priority = priority,
                    dueDate = if (dueDate.isBlank()) TikoDatabase.getTodayDateString() else dueDate,
                    dueTime = if (dueTime.isBlank()) "10:00 AM" else dueTime,
                    reminder = reminder,
                    done = done,
                    subtasksJson = TaskEntity.serializeSubtasks(subtasks)
                )
                repository.updateTask(task)
                onSuccess()
                goBack()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.clearAllTasks()
        }
    }

    fun deleteCompletedTasks(onCompleted: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.deleteCompletedTasks()
            onCompleted(count)
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.clearAllTasks()
        }
    }

    fun exportTasksFormatted(): String {
        val tasks = allTasks.value
        val sb = StringBuilder()
        sb.append("TIKO TASKS BACKUP\n")
        sb.append("Generated on: ${TikoDatabase.getTodayDateString()}\n")
        sb.append("Total tasks: ${tasks.size}\n")
        sb.append("----------------------------------------\n\n")

        tasks.forEachIndexed { index, task ->
            val status = if (task.done) "[X]" else "[ ]"
            sb.append("${index + 1}. $status ${task.title}\n")
            sb.append("   Category: ${task.category} | Priority: ${task.priority}\n")
            if (task.dueDate.isNotBlank() || task.dueTime.isNotBlank()) {
                sb.append("   Due: ${task.dueDate} at ${task.dueTime}\n")
            }
            if (task.notes.isNotBlank()) {
                sb.append("   Notes: ${task.notes}\n")
            }
            val subtasks = task.subtasks
            if (subtasks.isNotEmpty()) {
                sb.append("   Subtasks:\n")
                subtasks.forEach { sub ->
                    val subStatus = if (sub.done) "[X]" else "[ ]"
                    sb.append("     - $subStatus ${sub.title}\n")
                }
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}
