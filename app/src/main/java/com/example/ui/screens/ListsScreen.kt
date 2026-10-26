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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategories
import com.example.data.model.TaskEntity
import com.example.ui.components.NeoButton
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BricolageGrotesque
import com.example.ui.theme.DMSans
import com.example.ui.theme.TikoOutline
import com.example.ui.theme.TikoSurface
import com.example.ui.theme.TikoText
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel

@Composable
fun ListsScreen(
    viewModel: TikoViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var filterStatus by remember { mutableStateOf("All") } // All, Active, Completed

    val allCategories = listOf("All") + TaskCategories.ALL

    val filteredTasks = tasks.filter { task ->
        val matchesCategory = selectedCategory == null || selectedCategory == "All" || task.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true) || task.notes.contains(searchQuery, ignoreCase = true)
        val matchesStatus = when (filterStatus) {
            "Active" -> !task.done
            "Completed" -> task.done
            else -> true
        }
        matchesCategory && matchesSearch && matchesStatus
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Screen Header
        Text(
            text = "Lists",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                color = TikoText
            ),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search tasks or notes...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TikoText
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TikoText)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("search_input"),
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TikoOutline,
                unfocusedBorderColor = TikoOutline,
                focusedContainerColor = TikoSurface,
                unfocusedContainerColor = TikoSurface
            )
        )

        // Category Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allCategories.forEach { category ->
                val isSelected = selectedCategory == category
                val catColor = if (category == "All") TikoText else getCategoryColor(category)

                Box(
                    modifier = Modifier
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(
                            if (isSelected) TikoOutline else TikoSurface,
                            RoundedCornerShape(0.dp)
                        )
                        .clickable { viewModel.selectCategory(category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (category != "All") {
                            Icon(
                                imageVector = getCategoryIcon(category),
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TikoText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = DMSans,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TikoText
                            )
                        )
                    }
                }
            }
        }

        // Status Filter Chips (All, Active, Completed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Active", "Completed").forEach { status ->
                val isSelected = filterStatus == status
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                        .background(if (isSelected) TikoOutline else TikoSurface)
                        .clickable { filterStatus = status }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TikoText
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tasks Count Subtitle
        Text(
            text = "${filteredTasks.size} tasks found",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = DMSans,
                color = TikoText.copy(alpha = 0.7f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Task Items List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredTasks.isEmpty()) {
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
                            text = "No matching tasks found.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DMSans)
                        )
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onNavigate(Screen.TaskDetail(task.id)) },
                        onToggle = { viewModel.toggleTaskDone(task) }
                    )
                }
            }
        }

        // Quick Add Button
        NeoButton(
            onClick = { onNavigate(Screen.AddTask) },
            text = "+ Add new task",
            isPrimary = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}
