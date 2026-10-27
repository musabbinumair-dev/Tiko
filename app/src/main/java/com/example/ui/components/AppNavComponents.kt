package com.example.ui.components

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Shared capsule transition memory across screen changes
 * so switching tabs smoothly glides the capsule without jumping.
 */
internal object CapsuleTransitionState {
    var currentIndex: Float = 0f
}

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
 * Milky frosted glass gradient, specular light reflection border, soft shadow,
 * and Android 12+ blur RenderEffect.
 */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    testTag: String = "round_icon_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "glass_button_press_scale"
    )

    val isAndroid12OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val glassBackgroundBrush = Brush.verticalGradient(
        colors = if (isPressed) {
            listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFEFEFEF).copy(alpha = 0.90f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFFF7F6F3).copy(alpha = 0.82f)
            )
        }
    )

    val glassBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            Color.White.copy(alpha = 0.40f)
        )
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.10f),
                ambientColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(CircleShape)
            .then(
                if (isAndroid12OrAbove) {
                    Modifier.graphicsLayer {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(
                                16.dp.toPx(),
                                16.dp.toPx(),
                                android.graphics.Shader.TileMode.CLAMP
                            )
                            .asComposeRenderEffect()
                    }
                } else {
                    Modifier
                }
            )
            .background(glassBackgroundBrush)
            .border(1.dp, glassBorderBrush, CircleShape)
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
            tint = AppBlack,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Shared FrostedGlassPill:
 * Frosted glassmorphic glassy pill container for buttons, filters, badges, and chips.
 * Replaces plain white fill + black outline with luminous, semi-opaque frosted glass,
 * a specular light reflection rim, subtle soft shadow, and Android 12+ blur RenderEffect.
 */
@Composable
fun FrostedGlassPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String = "",
    elevation: Dp = 6.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "glass_pill_press_scale"
    )

    val isAndroid12OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val glassBackgroundBrush = Brush.verticalGradient(
        colors = if (isPressed && onClick != null) {
            listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFEFEFEF).copy(alpha = 0.90f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFFF7F6F3).copy(alpha = 0.82f)
            )
        }
    )

    val glassBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            Color.White.copy(alpha = 0.40f)
        )
    )

    val baseModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = elevation,
            shape = CircleShape,
            spotColor = Color.Black.copy(alpha = 0.10f),
            ambientColor = Color.Black.copy(alpha = 0.06f)
        )
        .clip(CircleShape)
        .then(
            if (isAndroid12OrAbove) {
                Modifier.graphicsLayer {
                    renderEffect = android.graphics.RenderEffect
                        .createBlurEffect(
                            14.dp.toPx(),
                            14.dp.toPx(),
                            android.graphics.Shader.TileMode.CLAMP
                        )
                        .asComposeRenderEffect()
                }
            } else {
                Modifier
            }
        )
        .background(glassBackgroundBrush)
        .border(borderWidth, glassBorderBrush, CircleShape)

    val finalModifier = if (onClick != null) {
        baseModifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
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
    elevation: Dp = 6.dp,
    borderWidth: Dp = 1.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = Color.Black.copy(alpha = 0.10f),
        ambientColor = Color.Black.copy(alpha = 0.06f)
    )
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFFF7F6F3).copy(alpha = 0.82f)
            )
        )
    )
    .border(
        borderWidth,
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.40f)
            )
        ),
        shape
    )

/**
 * Shared AppBottomNav:
 * Floating pill bar (68dp height, fully rounded, soft shadow).
 * Frosted glass with 20dp blur (Android 12+) and milky frosted gradient (85%-90% opacity), never washed-out transparent.
 * 1dp frosted specular border with subtle highlight.
 * Single black (#111111) capsule behind the active tab sliding with a fluid spring (dampingRatio 0.82, stiffness 380).
 * Continuous color transition synchronized with the sliding capsule.
 * 5 equal tabs with 24dp icons, 2dp gap, 11sp Outfit Medium label.
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

    // Persist position across screen recompositions so the capsule glides smoothly between tabs
    val animatableIndex = remember {
        Animatable(CapsuleTransitionState.currentIndex)
    }

    LaunchedEffect(activeIndex) {
        animatableIndex.animateTo(
            targetValue = activeIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 380f
            )
        ) {
            CapsuleTransitionState.currentIndex = value
        }
    }

    val isAndroid12OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Semi-opaque milky frosted glass gradient (never washed out or transparent)
    val frostedGlassBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.90f),
            Color(0xFFF7F6F3).copy(alpha = 0.85f)
        )
    )

    // Frosted specular rim border
    val frostedBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            Color.White.copy(alpha = 0.45f)
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.12f),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Frosted glass background layer (blurred on Android 12+, semi-opaque milky frosted surface)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .then(
                    if (isAndroid12OrAbove) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(
                                    20.dp.toPx(),
                                    20.dp.toPx(),
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                                .asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                )
                .background(frostedGlassBrush)
        )

        // 1dp specular frosted glass border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, frostedBorderBrush, CircleShape)
        )

        val currentAnimatedIndex = animatableIndex.value
        val totalWidth = maxWidth
        val tabWidth = totalWidth / 5

        // Subtle elastic fluid transit stretch that settles cleanly into the exact tab dimensions
        val distanceToTarget = abs(currentAnimatedIndex - activeIndex.toFloat())
        val stretchFactor = 1.0f + (distanceToTarget.coerceAtMost(1f) * 0.10f)
        val baseCapsuleWidth = (tabWidth - 8.dp).coerceAtLeast(0.dp)
        val capsuleWidth = baseCapsuleWidth * stretchFactor
        val capsuleHeight = 58.dp
        val centerX = (currentAnimatedIndex + 0.5f) * tabWidth.value
        val capsuleLeft = (centerX - (capsuleWidth.value / 2f)).dp

        // ONE solid black (#111111) rounded capsule sliding behind the active tab
        Box(
            modifier = Modifier
                .offset(x = capsuleLeft)
                .width(capsuleWidth)
                .height(capsuleHeight)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color(0xFF111111))
        )

        // 5 Equal Tab Items
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassNavTab(
                tabIndex = 0,
                animatedNavIndex = currentAnimatedIndex,
                label = "Home",
                filledIcon = Icons.Filled.Home,
                outlinedIcon = Icons.Outlined.Home,
                onClick = { onTabSelected(NavigationTab.HOME) },
                testTag = "nav_home_button",
                modifier = Modifier.weight(1f)
            )
            GlassNavTab(
                tabIndex = 1,
                animatedNavIndex = currentAnimatedIndex,
                label = "Lists",
                filledIcon = Icons.Filled.FormatListBulleted,
                outlinedIcon = Icons.Outlined.FormatListBulleted,
                onClick = { onTabSelected(NavigationTab.LISTS) },
                testTag = "nav_list_button",
                modifier = Modifier.weight(1f)
            )
            GlassNavTab(
                tabIndex = 2,
                animatedNavIndex = currentAnimatedIndex,
                label = "Add",
                filledIcon = Icons.Filled.Add,
                outlinedIcon = Icons.Outlined.Add,
                onClick = onAddClick,
                testTag = "nav_add_button",
                isSelectable = false,
                modifier = Modifier.weight(1f)
            )
            GlassNavTab(
                tabIndex = 3,
                animatedNavIndex = currentAnimatedIndex,
                label = "Calendar",
                filledIcon = Icons.Filled.CalendarToday,
                outlinedIcon = Icons.Outlined.CalendarToday,
                onClick = { onTabSelected(NavigationTab.CALENDAR) },
                testTag = "nav_calendar_button",
                modifier = Modifier.weight(1f)
            )
            GlassNavTab(
                tabIndex = 4,
                animatedNavIndex = currentAnimatedIndex,
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
    tabIndex: Int,
    animatedNavIndex: Float,
    label: String,
    filledIcon: ImageVector,
    outlinedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    isSelectable: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "tab_press_scale"
    )

    // Calculate how close the capsule is to this tab (0f = centered over it, >= 0.7f = inactive)
    val distance = abs(animatedNavIndex - tabIndex.toFloat())
    val activeFraction = if (isSelectable) {
        (1f - (distance * 1.6f)).coerceIn(0f, 1f)
    } else {
        (1f - (distance * 2f)).coerceIn(0f, 1f)
    }

    val inactiveIconColor = Color(0xFF111111).copy(alpha = 0.80f)
    val activeIconColor = Color.White
    val inactiveLabelColor = Color(0xFF111111)
    val activeLabelColor = Color.White

    // Smooth continuous color transition in direct lockstep with capsule motion
    val currentIconColor = lerp(inactiveIconColor, activeIconColor, activeFraction)
    val currentLabelColor = lerp(inactiveLabelColor, activeLabelColor, activeFraction)

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
            Icon(
                imageVector = if (activeFraction >= 0.45f) filledIcon else outlinedIcon,
                contentDescription = label,
                tint = currentIconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = currentLabelColor
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
