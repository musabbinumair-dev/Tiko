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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import com.musab.tiko.R
import com.example.data.model.TaskEntity
import com.example.ui.components.AnimatedStreakEmoji
import com.example.ui.components.AppBottomNav
import com.example.ui.components.ArrowOutwardIcon
import com.example.ui.components.EmptyState
import com.example.ui.components.FrostedGlassPill
import com.example.ui.components.RoundIconButton
import com.example.ui.components.screenTopPadding
import com.example.ui.components.pressable
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
    val id: String,
    val title: String,
    val time: String,
    val category: String,
    val categoryIcon: ImageVector,
    val subtitle: String? = null,
    val backgroundColor: Color,
    val contentColor: Color,
    val isLastTaller: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: TikoViewModel,
    tasks: List<TaskEntity>,
    stats: TodayStats,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    // Map active tasks to card data without mock fallbacks
    val cardModels = remember(tasks) {
        val palette = listOf(
            CardPurple to Color.White,
            CardYellow to HomeBlack,
            CardBlack to Color.White,
            CardLime to HomeBlack
        )
        val activeTasks = tasks.filter { !it.done }
        activeTasks.mapIndexed { index, task ->
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
                isLastTaller = (index == activeTasks.lastIndex)
            )
        }
    }

    val tikoColors = com.example.ui.theme.LocalTikoThemeColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tikoColors.screenBg)
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
            // 1. Header
            item(key = "header") {
                HomeHeader(
                    viewModel = viewModel,
                    onSearchClick = { onNavigate(Screen.Main(NavigationTab.LISTS)) },
                    isDark = isDark
                )
            }

            // 2. Title Row
            item(key = "title_row") {
                Spacer(modifier = Modifier.height(16.dp))
                HomeTitleRow(
                    onFilterClick = { onNavigate(Screen.Main(NavigationTab.LISTS)) },
                    onFilterLongClick = { onNavigate(Screen.NotFound) },
                    isDark = isDark
                )
            }

            // 3. Stat Pills
            item(key = "stat_pills") {
                Spacer(modifier = Modifier.height(14.dp))
                HomeStatPills(stats = stats, isDark = isDark)
            }

            // 4. Task cards — one item per task with stable key + animateItem
            if (tasks.isEmpty()) {
                item(key = "empty_state") {
                    Spacer(modifier = Modifier.height(24.dp))
                    EmptyState(
                        image = R.drawable.illus_no_tasks,
                        title = "No tasks yet",
                        caption = "Tap below to add your first task.",
                        buttonText = "Add your first task",
                        onButton = { onNavigate(Screen.AddTask) },
                        isDark = isDark
                    )
                }
            } else if (tasks.none { !it.done }) {
                item(key = "all_done_state") {
                    Spacer(modifier = Modifier.height(24.dp))
                    EmptyState(
                        image = R.drawable.illus_all_done,
                        title = "All done!",
                        caption = "You finished everything. Enjoy the rest of your day.",
                        isDark = isDark
                    )
                }
            } else {
                item(key = "cards_spacer") { Spacer(modifier = Modifier.height(24.dp)) }
                itemsIndexed(
                    items = cardModels,
                    key = { _, card -> card.id }
                ) { index, card ->
                    val isLast = index == cardModels.lastIndex
                    SingleOverlappingCard(
                        card = card,
                        isOverlapped = !isLast,
                        zIndex = index.toFloat(),
                        onClick = { onNavigate(Screen.TaskDetail(card.id)) },
                        modifier = Modifier.animateItem(),
                        isDark = isDark
                    )
                }
            }
        }

        // Soft fade scrim behind the bar (90dp gradient, transparent to near-black or cream at 80%)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            tikoColors.scrim
                        )
                    )
                )
        )
    }
}

// HEADER
@Composable
private fun HomeHeader(
    viewModel: TikoViewModel,
    onSearchClick: () -> Unit,
    isDark: Boolean = false
) {
    val preferences = viewModel.preferences
    val userName by preferences.userName.collectAsState()
    val avatarId by preferences.avatarId.collectAsState()
    val isGuestMode by preferences.isGuestMode.collectAsState()
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
            com.example.ui.components.UserAvatar(
                name = userName,
                size = 52.dp,
                avatarId = if (isGuestMode) null else avatarId
            )

            Spacer(modifier = Modifier.width(12.dp))

            val tikoColors = com.example.ui.theme.LocalTikoThemeColors.current
            Column {
                Text(
                    text = displayName,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = tikoColors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Good morning",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = tikoColors.textSecondary
                )
            }
        }

        // Right: 48dp circle, white fill, 1dp #151515 outline, search icon 24dp
        RoundIconButton(
            icon = Icons.Outlined.Search,
            onClick = onSearchClick,
            contentDescription = "Search",
            testTag = "home_search_button",
            isDark = isDark
        )
    }
}

// TITLE ROW: "MY TASKS" + "Filters" pill
@Composable
private fun HomeTitleRow(
    onFilterClick: () -> Unit,
    onFilterLongClick: (() -> Unit)? = null,
    isDark: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tikoColors = com.example.ui.theme.LocalTikoThemeColors.current
        Text(
            text = "MY TASKS",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 44.sp,
            lineHeight = 44.sp,
            letterSpacing = (-1).sp,
            color = tikoColors.textPrimary,
            modifier = Modifier.testTag("home_title_my_tasks")
        )

        // Filters pill: frosted glassmorphic glassy pill, height 40dp, tune icon + Outfit Medium 16sp
        FrostedGlassPill(
            modifier = Modifier.height(40.dp),
            onClick = onFilterClick,
            onLongClick = onFilterLongClick,
            testTag = "home_filters_pill",
            isDark = isDark
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filters",
                    tint = if (isDark) Color(0xFFF5F1E6) else HomeBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Filters",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = if (isDark) Color(0xFFF5F1E6) else HomeBlack
                )
            }
        }
    }
}

// STAT PILLS: 3 frosted glassmorphic pills equal gap 8dp, height 40dp
@Composable
private fun HomeStatPills(stats: TodayStats, isDark: Boolean = false) {
    val tikoColors = com.example.ui.theme.LocalTikoThemeColors.current
    val pillIconTint = tikoColors.textPrimary
    val pillTextColor = tikoColors.textPrimary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pill 1: 12 Done (filled black circle check)
        FrostedGlassPill(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            isDark = isDark
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = pillIconTint,
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
                    color = pillTextColor
                )
            }
        }

        // Pill 2: Pending (clock)
        FrostedGlassPill(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            isDark = isDark
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = pillIconTint,
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
                    color = pillTextColor
                )
            }
        }

        // Pill 3: Day Streak (flame) - one line
        FrostedGlassPill(
            modifier = Modifier
                .weight(1.15f)
                .height(40.dp),
            isDark = isDark
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedStreakEmoji(
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${stats.streak} day streak",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = pillTextColor
                )
            }
        }
    }
}

// STACKED OVERLAPPING TASK CARDS — kept for reference, items now rendered directly in LazyColumn
@Composable
private fun OverlappingTaskCardsList(
    cards: List<HomeCardData>,
    onCardClick: (String) -> Unit
) {
    if (cards.isEmpty()) return
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

@Composable
private fun SingleOverlappingCard(
    card: HomeCardData,
    isOverlapped: Boolean,
    zIndex: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "card_press_scale"
    )
    val cardHeight = if (card.isLastTaller) 190.dp else 134.dp
    val overlapPx = 18.dp

    val isCardBlack = card.backgroundColor == CardBlack || card.backgroundColor == Color(0xFF151515)
    val targetBg = if (isDark && isCardBlack) Color(0xFF2B2B2B) else card.backgroundColor
    val effectiveBg by androidx.compose.animation.animateColorAsState(targetBg, androidx.compose.animation.core.tween(380), label = "card_bg")
    val borderModifier = if (isDark && isCardBlack) Modifier.border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(28.dp)) else Modifier
    val pillBg by androidx.compose.animation.animateColorAsState(if (isDark) Color(0xFF1E1E1E) else Color.White, androidx.compose.animation.core.tween(380), label = "pill_bg")
    val pillTint by androidx.compose.animation.animateColorAsState(if (isDark) Color(0xFFF5F1E6) else HomeBlack, androidx.compose.animation.core.tween(380), label = "pill_tint")

    Box(
        modifier = modifier
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
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(28.dp))
            .background(effectiveBg)
            .then(borderModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
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
                // Category pill: in dark mode becomes #1E1E1E with light text and icon
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(pillBg)
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
                            tint = pillTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = card.category,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = pillTint
                        )
                    }
                }

                // Arrow circle: in dark mode becomes #1E1E1E with light arrow
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(pillBg)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    ArrowOutwardIcon(
                        size = 22.dp,
                        color = pillTint
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

