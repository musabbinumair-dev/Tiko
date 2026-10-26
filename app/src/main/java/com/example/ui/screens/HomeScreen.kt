package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TaskEntity
import com.example.ui.components.CurlyLoopDoodle
import com.example.ui.components.HollowTriangleDoodle
import com.example.ui.components.LargeArcDoodle
import com.example.ui.components.LargeGridCircleDoodle
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCheckbox
import com.example.ui.components.SmallUpTriangleDoodle
import com.example.ui.components.TikoCategoryIcon
import com.example.ui.components.getCategoryColor
import com.example.ui.theme.BricolageGrotesque
import com.example.ui.theme.DMSans
import com.example.ui.theme.TikoCardFill
import com.example.ui.theme.TikoCoral
import com.example.ui.theme.TikoGreen
import com.example.ui.theme.TikoMustard
import com.example.ui.theme.TikoOutline
import com.example.ui.theme.TikoText
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel
import com.example.ui.viewmodel.TodayStats

@Composable
fun HomeScreen(
    viewModel: TikoViewModel,
    tasks: List<TaskEntity>,
    stats: TodayStats,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTasks = tasks

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Top App Bar: Brand Logo & User Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tiko",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.ExtraBold, // 800
                    fontSize = 44.sp,
                    letterSpacing = (-1).sp,
                    color = TikoText
                ),
                modifier = Modifier.testTag("tiko_brand_logo")
            )

            // User Avatar with circular 2dp black border
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, TikoOutline, CircleShape)
                    .background(TikoMustard),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tiko_avatar),
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Today Header
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.ExtraBold, // 800
                            fontSize = 42.sp,
                            lineHeight = 44.sp,
                            letterSpacing = (-1).sp,
                            color = TikoText
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stats.pending} tasks left",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = DMSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = TikoCoral // Coral subtitle on blush background as requested
                        )
                    )
                }
            }

            item {
                // Three touching stats tiles: Done, Pending, Streak (~160dp tall, 2dp black border)
                HomeStatsTiles(stats = stats)
            }

            item {
                // Section Title: "Your tasks"
                Text(
                    text = "Your tasks",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold, // 800
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp,
                        color = TikoText
                    ),
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            }

            if (displayTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
                            .background(TikoCardFill)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks yet! Tap '+ Add task' below to start.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = DMSans,
                                color = TikoText
                            )
                        )
                    }
                }
            } else {
                items(displayTasks, key = { it.id }) { task ->
                    HomeTaskCard(
                        task = task,
                        onClick = { onNavigate(Screen.TaskDetail(task.id)) },
                        onToggle = { viewModel.toggleTaskDone(task) }
                    )
                }
            }
        }

        // Bottom Action Button: "+ Add task" (Black, 2dp border, 0dp corner radius)
        NeoButton(
            onClick = { onNavigate(Screen.AddTask) },
            text = "+ Add task",
            isPrimary = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("add_task_button")
        )
    }
}

// Three touching tiles (160dp tall, 2dp black border) with clean solid backgrounds
@Composable
private fun HomeStatsTiles(stats: TodayStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
            .clipToBounds()
    ) {
        // Tile 1: Done (Green #5DB36A)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(TikoGreen)
                .border(width = 1.dp, color = TikoOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TikoText
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stats.done.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold, // 800
                        fontSize = 44.sp,
                        color = TikoText
                    )
                )
            }
        }

        // Tile 2: Pending (Mustard #EFA93B)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(TikoMustard)
                .border(width = 1.dp, color = TikoOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Pending",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TikoText
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stats.pending.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold, // 800
                        fontSize = 44.sp,
                        color = TikoText
                    )
                )
            }
        }

        // Tile 3: Streak (Coral #E8624A)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(TikoCoral)
                .border(width = 1.dp, color = TikoOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Streak",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DMSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TikoText
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stats.streak.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold, // 800
                        fontSize = 44.sp,
                        color = TikoText
                    )
                )
            }
        }
    }
}

// Single Task Card matching Mockup 1 exactly
@Composable
fun HomeTaskCard(
    task: TaskEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = getCategoryColor(task.category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .border(2.dp, TikoOutline, RoundedCornerShape(0.dp))
            .background(TikoCardFill, RoundedCornerShape(0.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("task_item_${task.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left solid colored square block with 2dp right border
        Box(
            modifier = Modifier
                .size(78.dp)
                .background(categoryColor)
                .border(width = 2.dp, color = TikoOutline),
            contentAlignment = Alignment.Center
        ) {
            TikoCategoryIcon(
                category = task.category,
                modifier = Modifier.size(38.dp),
                color = TikoText,
                strokeWidth = 2.dp
            )
        }

        // Middle task title and metadata on blush background
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold, // 700
                    fontSize = 17.sp,
                    color = TikoText
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${task.dueTime} • ${task.category}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = DMSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = TikoText
                ),
                maxLines = 1
            )
        }

        // Right Square Checkbox
        Box(modifier = Modifier.padding(end = 16.dp)) {
            NeoCheckbox(
                checked = task.done,
                onCheckedChange = { onToggle() },
                size = 28.dp,
                modifier = Modifier.testTag("checkbox_${task.id}")
            )
        }
    }
}

// TaskCard alias for Lists and Calendar screens
@Composable
fun TaskCard(
    task: TaskEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeTaskCard(task = task, onClick = onClick, onToggle = onToggle, modifier = modifier)
}


