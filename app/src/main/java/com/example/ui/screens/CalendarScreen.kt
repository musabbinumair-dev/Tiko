package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.musab.tiko.R
import com.example.data.local.TikoDatabase
import com.example.data.model.TaskEntity
import com.example.ui.components.AppBlack
import com.example.ui.components.AppBottomNav
import com.example.ui.components.EmptyState
import com.example.ui.components.AppCreamBackground
import com.example.ui.components.ArrowOutwardIcon
import com.example.ui.components.CardBlack
import com.example.ui.components.CardLime
import com.example.ui.components.CardPurple
import com.example.ui.components.CardYellow
import com.example.ui.components.FrostedGlassPill
import com.example.ui.components.RoundIconButton
import com.example.ui.components.frostedGlass
import com.example.ui.components.pressable
import com.example.ui.components.screenTopPadding
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class CalendarTaskCardData(
    val id: String,
    val title: String,
    val time: String,
    val category: String,
    val categoryIcon: ImageVector,
    val backgroundColor: Color,
    val contentColor: Color
)

private data class CalendarDayCell(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val dateString: String
)

@Composable
fun CalendarScreen(
    viewModel: TikoViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware/gesture back press to navigate to Home
    BackHandler {
        viewModel.selectTab(NavigationTab.HOME)
    }

    val tasks by viewModel.allTasks.collectAsState()
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()

    // Default to the month of the selected date or system current month (October 2026)
    var displayedCalendar by remember {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(selectedDate)
            if (d != null) cal.time = d
        } catch (_: Exception) {}
        mutableStateOf(cal)
    }

    val todayDateString = remember { TikoDatabase.getTodayDateString() }

    // Selected day tasks
    val dayTasks = tasks.filter {
        it.dueDate == selectedDate || (selectedDate == todayDateString && (it.dueDate.isBlank() || it.dueDate == selectedDate))
    }

    // Default cards to match the mockup 100% when selected date matches mockup demo
    val displayCards = remember(dayTasks, selectedDate) {
        val palette = listOf(
            CardPurple to Color.White,
            CardYellow to AppBlack,
            CardBlack to Color.White,
            CardLime to AppBlack
        )
        dayTasks.mapIndexed { index, task ->
            val (bg, content) = palette[index % palette.size]
            val icon = when (task.category.lowercase()) {
                "work" -> Icons.Outlined.Work
                "health" -> Icons.Outlined.FitnessCenter
                "study" -> Icons.Outlined.MenuBook
                else -> Icons.Outlined.Person
            }
            CalendarTaskCardData(
                id = task.id,
                title = task.title,
                time = if (task.dueTime.isNotBlank()) task.dueTime else "10:00 AM",
                category = task.category.ifBlank { "Personal" },
                categoryIcon = icon,
                backgroundColor = bg,
                contentColor = content
            )
        }
    }

    // Formatted selected date pill label (e.g. "Tuesday, Oct 27")
    val selectedDateFormatted = remember(selectedDate) {
        try {
            val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outSdf = SimpleDateFormat("EEEE, MMM d", Locale.US)
            val parsed = inSdf.parse(selectedDate)
            if (parsed != null) outSdf.format(parsed) else "Tuesday, Oct 27"
        } catch (_: Exception) {
            "Tuesday, Oct 27"
        }
    }

    // Month text display (e.g. "October 2026")
    val monthTitle = remember(displayedCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(displayedCalendar.time)
    }

    // Compute month grid days (previous month padding + current month days)
    val gridWeeks = remember(displayedCalendar) {
        val weeks = mutableListOf<List<CalendarDayCell>>()
        val cal = (displayedCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ...
        val daysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month calculation
        val prevCal = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevDaysCount = firstDayOfWeek - 1 // Days from prev month to prepend

        val allCells = mutableListOf<CalendarDayCell>()

        // 1. Prepend days from previous month
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in (daysInPrevMonth - prevDaysCount + 1)..daysInPrevMonth) {
            val dCal = (prevCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, i)
            }
            allCells.add(CalendarDayCell(i, false, sdf.format(dCal.time)))
        }

        // 2. Add current month days
        for (i in 1..daysInCurrentMonth) {
            val dCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, i)
            }
            allCells.add(CalendarDayCell(i, true, sdf.format(dCal.time)))
        }

        // 3. Append days from next month to complete the row
        val remaining = 7 - (allCells.size % 7)
        if (remaining in 1..6) {
            val nextCal = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            for (i in 1..remaining) {
                val dCal = (nextCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, i)
                }
                allCells.add(CalendarDayCell(i, false, sdf.format(dCal.time)))
            }
        }

        // Chunk into 7-day weeks
        allCells.chunked(7).forEach { week ->
            weeks.add(week)
        }
        weeks
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppCreamBackground)
    ) {
        // Header warm gradient #FFCF4D fading to cream behind the top area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFFFFCF4D),
                        0.45f to Color(0xFFFFDB78).copy(alpha = 0.5f),
                        1.0f to AppCreamBackground
                    )
                )
        )

        // Main scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("calendar_screen_scroll"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // TOP ROW: Back button (left), "Today" button (right)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .screenTopPadding()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundIconButton(
                        icon = Icons.Outlined.ChevronLeft,
                        onClick = { viewModel.selectTab(NavigationTab.HOME) },
                        contentDescription = "Back",
                        testTag = "calendar_back_button"
                    )

                    // "Today" button pill: frosted glassmorphic glassy pill, 48dp height
                    FrostedGlassPill(
                        modifier = Modifier.height(48.dp),
                        onClick = {
                            val now = Calendar.getInstance()
                            displayedCalendar = now
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
                            viewModel.selectCalendarDate(todayStr)
                        },
                        testTag = "calendar_today_button"
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = AppBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = AppBlack
                            )
                        }
                    }
                }
            }

            // TITLE: "CALENDAR" Outfit Black, 44sp, uppercase, black, letter spacing -1sp
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CALENDAR",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 44.sp,
                    letterSpacing = (-1).sp,
                    color = AppBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
            }

            // MONTH ROW: Prev button, Frosted pill "October 2026", Next button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev Month Button: 40dp frosted circle
                    FrostedGlassPill(
                        modifier = Modifier.size(40.dp),
                        onClick = {
                            displayedCalendar = (displayedCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                        },
                        testTag = "calendar_prev_month_button"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = AppBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Month Name Pill: frosted glassmorphic pill, height 40dp
                    FrostedGlassPill(
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = monthTitle,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppBlack
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Next Month Button: 40dp frosted circle
                    FrostedGlassPill(
                        modifier = Modifier.size(40.dp),
                        onClick = {
                            displayedCalendar = (displayedCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                        },
                        testTag = "calendar_next_month_button"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Next Month",
                            tint = AppBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // MONTH GRID CARD: White card, 28dp radius, weekday letters, bold date numbers
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Weekday Row: S, M, T, W, T, F, S
                        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekdays.forEach { dayLetter ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLetter,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AppBlack
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Days Grid Rows
                        gridWeeks.forEachIndexed { weekIndex, week ->
                            if (weekIndex > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                week.forEach { cell ->
                                    val isSelected = cell.dateString == selectedDate
                                    val isToday = cell.dateString == todayDateString
                                    val hasTasks = tasks.any { it.dueDate == cell.dateString }

                                    // Dot color: show ONLY on days that have at least one task in the database
                                    val dotColor = if (hasTasks) {
                                        val t = tasks.firstOrNull { it.dueDate == cell.dateString }
                                        when (t?.category?.lowercase()) {
                                            "work" -> CardPurple
                                            "health" -> CardYellow
                                            "study" -> CardLime
                                            else -> Color(0xFFFF5A5F)
                                        }
                                    } else {
                                        null
                                    }

                                    CalendarDayItem(
                                        cell = cell,
                                        isSelected = isSelected,
                                        isToday = isToday,
                                        dotColor = dotColor,
                                        onClick = {
                                            viewModel.selectCalendarDate(cell.dateString)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SELECTED DATE PILL LABEL: (e.g. "Tuesday, Oct 27")
            item {
                Spacer(modifier = Modifier.height(18.dp))
                FrostedGlassPill(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .height(38.dp)
                ) {
                    Text(
                        text = selectedDateFormatted,
                        modifier = Modifier.padding(horizontal = 18.dp),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppBlack
                    )
                }
            }

            // STACKED COLOR CARDS FOR THAT DAY'S TASKS
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    CalendarOverlappingCardsList(
                        cards = displayCards,
                        onCardClick = { taskId ->
                            onNavigate(Screen.TaskDetail(taskId))
                        }
                    )
                }
            }
        }

        // Soft fade scrim behind the bar (90dp gradient, transparent to cream #EFEBDD at 80%)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFEFEBDD).copy(alpha = 0.80f)
                        )
                    )
                )
        )
    }
}

/**
 * Single Day Cell in the Month Grid
 */
@Composable
private fun CalendarDayItem(
    cell: CalendarDayCell,
    isSelected: Boolean,
    isToday: Boolean,
    dotColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick)
            .testTag("cal_day_${cell.dayNumber}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        when {
                            isSelected -> Modifier.background(AppBlack)
                            isToday -> Modifier.border(2.dp, CardPurple, CircleShape)
                            else -> Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = cell.dayNumber.toString(),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = when {
                            isSelected -> Color.White
                            cell.isCurrentMonth -> AppBlack
                            else -> Color(0xFF9E9E9E)
                        }
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(CardLime)
                        )
                    }
                }
            }

            if (!isSelected && dotColor != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

/**
 * Stacked overlapping task cards matching Home and Lists
 */
@Composable
private fun CalendarOverlappingCardsList(
    cards: List<CalendarTaskCardData>,
    onCardClick: (String) -> Unit
) {
    if (cards.isEmpty()) {
        EmptyState(
            image = R.drawable.illus_no_events,
            title = "Nothing planned",
            caption = "A free day. Tap + to add something."
        )
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            cards.forEachIndexed { index, card ->
                val isLast = index == cards.lastIndex
                SingleCalendarOverlappingCard(
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
private fun SingleCalendarOverlappingCard(
    card: CalendarTaskCardData,
    isOverlapped: Boolean,
    zIndex: Float,
    onClick: () -> Unit
) {
    val cardHeight = 134.dp
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
            .testTag("calendar_card_${card.id}")
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
                            tint = AppBlack,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = card.category,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = AppBlack
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
                        color = AppBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title: Outfit Bold 26sp
            Text(
                text = card.title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = card.contentColor,
                lineHeight = 30.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Time: Outfit Regular 15sp
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
