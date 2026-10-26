package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TikoDatabase
import com.example.ui.components.NeoButton
import com.example.ui.theme.BricolageGrotesque
import com.example.ui.theme.DMSans
import com.example.ui.theme.TikoCoral
import com.example.ui.theme.TikoGreen
import com.example.ui.theme.TikoMustard
import com.example.ui.theme.TikoOutline
import com.example.ui.theme.TikoSurface
import com.example.ui.theme.TikoText
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: TikoViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsState()
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()

    // Generate days around current date
    val calendarDays = remember {
        val days = mutableListOf<Pair<String, Pair<String, String>>>() // dateStr, (dayOfWeek, dayNum)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3) // 3 days past
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeekSdf = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumSdf = SimpleDateFormat("dd", Locale.getDefault())

        for (i in 0..14) {
            val d = cal.time
            days.add(
                dateSdf.format(d) to (dayOfWeekSdf.format(d).uppercase() to dayNumSdf.format(d))
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        days
    }

    val dayTasks = tasks.filter {
        it.dueDate == selectedDate || (selectedDate == TikoDatabase.getTodayDateString() && (it.dueDate.isBlank() || it.dueDate == selectedDate))
    }
    val doneCount = dayTasks.count { it.done }
    val pendingCount = dayTasks.count { !it.done }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Screen Header
        Text(
            text = "Calendar",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                color = TikoText
            ),
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        Text(
            text = "Schedule & daily timeline",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = DMSans,
                color = TikoText.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Horizontal Days Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            calendarDays.forEach { (dateStr, display) ->
                val (dayOfWeek, dayNum) = display
                val isSelected = selectedDate == dateStr
                val isToday = dateStr == TikoDatabase.getTodayDateString()

                Box(
                    modifier = Modifier
                        .width(62.dp)
                        .height(84.dp)
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(
                            when {
                                isSelected -> TikoOutline
                                isToday -> TikoMustard
                                else -> TikoSurface
                            }
                        )
                        .clickable { viewModel.selectCalendarDate(dateStr) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dayOfWeek,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TikoText
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayNum,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = if (isSelected) Color.White else TikoText
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Day Summary Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                .background(TikoSurface)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (selectedDate == TikoDatabase.getTodayDateString()) "Today" else selectedDate,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        color = TikoText
                    )
                )
                Text(
                    text = "$pendingCount pending • $doneCount completed",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = DMSans)
                )
            }

            Box(
                modifier = Modifier
                    .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                    .background(if (pendingCount == 0 && doneCount > 0) TikoGreen else TikoMustard)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (pendingCount == 0 && doneCount > 0) "All Done!" else "$pendingCount Left",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Bold,
                        color = TikoText
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Task Items for this day
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (dayTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                            .background(TikoSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks scheduled for this day.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DMSans)
                        )
                    }
                }
            } else {
                items(dayTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onNavigate(Screen.TaskDetail(task.id)) },
                        onToggle = { viewModel.toggleTaskDone(task) }
                    )
                }
            }
        }

        NeoButton(
            onClick = { onNavigate(Screen.AddTask) },
            text = "+ Add task for this day",
            isPrimary = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}
