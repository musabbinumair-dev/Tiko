package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class TikoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TikoDatabase? = null

        fun getInstance(context: Context): TikoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TikoDatabase::class.java,
                    "tiko_tasks.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    seedDefaultTasks(database.taskDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        suspend fun seedDefaultTasks(dao: TaskDao) {
            val today = getTodayDateString()

            val initialTasks = listOf(
                // 1. Finish project proposal (Matches Screenshot 1 & 2!)
                TaskEntity(
                    title = "Finish project proposal",
                    notes = "Prepare a clear proposal with timeline, budget and key objectives.",
                    category = "Work",
                    priority = "High",
                    dueDate = today,
                    dueTime = "10:00 AM",
                    reminder = "30 min before",
                    done = true,
                    subtasksJson = TaskEntity.serializeSubtasks(
                        listOf(
                            SubtaskItem(title = "Write outline", done = true),
                            SubtaskItem(title = "Add budget", done = false),
                            SubtaskItem(title = "Send to team", done = false)
                        )
                    )
                ),
                // 2. Morning workout (Matches Screenshot 1!)
                TaskEntity(
                    title = "Morning workout",
                    notes = "30 min cardio and core workout to kickstart the day.",
                    category = "Health",
                    priority = "Medium",
                    dueDate = today,
                    dueTime = "7:00 AM",
                    reminder = "15 min before",
                    done = false,
                    subtasksJson = TaskEntity.serializeSubtasks(
                        listOf(
                            SubtaskItem(title = "Warmup stretching", done = true),
                            SubtaskItem(title = "15 min HIIT run", done = false),
                            SubtaskItem(title = "Core plank circuit", done = false)
                        )
                    )
                ),
                // 3. Buy groceries (Matches Screenshot 1!)
                TaskEntity(
                    title = "Buy groceries",
                    notes = "Weekly grocery run at the farmers market and supermarket.",
                    category = "Personal",
                    priority = "Low",
                    dueDate = today,
                    dueTime = "4:00 PM",
                    reminder = "1 hour before",
                    done = false,
                    subtasksJson = TaskEntity.serializeSubtasks(
                        listOf(
                            SubtaskItem(title = "Oat milk & organic eggs", done = false),
                            SubtaskItem(title = "Sourdough loaf", done = false),
                            SubtaskItem(title = "Avocados and spinach", done = false)
                        )
                    )
                ),
                // 4. Read a book (Matches Screenshot 1!)
                TaskEntity(
                    title = "Read a book",
                    notes = "Read 20 pages of chapter 4 in Design Systems handbook.",
                    category = "Personal",
                    priority = "Low",
                    dueDate = today,
                    dueTime = "9:00 PM",
                    reminder = "None",
                    done = false,
                    subtasksJson = TaskEntity.serializeSubtasks(
                        listOf(
                            SubtaskItem(title = "Take chapter notes", done = false)
                        )
                    )
                ),
                // 5. Study Compose architecture (Pending)
                TaskEntity(
                    title = "Study Compose M3 animations",
                    notes = "Review state hoisting and custom canvas graphics.",
                    category = "Study",
                    priority = "Medium",
                    dueDate = today,
                    dueTime = "2:00 PM",
                    reminder = "15 min before",
                    done = false,
                    subtasksJson = TaskEntity.serializeSubtasks(
                        listOf(
                            SubtaskItem(title = "Watch Android Dev Summit clip", done = true),
                            SubtaskItem(title = "Implement prototype", done = false)
                        )
                    )
                ),
                // 6. Brainstorm brand identity (Pending)
                TaskEntity(
                    title = "Brainstorm color palettes",
                    notes = "Pick high contrast warm retro colors for next release.",
                    category = "Ideas",
                    priority = "Medium",
                    dueDate = today,
                    dueTime = "5:30 PM",
                    reminder = "None",
                    done = false,
                    subtasksJson = TaskEntity.serializeSubtasks(
                        listOf(
                            SubtaskItem(title = "Collect moodboard screenshots", done = true),
                            SubtaskItem(title = "Export hex swatches", done = false)
                        )
                    )
                ),
                // 11 More completed tasks to reach 12 Done, 5 Pending (exactly matching Done 12 / Pending 5 in mockup!)
                TaskEntity(
                    title = "Check morning emails",
                    notes = "Inbox zero accomplished",
                    category = "Work",
                    priority = "Medium",
                    dueDate = today,
                    dueTime = "8:30 AM",
                    reminder = "None",
                    done = true
                ),
                TaskEntity(
                    title = "10-minute mindful meditation",
                    notes = "Calm breathing session",
                    category = "Health",
                    priority = "Low",
                    dueDate = today,
                    dueTime = "7:45 AM",
                    reminder = "None",
                    done = true
                ),
                TaskEntity(
                    title = "Drink 1L water",
                    notes = "Hydration target",
                    category = "Health",
                    priority = "Low",
                    dueDate = today,
                    dueTime = "8:00 AM",
                    reminder = "None",
                    done = true
                ),
                TaskEntity(
                    title = "Order mechanical keyboard caps",
                    notes = "Shopping order placed",
                    category = "Shopping",
                    priority = "Low",
                    dueDate = today,
                    dueTime = "9:15 AM",
                    reminder = "None",
                    done = true
                ),
                TaskEntity(
                    title = "Sync with engineering lead",
                    notes = "Sprint sync finished",
                    category = "Work",
                    priority = "High",
                    dueDate = today,
                    dueTime = "9:30 AM",
                    reminder = "15 min before",
                    done = true
                ),
                TaskEntity(
                    title = "Review pull request #42",
                    notes = "Approved and merged",
                    category = "Work",
                    priority = "High",
                    dueDate = today,
                    dueTime = "10:30 AM",
                    reminder = "None",
                    done = true
                ),
                TaskEntity(
                    title = "Post weekly team update",
                    notes = "Slack status shared",
                    category = "Work",
                    priority = "Medium",
                    dueDate = today,
                    dueTime = "11:00 AM",
                    reminder = "None",
                    done = true
                ),
                TaskEntity(
                    title = "Healthy salad lunch",
                    notes = "Nutritious meal",
                    category = "Health",
                    priority = "Low",
                    dueDate = today,
                    dueTime = "12:30 PM",
                    reminder = "None",
                    done = true
                ),
                TaskEntity(
                    title = "Call landlord re: package delivery",
                    notes = "Resolved and confirmed",
                    category = "Personal",
                    priority = "Low",
                    dueDate = today,
                    dueTime = "1:00 PM",
                    reminder = "None",
                    done = true
                ),
                TaskEntity(
                    title = "Sketch app icon concepts",
                    notes = "Vector outlines exported",
                    category = "Ideas",
                    priority = "Medium",
                    dueDate = today,
                    dueTime = "1:30 PM",
                    reminder = "None",
                    done = true
                ),
                TaskEntity(
                    title = "Review study flashcards",
                    notes = "30 cards practiced",
                    category = "Study",
                    priority = "Medium",
                    dueDate = today,
                    dueTime = "1:45 PM",
                    reminder = "None",
                    done = true
                )
            )

            dao.insertAll(initialTasks)
        }
    }
}
