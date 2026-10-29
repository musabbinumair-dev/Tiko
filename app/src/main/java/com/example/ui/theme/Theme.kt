package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Strict Tiko Flat Palette with #F6EDE9 on all screens and cards, 0dp radius everywhere
private val TikoColorScheme = lightColorScheme(
    primary = TikoOutline,
    onPrimary = TikoCardFill,
    primaryContainer = TikoMustard,
    onPrimaryContainer = TikoText,
    secondary = TikoGreen,
    onSecondary = TikoText,
    secondaryContainer = TikoGreen,
    onSecondaryContainer = TikoText,
    tertiary = TikoCoral,
    onTertiary = TikoCardFill,
    tertiaryContainer = TikoCoral,
    onTertiaryContainer = TikoCardFill,
    background = TikoBackground,
    onBackground = TikoText,
    surface = TikoCardFill,
    onSurface = TikoText,
    surfaceVariant = TikoCardFill,
    onSurfaceVariant = TikoText,
    outline = TikoOutline,
    outlineVariant = TikoOutline
)

val TikoShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

data class TikoThemeColors(
    val screenBg: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val divider: Color,
    val bottomNavBg: Color,
    val bottomNavCapsule: Color,
    val roundButtonBg: Color,
    val roundButtonBorder: Color,
    val roundButtonTint: Color,
    val scrim: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val isDark: Boolean
)

val LocalTikoThemeColors = compositionLocalOf {
    TikoThemeColors(
        screenBg = Color(0xFFEFEBDD),
        cardBg = Color.White,
        cardBorder = Color(0xFFE8E8E8),
        textPrimary = Color(0xFF151515),
        textSecondary = Color(0xFF6B6B6B),
        textMuted = Color(0xFF9E9E9E),
        divider = Color(0xFFE8E8E8),
        bottomNavBg = Color(0xFFFFFFFF).copy(alpha = 0.96f),
        bottomNavCapsule = Color(0xFF111111),
        roundButtonBg = Color.White,
        roundButtonBorder = Color(0xFF151515),
        roundButtonTint = Color(0xFF151515),
        scrim = Color(0xFFEFEBDD).copy(alpha = 0.80f),
        gradientStart = Color(0xFFFFCF4D).copy(alpha = 0.85f),
        gradientEnd = Color(0xFFEFEBDD),
        isDark = false
    )
}

@Composable
fun rememberAnimatedTikoColors(isDark: Boolean): TikoThemeColors {
    val duration = 380
    val spec = tween<Color>(durationMillis = duration, easing = FastOutSlowInEasing)

    val screenBg by animateColorAsState(if (isDark) Color(0xFF141414) else Color(0xFFEFEBDD), spec, label = "screenBg")
    val cardBg by animateColorAsState(if (isDark) Color(0xFF242424) else Color.White, spec, label = "cardBg")
    val cardBorder by animateColorAsState(if (isDark) Color(0xFF383838) else Color(0xFFDCD8CB), spec, label = "cardBorder")
    val textPrimary by animateColorAsState(if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515), spec, label = "textPrimary")
    val textSecondary by animateColorAsState(if (isDark) Color(0xFFA8A39A) else Color(0xFF6B6B6B), spec, label = "textSecondary")
    val textMuted by animateColorAsState(if (isDark) Color(0xFF706D66) else Color(0xFF9E9E9E), spec, label = "textMuted")
    val divider by animateColorAsState(if (isDark) Color(0xFF2E2E2E) else Color(0xFFE8E8E8), spec, label = "divider")
    val bottomNavBg by animateColorAsState(if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF).copy(alpha = 0.96f), spec, label = "bottomNavBg")
    val bottomNavCapsule by animateColorAsState(if (isDark) Color(0xFFF5F1E6) else Color(0xFF111111), spec, label = "bottomNavCapsule")
    val roundButtonBg by animateColorAsState(if (isDark) Color(0xFF242424) else Color.White, spec, label = "roundButtonBg")
    val roundButtonBorder by animateColorAsState(if (isDark) Color(0xFF383838) else Color(0xFF151515), spec, label = "roundButtonBorder")
    val roundButtonTint by animateColorAsState(if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515), spec, label = "roundButtonTint")
    val scrim by animateColorAsState((if (isDark) Color(0xFF141414) else Color(0xFFEFEBDD)).copy(alpha = 0.80f), spec, label = "scrim")
    val gradientStart by animateColorAsState(if (isDark) Color(0xFF4B3512) else Color(0xFFFFCF4D).copy(alpha = 0.85f), spec, label = "gradientStart")
    val gradientEnd by animateColorAsState(if (isDark) Color(0xFF141414) else Color(0xFFEFEBDD), spec, label = "gradientEnd")

    return TikoThemeColors(
        screenBg = screenBg,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textMuted = textMuted,
        divider = divider,
        bottomNavBg = bottomNavBg,
        bottomNavCapsule = bottomNavCapsule,
        roundButtonBg = roundButtonBg,
        roundButtonBorder = roundButtonBorder,
        roundButtonTint = roundButtonTint,
        scrim = scrim,
        gradientStart = gradientStart,
        gradientEnd = gradientEnd,
        isDark = isDark
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TikoColorScheme,
        typography = Typography,
        shapes = TikoShapes,
        content = content
    )
}
