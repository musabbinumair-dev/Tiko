package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DMSans
import com.example.ui.theme.TikoBackground
import com.example.ui.theme.TikoGreen
import com.example.ui.theme.TikoOutline
import com.example.ui.theme.TikoText
import com.example.ui.viewmodel.NavigationTab

@Composable
fun TikoBottomBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(TikoBackground)
            .border(width = 2.dp, color = TikoOutline, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home
        BottomNavItem(
            label = "Home",
            isSelected = currentTab == NavigationTab.HOME,
            onClick = { onTabSelected(NavigationTab.HOME) },
            tag = "nav_home",
            icon = { isSel ->
                TikoNavHomeIcon(
                    isSelected = isSel,
                    modifier = Modifier.size(28.dp),
                    activeColor = TikoGreen,
                    strokeColor = TikoOutline
                )
            }
        )

        // Lists
        BottomNavItem(
            label = "Lists",
            isSelected = currentTab == NavigationTab.LISTS,
            onClick = { onTabSelected(NavigationTab.LISTS) },
            tag = "nav_lists",
            icon = { isSel ->
                TikoNavListIcon(
                    modifier = Modifier.size(28.dp),
                    color = if (isSel) TikoGreen else TikoOutline
                )
            }
        )

        // Calendar
        BottomNavItem(
            label = "Calendar",
            isSelected = currentTab == NavigationTab.CALENDAR,
            onClick = { onTabSelected(NavigationTab.CALENDAR) },
            tag = "nav_calendar",
            icon = { isSel ->
                TikoNavCalendarIcon(
                    modifier = Modifier.size(28.dp),
                    color = if (isSel) TikoGreen else TikoOutline
                )
            }
        )

        // Settings
        BottomNavItem(
            label = "Settings",
            isSelected = currentTab == NavigationTab.SETTINGS,
            onClick = { onTabSelected(NavigationTab.SETTINGS) },
            tag = "nav_settings",
            icon = { isSel ->
                TikoNavSettingsIcon(
                    modifier = Modifier.size(28.dp),
                    color = if (isSel) TikoGreen else TikoOutline
                )
            }
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String,
    icon: @Composable (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val activeColor = TikoGreen
    val inactiveColor = TikoText

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon(isSelected)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = DMSans,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (isSelected) activeColor else inactiveColor
                )
            )
        }
    }
}
