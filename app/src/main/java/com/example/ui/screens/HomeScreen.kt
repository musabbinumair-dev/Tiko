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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import com.example.R
import com.example.data.model.TaskEntity
import com.example.ui.components.AppBottomNav
import com.example.ui.components.ArrowOutwardIcon
import com.example.ui.components.RoundIconButton
import com.example.ui.components.screenTopPadding
import com.example.ui.components.NeoCheckbox
import com.example.ui.components.TikoCategoryIcon
import com.example.ui.components.getCategoryColor
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TikoCardFill
import com.example.ui.theme.TikoOutline
import com.example.ui.theme.TikoText
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel
import com.example.ui.viewmodel.TodayStats

// Specific palette for restyled Home design
private val HomeCreamBackground = Color(0xFFEFEBDD)
private val HomeBlack = Color(0xFF151515)
private val HomeGray = Color(0xFF6B6B6B)
private val HomeOutlineGray = Color(0xFFBDBDBD)
private val CardPurple = Color(0xFF6B4EFF)
private val CardYellow = Color(0xFFFFB92E)
private val CardBlack = Color(0xFF151515)
private val CardLime = Color(0xFFC8DB6A)

private data class HomeCardData(
    val id: Long,
    val title: String,
    val time: String,
    val category: String,
    val categoryIcon: ImageVector,
    val subtitle: String? = null,
    val backgroundColor: Color,
    val contentColor: Color,
    val isLastTaller: Boolean = false
)

@Composable
fun HomeScreen(
    viewModel: TikoViewModel,
    tasks: List<TaskEntity>,
    stats: TodayStats,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    // Map real tasks to card data without mock fallbacks
    val cardModels = remember(tasks) {
        val palette = listOf(
            CardPurple to Color.White,
            CardYellow to HomeBlack,
            CardBlack to Color.White,
            CardLime to HomeBlack
        )
        tasks.mapIndexed { index, task ->
            val (bg, content) = palette[index % palette.size]
            val icon = when (task.category.lowercase()) {
                "work" -> Icons.Outlined.Work
                "health" -> Icons.Outlined.FitnessCenter
                "study" -> Icons.Outlined.MenuBook
                else -> Icons.Outlined.Person
            }
            HomeCardData(
                id = task.id,
                title = task.title,
                time = if (task.dueTime.isNotBlank()) task.dueTime else "10:00 AM",
                category = task.category.ifBlank { "Personal" },
                categoryIcon = icon,
                backgroundColor = bg,
                contentColor = content,
                isLastTaller = (index == tasks.lastIndex)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeCreamBackground)
    ) {
        // Main scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 120.dp // Allows full list to scroll above floating bottom nav
            )
        ) {
            // 1. Header (status bar + top spacing handled by screenTopPadding)
            item {
                HomeHeader(
                    viewModel = viewModel,
                    onSearchClick = { onNavigate(Screen.Main(NavigationTab.LISTS)) }
                )
            }

            // 2. Title Row (16dp below header)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HomeTitleRow(
                    onFilterClick = { onNavigate(Screen.Main(NavigationTab.LISTS)) }
                )
            }

            // 3. Stat Pills (14dp below title)
            item {
                Spacer(modifier = Modifier.height(14.dp))
                HomeStatPills(stats = stats)
            }

            // 4. Overlapping Task Cards (24dp below pills)
            item {
                Spacer(modifier = Modifier.height(24.dp))
                OverlappingTaskCardsList(
                    cards = cardModels,
                    onCardClick = { taskId -> onNavigate(Screen.TaskDetail(taskId)) }
                )
            }
        }

        // Soft fade scrim behind the bar (90dp gradient, transparent to cream #EFEBDD at 80%)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFEFEBDD).copy(alpha = 0.80f)
                        )
                    )
                )
        )

        // Floating Bottom Navigation Bar (16dp side margin, 12dp from bottom above system inset)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            AppBottomNav(
                activeTab = NavigationTab.HOME,
                onTabSelected = { viewModel.selectTab(it) },
                onAddClick = { onNavigate(Screen.AddTask) }
            )
        }
    }
}

// HEADER
@Composable
private fun HomeHeader(
    viewModel: TikoViewModel,
    onSearchClick: () -> Unit
) {
    val preferences = viewModel.preferences
    val userName by preferences.userName.collectAsState()
    val displayName = if (userName.isNotBlank()) userName else "User"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .screenTopPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Avatar + Name + Greeting
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.example.ui.components.UserInitialsAvatar(
                name = userName,
                size = 52.dp,
                backgroundColor = CardYellow,
                textColor = HomeBlack
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = displayName,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = HomeBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Good morning",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = HomeGray
                )
            }
        }

        // Right: 48dp circle, white fill, 1dp #151515 outline, search icon 24dp
        RoundIconButton(
            icon = Icons.Outlined.Search,
            onClick = onSearchClick,
            contentDescription = "Search",
            testTag = "home_search_button"
        )
    }
}

// TITLE ROW: "MY TASKS" + "Filters" pill
@Composable
private fun HomeTitleRow(onFilterClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MY TASKS",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 44.sp,
            lineHeight = 44.sp,
            letterSpacing = (-1).sp,
            color = HomeBlack,
            modifier = Modifier.testTag("home_title_my_tasks")
        )

        // Filters pill: white fill, 1dp black outline, height 40dp, tune icon + Outfit Medium 16sp
        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, HomeBlack, CircleShape)
                .clickable(onClick = onFilterClick)
                .padding(horizontal = 16.dp)
                .testTag("home_filters_pill"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filters",
                    tint = HomeBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Filters",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = HomeBlack
                )
            }
        }
    }
}

// STAT PILLS: 3 pills equal gap 8dp, height 40dp, white fill, 1dp black outline, fully rounded
@Composable
private fun HomeStatPills(stats: TodayStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pill 1: 12 Done (filled black circle check)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, HomeBlack, CircleShape)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = HomeBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${stats.done} Done",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = HomeBlack
                )
            }
        }

        // Pill 2: Pending (clock)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, HomeBlack, CircleShape)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = HomeBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${stats.pending} Pending",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = HomeBlack
                )
            }
        }

        // Pill 3: Day Streak (flame) - one line
        Box(
            modifier = Modifier
                .weight(1.15f)
                .height(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, HomeBlack, CircleShape)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = HomeBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${stats.streak} day streak",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = HomeBlack
                )
            }
        }
    }
}

// STACKED OVERLAPPING TASK CARDS
@Composable
private fun OverlappingTaskCardsList(
    cards: List<HomeCardData>,
    onCardClick: (Long) -> Unit
) {
    if (cards.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CardLime)
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start here",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = HomeBlack
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        ArrowOutwardIcon(size = 22.dp, color = HomeBlack)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No tasks yet",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    color = HomeBlack
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap + to add your first task",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = HomeBlack
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            cards.forEachIndexed { index, card ->
                val isLast = index == cards.lastIndex
                SingleOverlappingCard(
                    card = card,
                    isOverlapped = !isLast,
                    zIndex = index.toFloat(),
                    onClick = { onCardClick(card.id) }
                )
            }
        }
    }
}

@Composable
private fun SingleOverlappingCard(
    card: HomeCardData,
    isOverlapped: Boolean,
    zIndex: Float,
    onClick: () -> Unit
) {
    val cardHeight = if (card.isLastTaller) 190.dp else 134.dp
    val overlapPx = 18.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(zIndex)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val reportedHeight = if (isOverlapped) {
                    placeable.height - overlapPx.roundToPx()
                } else {
                    placeable.height
                }
                layout(placeable.width, reportedHeight) {
                    placeable.placeRelative(0, 0)
                }
            }
            .height(cardHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(card.backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("home_card_${card.id}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Row: Category tag (left) + Arrow outward circle (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // White pill tag: height 32dp, icon 18dp + Outfit Medium 14sp black
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = card.categoryIcon,
                            contentDescription = card.category,
                            tint = HomeBlack,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = card.category,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = HomeBlack
                        )
                    }
                }

                // 44dp white circle with black arrow_outward icon 22dp
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    ArrowOutwardIcon(
                        size = 22.dp,
                        color = HomeBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title: Outfit Bold 28sp
            Text(
                text = card.title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                color = card.contentColor
            )

            if (card.subtitle != null) {
                // Subtitle: e.g. "Chapter 5 of 12" Outfit Regular 16sp
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.subtitle,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = card.contentColor
                )
            }

            if (card.isLastTaller) {
                Spacer(modifier = Modifier.weight(1f))
                // Bottom row for last card: clock icon + "9:00 PM"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Time",
                        tint = card.contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.time,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = card.contentColor
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                // Time: Outfit Regular 15sp, same color as title
                Text(
                    text = card.time,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = card.contentColor
                )
            }
        }
    }
}



// TaskCard alias for Lists and Calendar screens compatibility
@Composable
fun TaskCard(
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

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = task.title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TikoText
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${task.dueTime} • ${task.category}",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = TikoText
            )
        }

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

@Composable
fun HomeTaskCard(
    task: TaskEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    TaskCard(task = task, onClick = onClick, onToggle = onToggle, modifier = modifier)
}
