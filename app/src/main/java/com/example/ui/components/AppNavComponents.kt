package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.NavigationTab

/**
 * Reusable press-scale modifier: scales to 0.96 while pressed (spring, dampingRatio 0.6),
 * returns to 1.0 on release. Wrap around any tappable composable.
 * Usage: Modifier.pressable(interactionSource)
 */
@Composable
fun Modifier.pressable(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.96f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pressable_scale"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

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
 * Shared RoundIconButton:
 * Frosted glassmorphic glassy circular button (48dp).
 * Features a subtle porcelain frosted gradient, a specular white light reflection rim,
 * soft diffused drop shadow, and crisp 100% visible sharp vector icons with no blur.
 */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    testTag: String = "round_icon_button",
    isDark: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "glass_button_press_scale"
    )

    val bgBrush = if (isDark) {
        Brush.verticalGradient(
            colors = if (isPressed) {
                listOf(Color(0xFF1E1E1E), Color(0xFF1A1A1A))
            } else {
                listOf(Color(0xFF242424), Color(0xFF242424))
            }
        )
    } else {
        Brush.verticalGradient(
            colors = if (isPressed) {
                listOf(Color(0xFFF0EFEF), Color(0xFFE5E4DE))
            } else {
                listOf(Color(0xFFFFFFFF), Color(0xFFF6F5F2))
            }
        )
    }

    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF383838),
                Color(0xFF2E2E2E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFDCDAD4).copy(alpha = 0.8f)
            )
        )
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = if (isDark) 0.25f else 0.10f),
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.12f else 0.04f)
            )
            .clip(CircleShape)
            .background(bgBrush)
            .border(
                1.dp,
                borderBrush,
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isDark) Color(0xFFF5F1E6) else AppBlack,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Shared FrostedGlassPill:
 * Frosted glassmorphic pill container for buttons, filters, badges, and chips.
 * Soft porcelain frosted gradient with specular rim and subtle shadow.
 * Fully visible and razor-sharp content with no blur on text or icons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FrostedGlassPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    testTag: String = "",
    elevation: Dp = 4.dp,
    borderWidth: Dp = 1.dp,
    isDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && (onClick != null || onLongClick != null)) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "glass_pill_press_scale"
    )

    val bgBrush = if (isDark) {
        Brush.verticalGradient(
            colors = if (isPressed && (onClick != null || onLongClick != null)) {
                listOf(Color(0xFF1E1E1E), Color(0xFF1A1A1A))
            } else {
                listOf(Color(0xFF242424), Color(0xFF242424))
            }
        )
    } else {
        Brush.verticalGradient(
            colors = if (isPressed && (onClick != null || onLongClick != null)) {
                listOf(Color(0xFFF0EFEF), Color(0xFFE8E7E2))
            } else {
                listOf(Color(0xFFFFFFFF), Color(0xFFF6F5F2))
            }
        )
    }

    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF383838),
                Color(0xFF2E2E2E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFDCDAD4).copy(alpha = 0.8f)
            )
        )
    }

    val baseModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = elevation,
            shape = CircleShape,
            spotColor = Color.Black.copy(alpha = if (isDark) 0.20f else 0.08f),
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.10f else 0.04f)
        )
        .clip(CircleShape)
        .background(bgBrush)
        .border(
            borderWidth,
            borderBrush,
            CircleShape
        )

    val finalModifier = if (onClick != null || onLongClick != null) {
        baseModifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick ?: {},
                onLongClick = onLongClick
            )
            .testTag(testTag)
    } else {
        baseModifier.testTag(testTag)
    }

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Modifier extension to apply frosted glassmorphic styling to any Box or layout.
 */
fun Modifier.frostedGlass(
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    elevation: Dp = 4.dp,
    borderWidth: Dp = 1.dp,
    isDark: Boolean = false
): Modifier = if (isDark) {
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = Color.Black.copy(alpha = 0.20f),
            ambientColor = Color.Black.copy(alpha = 0.10f)
        )
        .clip(shape)
        .background(Color(0xFF242424))
        .border(
            borderWidth,
            Color(0xFF383838),
            shape
        )
} else {
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = Color.Black.copy(alpha = 0.08f),
            ambientColor = Color.Black.copy(alpha = 0.04f)
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF6F5F2)
                )
            )
        )
        .border(
            borderWidth,
            Brush.verticalGradient(
                listOf(
                    Color.White,
                    Color(0xFFDCDAD4).copy(alpha = 0.8f)
                )
            ),
            shape
        )
}

/**
 * Shared AppBottomNav:
 * Floating pill bar (68dp height, fully rounded, floating soft shadow).
 * Dark theme uses #1E1E1E container with cream pill (#F5F1E6) active tab and #A8A39A inactive icons.
 * Light theme preserves the luminous Foodpanda-style milky frosted porcelain surface with #111111 capsule.
 */
@Composable
fun AppBottomNav(
    activeTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val activeIndex = when (activeTab) {
        NavigationTab.HOME -> 0
        NavigationTab.LISTS -> 1
        NavigationTab.CALENDAR -> 3
        NavigationTab.SETTINGS -> 4
    }

    val activeTarget = activeIndex.toFloat()

    // Leading edge springs forward with quick response
    val leadIndex by animateFloatAsState(
        targetValue = activeTarget,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = 450f
        ),
        label = "capsule_lead"
    )

    // Trailing edge follows with slight inertia to create a fluid stretchy rubber-band toggle
    val trailIndex by animateFloatAsState(
        targetValue = activeTarget,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = 300f
        ),
        label = "capsule_trail"
    )

    val navBarBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF).copy(alpha = 0.96f)
    val navBorderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF333333),
                Color(0xFF242424)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color(0xFFE0DDD5).copy(alpha = 0.80f)
            )
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.14f),
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.18f else 0.08f)
            )
            .clip(CircleShape)
            .background(navBarBg)
            .border(1.dp, navBorderBrush, CircleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val tabWidth = totalWidth / 5
        val baseCapsuleWidth = (tabWidth - 8.dp).coerceAtLeast(0.dp)
        val capsuleHeight = 58.dp

        // Dynamic 120fps stretchy toggle physics between tabs
        val minIdx = minOf(leadIndex, trailIndex)
        val maxIdx = maxOf(leadIndex, trailIndex)
        val leftOffset = (minIdx * tabWidth.value + 4f).dp
        val stretchWidth = ((maxIdx - minIdx) * tabWidth.value).dp
        val dynamicCapsuleWidth = baseCapsuleWidth + stretchWidth
        val stretchRatio = (maxIdx - minIdx).coerceIn(0f, 1.5f)
        val squishY = (1f - stretchRatio * 0.05f).coerceIn(0.92f, 1f)

        // Dark: cream pill (#F5F1E6). Light: solid black (#111111) rounded capsule
        val capsuleColor = if (isDark) Color(0xFFF5F1E6) else Color(0xFF111111)

        Box(
            modifier = Modifier
                .offset(x = leftOffset)
                .width(dynamicCapsuleWidth)
                .height(capsuleHeight)
                .graphicsLayer {
                    scaleY = squishY
                }
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(capsuleColor)
        )

        // 5 Equal Tab Items
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassNavTab(
                isActive = activeTab == NavigationTab.HOME,
                label = "Home",
                filledIcon = Icons.Filled.Home,
                outlinedIcon = Icons.Outlined.Home,
                onClick = { onTabSelected(NavigationTab.HOME) },
                testTag = "nav_home_button",
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
            GlassNavTab(
                isActive = activeTab == NavigationTab.LISTS,
                label = "Lists",
                filledIcon = Icons.Filled.FormatListBulleted,
                outlinedIcon = Icons.Outlined.FormatListBulleted,
                onClick = { onTabSelected(NavigationTab.LISTS) },
                testTag = "nav_list_button",
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
            GlassNavTab(
                isActive = false,
                label = "Add",
                filledIcon = Icons.Filled.Add,
                outlinedIcon = Icons.Outlined.Add,
                onClick = onAddClick,
                testTag = "nav_add_button",
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
            GlassNavTab(
                isActive = activeTab == NavigationTab.CALENDAR,
                label = "Calendar",
                filledIcon = Icons.Filled.CalendarToday,
                outlinedIcon = Icons.Outlined.CalendarToday,
                onClick = { onTabSelected(NavigationTab.CALENDAR) },
                testTag = "nav_calendar_button",
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
            GlassNavTab(
                isActive = activeTab == NavigationTab.SETTINGS,
                label = "Profile",
                filledIcon = Icons.Filled.Person,
                outlinedIcon = Icons.Outlined.Person,
                onClick = { onTabSelected(NavigationTab.SETTINGS) },
                testTag = "nav_profile_button",
                isDark = isDark,
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
    testTag: String = "",
    isDark: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "tab_press_scale"
    )

    val targetIconColor = if (isDark) {
        if (isActive) Color(0xFF151515) else Color(0xFFA8A39A)
    } else {
        if (isActive) Color.White else Color(0xFF111111).copy(alpha = 0.80f)
    }

    val targetLabelColor = if (isDark) {
        if (isActive) Color(0xFF151515) else Color(0xFFA8A39A)
    } else {
        if (isActive) Color.White else Color(0xFF111111)
    }

    val iconColor by animateColorAsState(
        targetValue = targetIconColor,
        animationSpec = tween(durationMillis = 180),
        label = "tab_icon_color"
    )

    val labelColor by animateColorAsState(
        targetValue = targetLabelColor,
        animationSpec = tween(durationMillis = 180),
        label = "tab_label_color"
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
                animationSpec = tween(durationMillis = 180),
                label = "tab_icon_crossfade"
            ) { active ->
                Icon(
                    imageVector = if (active) filledIcon else outlinedIcon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = labelColor
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
