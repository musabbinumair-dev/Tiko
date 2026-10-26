package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
