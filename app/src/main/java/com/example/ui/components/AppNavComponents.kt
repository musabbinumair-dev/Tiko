package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.NavigationTab

val AppCreamBackground = Color(0xFFEFEBDD)
val AppBlack = Color(0xFF151515)
val AppGray = Color(0xFF6B6B6B)
val AppOutlineGray = Color(0xFFBDBDBD)
val CardPurple = Color(0xFF6B4EFF)
val CardYellow = Color(0xFFFFB92E)
val CardBlack = Color(0xFF151515)
val CardLime = Color(0xFFC8DB6A)

/**
 * Shared RoundIconButton matching Home screen's top-bar buttons:
 * 48dp circle, white fill, 1dp #151515 outline, 24dp icon.
 */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    testTag: String = "round_icon_button"
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, AppBlack, CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AppBlack,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Shared AppBottomNav:
 * Floating white pill, 20dp side margin, 14dp from bottom, height 66dp, fully rounded, very subtle shadow.
 * 5 circles, 46dp, evenly spaced:
 * Active tab has black fill #151515 with white icon.
 * Inactive tabs have white fill with 1dp #BDBDBD outline and black icon.
 */
@Composable
fun AppBottomNav(
    activeTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                spotColor = Color(0x30000000),
                ambientColor = Color(0x18000000)
            )
            .background(Color.White, CircleShape)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            NavCircleButton(
                isActive = activeTab == NavigationTab.HOME,
                icon = Icons.Filled.Home,
                contentDescription = "Home",
                onClick = { onTabSelected(NavigationTab.HOME) },
                testTag = "nav_home_button"
            )

            // 2. Lists
            NavCircleButton(
                isActive = activeTab == NavigationTab.LISTS,
                icon = Icons.Outlined.FormatListBulleted,
                contentDescription = "Lists",
                onClick = { onTabSelected(NavigationTab.LISTS) },
                testTag = "nav_list_button"
            )

            // 3. Add
            NavCircleButton(
                isActive = false,
                icon = Icons.Outlined.Add,
                contentDescription = "Add Task",
                onClick = onAddClick,
                testTag = "nav_add_button"
            )

            // 4. Calendar
            NavCircleButton(
                isActive = activeTab == NavigationTab.CALENDAR,
                icon = Icons.Outlined.CalendarToday,
                contentDescription = "Calendar",
                iconSize = 22.dp,
                onClick = { onTabSelected(NavigationTab.CALENDAR) },
                testTag = "nav_calendar_button"
            )

            // 5. Person / Settings
            NavCircleButton(
                isActive = activeTab == NavigationTab.SETTINGS,
                icon = Icons.Outlined.Person,
                contentDescription = "Profile",
                onClick = { onTabSelected(NavigationTab.SETTINGS) },
                testTag = "nav_profile_button"
            )
        }
    }
}

@Composable
private fun NavCircleButton(
    isActive: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .then(
                if (isActive) {
                    Modifier.background(AppBlack)
                } else {
                    Modifier
                        .background(Color.White)
                        .border(1.dp, AppOutlineGray, CircleShape)
                }
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.White else AppBlack,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Arrow Outward icon rendered via Canvas for crisp diagonal alignment and arrow head
 */
@Composable
fun ArrowOutwardIcon(
    size: Dp = 22.dp,
    color: Color = AppBlack,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 2.4.dp.toPx()
        val pad = 4.dp.toPx()
        val start = Offset(pad, this.size.height - pad)
        val end = Offset(this.size.width - pad, pad)

        // Diagonal main shaft
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Arrow head top horizontal barb
        val barb = 7.dp.toPx()
        drawLine(
            color = color,
            start = Offset(end.x - barb, end.y),
            end = end,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Arrow head right vertical barb
        drawLine(
            color = color,
            start = Offset(end.x, end.y + barb),
            end = end,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
