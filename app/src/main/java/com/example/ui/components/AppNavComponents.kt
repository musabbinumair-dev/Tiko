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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.NavigationTab
import kotlin.math.abs


val AppCreamBackground = Color(0xFFEFEBDD)
val AppBlack = Color(0xFF151515)
val AppGray = Color(0xFF6B6B6B)
val AppOutlineGray = Color(0xFFBDBDBD)
val CardPurple = Color(0xFF6B4EFF)
val CardYellow = Color(0xFFFFB92E)
val CardBlack = Color(0xFF151515)
val CardLime = Color(0xFFC8DB6A)

/**
 * Shared screenTopPadding for all screen top rows:
 * Handles the status bar inset plus the 10dp top padding from Login/SignUp screens.
 */
fun Modifier.screenTopPadding(): Modifier = this
    .statusBarsPadding()
    .padding(top = 10.dp)

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
 * Glassmorphic floating capsule, 72dp high, fully rounded, soft shadow.
 * 5 equal tabs with 24dp icons and Outfit SemiBold 12sp labels: Home, Lists, Add, Calendar, Profile.
 * Inactive: outlined icon, gray #5B5F66. Active: filled icon, black #111111.
 * Active tab capsule: 56dp high, tab width minus 6dp, black at 12% alpha.
 * Animated with spring (dampingRatio 0.72, stiffness 380) with 12% horizontal stretch.
 */
@Composable
fun AppBottomNav(
    activeTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIndex = when (activeTab) {
        NavigationTab.HOME -> 0
        NavigationTab.LISTS -> 1
        NavigationTab.CALENDAR -> 3
        NavigationTab.SETTINGS -> 4
    }

    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 380f
        ),
        label = "active_tab_capsule_slide"
    )

    val sdkInt = android.os.Build.VERSION.SDK_INT
    val glassBgColor = if (sdkInt >= 31) {
        Color.White.copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }

    val borderGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.70f),
            Color.White.copy(alpha = 0.10f)
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.12f),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(CircleShape)
            .background(glassBgColor)
            .border(1.dp, borderGradient, CircleShape)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val tabWidth = totalWidth / 5
        val diff = abs(animatedIndex - activeIndex)
        val stretchFactor = 1.0f + (diff.coerceAtMost(1f) * 0.12f)
        val baseCapsuleWidth = (tabWidth - 6.dp).coerceAtLeast(0.dp)
        val capsuleWidth = baseCapsuleWidth * stretchFactor
        val capsuleHeight = 56.dp
        val centerX = (animatedIndex + 0.5f) * tabWidth
        val capsuleLeft = centerX - (capsuleWidth / 2f)

        // Active tab pill capsule indicator
        Box(
            modifier = Modifier
                .offset(x = capsuleLeft)
                .width(capsuleWidth)
                .height(capsuleHeight)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color(0xFF111111).copy(alpha = 0.12f))
        )

        // 5 Equal Tab Items
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            GlassNavTab(
                isActive = activeTab == NavigationTab.HOME,
                label = "Home",
                filledIcon = Icons.Filled.Home,
                outlinedIcon = Icons.Outlined.Home,
                onClick = { onTabSelected(NavigationTab.HOME) },
                testTag = "nav_home_button",
                modifier = Modifier.weight(1f)
            )

            // 2. Lists
            GlassNavTab(
                isActive = activeTab == NavigationTab.LISTS,
                label = "Lists",
                filledIcon = Icons.Filled.FormatListBulleted,
                outlinedIcon = Icons.Outlined.FormatListBulleted,
                onClick = { onTabSelected(NavigationTab.LISTS) },
                testTag = "nav_list_button",
                modifier = Modifier.weight(1f)
            )

            // 3. Add (never stays active, opens Add task)
            GlassNavTab(
                isActive = false,
                label = "Add",
                filledIcon = Icons.Filled.Add,
                outlinedIcon = Icons.Outlined.Add,
                onClick = onAddClick,
                testTag = "nav_add_button",
                modifier = Modifier.weight(1f)
            )

            // 4. Calendar
            GlassNavTab(
                isActive = activeTab == NavigationTab.CALENDAR,
                label = "Calendar",
                filledIcon = Icons.Filled.CalendarToday,
                outlinedIcon = Icons.Outlined.CalendarToday,
                onClick = { onTabSelected(NavigationTab.CALENDAR) },
                testTag = "nav_calendar_button",
                modifier = Modifier.weight(1f)
            )

            // 5. Profile
            GlassNavTab(
                isActive = activeTab == NavigationTab.SETTINGS,
                label = "Profile",
                filledIcon = Icons.Filled.Person,
                outlinedIcon = Icons.Outlined.Person,
                onClick = { onTabSelected(NavigationTab.SETTINGS) },
                testTag = "nav_profile_button",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GlassNavTab(
    isActive: Boolean,
    label: String,
    filledIcon: ImageVector,
    outlinedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "tab_press_scale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF111111) else Color(0xFF5B5F66),
        animationSpec = tween(durationMillis = 200),
        label = "tab_content_color"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Crossfade(
                targetState = isActive,
                animationSpec = tween(durationMillis = 200),
                label = "tab_icon_crossfade"
            ) { active ->
                Icon(
                    imageVector = if (active) filledIcon else outlinedIcon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = contentColor
            )
        }
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

/**
 * Clean initials avatar or fallback icon in a colored circle
 */
@Composable
fun UserInitialsAvatar(
    name: String,
    size: Dp = 52.dp,
    backgroundColor: Color = CardYellow,
    textColor: Color = AppBlack,
    fontSize: TextUnit = (size.value * 0.40f).sp,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (initial.isNotBlank()) {
            Text(
                text = initial,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = textColor
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "User Avatar",
                tint = textColor,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}
