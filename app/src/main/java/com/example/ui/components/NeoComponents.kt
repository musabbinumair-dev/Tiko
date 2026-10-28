package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.TaskEntity
import com.example.ui.screens.HomeTaskCard
import com.example.ui.theme.BricolageGrotesque
import com.example.ui.theme.DMSans
import com.example.ui.theme.TikoCardFill
import com.example.ui.theme.TikoCoral
import com.example.ui.theme.TikoGreen
import com.example.ui.theme.TikoMint
import com.example.ui.theme.TikoMustard
import com.example.ui.theme.TikoOutline
import com.example.ui.theme.TikoPeach
import com.example.ui.theme.TikoText

// Neo-Brutalist Card with strict 2dp black border and 0dp radius, #F6EDE9 fill
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = TikoCardFill,
    borderWidth: Dp = 2.dp,
    borderColor: Color = TikoOutline,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(0.dp)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .then(clickableModifier),
        content = content
    )
}

// Neo-Brutalist Square Checkbox matching Mockup exactly
@Composable
fun NeoCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    if (checked) TikoOutline else TikoCardFill,
                    RoundedCornerShape(0.dp)
                )
                .border(2.dp, TikoOutline, RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Checked",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.72f)
                )
            }
        }
    }
}

// Neo-Brutalist Button
@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    leadingIcon: ImageVector? = null,
    isPrimary: Boolean = true,
    backgroundColor: Color? = null,
    contentColor: Color? = null,
    height: Dp = 54.dp
) {
    val resolvedBg = backgroundColor ?: if (isPrimary) TikoOutline else TikoCardFill
    val resolvedContent = contentColor ?: if (isPrimary) Color.White else TikoText

    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .border(2.dp, TikoOutline, RoundedCornerShape(0.dp)),
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = resolvedBg,
            contentColor = resolvedContent
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = resolvedContent
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = BricolageGrotesque,
                    color = resolvedContent
                )
            )
        }
    }
}

// Category color resolver
fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "work" -> TikoMustard
        "health" -> TikoMint
        "personal" -> TikoPeach
        "shopping" -> TikoPeach
        "study" -> TikoGreen
        "ideas" -> TikoMustard
        else -> TikoMint
    }
}

// Fallback imagevector resolver for screens that use it
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "work" -> Icons.Outlined.Description
        "health" -> Icons.Outlined.FitnessCenter
        "personal" -> Icons.Outlined.Person
        "shopping" -> Icons.Outlined.ShoppingCart
        "study" -> Icons.Outlined.MenuBook
        "ideas" -> Icons.Outlined.Lightbulb
        else -> Icons.Outlined.Description
    }
}

// TaskCard alias for other screens
@Composable
fun TaskCard(
    task: TaskEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeTaskCard(task = task, onClick = onClick, onToggle = onToggle, modifier = modifier)
}

// ==========================================
// CUSTOM OUTLINED ICONS (2dp Stroke, Square-ish)
// ==========================================

@Composable
fun TikoCategoryIcon(
    category: String,
    modifier: Modifier = Modifier,
    color: Color = TikoText,
    strokeWidth: Dp = 2.dp
) {
    when (category.lowercase()) {
        "work" -> TikoDocumentIcon(modifier, color, strokeWidth)
        "health" -> TikoDumbbellIcon(modifier, color, strokeWidth)
        "personal", "shopping" -> TikoCartIcon(modifier, color, strokeWidth)
        "study" -> TikoBookIcon(modifier, color, strokeWidth)
        "ideas" -> TikoLightbulbIcon(modifier, color, strokeWidth)
        else -> TikoDocumentIcon(modifier, color, strokeWidth)
    }
}

// Document Icon: folded top right corner, horizontal lines inside
@Composable
fun TikoDocumentIcon(
    modifier: Modifier = Modifier,
    color: Color = TikoText,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val left = w * 0.18f
        val top = h * 0.12f
        val right = w * 0.82f
        val bottom = h * 0.88f
        val fold = w * 0.22f

        // Document perimeter path with folded corner
        val docPath = Path().apply {
            moveTo(left, top)
            lineTo(right - fold, top)
            lineTo(right, top + fold)
            lineTo(right, bottom)
            lineTo(left, bottom)
            close()
        }
        drawPath(
            path = docPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )

        // Fold corner flap
        val foldPath = Path().apply {
            moveTo(right - fold, top)
            lineTo(right - fold, top + fold)
            lineTo(right, top + fold)
        }
        drawPath(
            path = foldPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )

        // Horizontal lines inside document
        val lineLeft = left + w * 0.12f
        val lineRight = right - w * 0.12f
        val line1Y = top + h * 0.32f
        val line2Y = top + h * 0.46f
        val line3Y = top + h * 0.60f

        drawLine(color, Offset(lineLeft, line1Y), Offset(lineRight, line1Y), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(lineLeft, line2Y), Offset(lineRight, line2Y), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(lineLeft, line3Y), Offset(lineRight * 0.75f, line3Y), strokeWidth = stroke, cap = StrokeCap.Square)
    }
}

// Dumbbell Icon: bar connecting two weights on left & right
@Composable
fun TikoDumbbellIcon(
    modifier: Modifier = Modifier,
    color: Color = TikoText,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height
        val centerY = h * 0.5f

        // Central connecting bar
        drawLine(
            color = color,
            start = Offset(w * 0.25f, centerY),
            end = Offset(w * 0.75f, centerY),
            strokeWidth = stroke * 1.5f,
            cap = StrokeCap.Square
        )

        // Left outer weight plate
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, centerY - h * 0.28f),
            size = Size(w * 0.12f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
            style = Stroke(width = stroke)
        )
        // Left inner weight plate
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.24f, centerY - h * 0.20f),
            size = Size(w * 0.08f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
            style = Stroke(width = stroke)
        )

        // Right inner weight plate
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.68f, centerY - h * 0.20f),
            size = Size(w * 0.08f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
            style = Stroke(width = stroke)
        )
        // Right outer weight plate
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.76f, centerY - h * 0.28f),
            size = Size(w * 0.12f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
            style = Stroke(width = stroke)
        )
    }
}

// Shopping Cart Icon: wireframe basket with wheels
@Composable
fun TikoCartIcon(
    modifier: Modifier = Modifier,
    color: Color = TikoText,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        // Handle and basket path
        val cartPath = Path().apply {
            moveTo(w * 0.14f, h * 0.24f)
            lineTo(w * 0.26f, h * 0.24f)
            lineTo(w * 0.38f, h * 0.62f)
            lineTo(w * 0.82f, h * 0.62f)
            lineTo(w * 0.90f, h * 0.32f)
            lineTo(w * 0.30f, h * 0.32f)
        }
        drawPath(
            path = cartPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )

        // Internal basket divider lines
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.32f),
            end = Offset(w * 0.50f, h * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )
        drawLine(
            color = color,
            start = Offset(w * 0.70f, h * 0.32f),
            end = Offset(w * 0.70f, h * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )
        drawLine(
            color = color,
            start = Offset(w * 0.35f, h * 0.47f),
            end = Offset(w * 0.85f, h * 0.47f),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )

        // Wheels
        val wheelRadius = w * 0.065f
        drawCircle(
            color = color,
            radius = wheelRadius,
            center = Offset(w * 0.42f, h * 0.76f),
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = color,
            radius = wheelRadius,
            center = Offset(w * 0.78f, h * 0.76f),
            style = Stroke(width = stroke)
        )
    }
}

// Open Book Icon: two open pages with center spine
@Composable
fun TikoBookIcon(
    modifier: Modifier = Modifier,
    color: Color = TikoText,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val centerX = w * 0.5f
        val topY = h * 0.28f
        val bottomY = h * 0.75f

        // Left page
        val leftPage = Path().apply {
            moveTo(centerX, bottomY)
            lineTo(centerX, topY)
            cubicTo(w * 0.40f, topY - h * 0.08f, w * 0.22f, topY - h * 0.02f, w * 0.12f, topY + h * 0.04f)
            lineTo(w * 0.12f, bottomY + h * 0.04f)
            cubicTo(w * 0.22f, bottomY - h * 0.02f, w * 0.40f, bottomY - h * 0.08f, centerX, bottomY)
            close()
        }
        drawPath(
            path = leftPage,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )

        // Right page
        val rightPage = Path().apply {
            moveTo(centerX, bottomY)
            lineTo(centerX, topY)
            cubicTo(w * 0.60f, topY - h * 0.08f, w * 0.78f, topY - h * 0.02f, w * 0.88f, topY + h * 0.04f)
            lineTo(w * 0.88f, bottomY + h * 0.04f)
            cubicTo(w * 0.78f, bottomY - h * 0.02f, w * 0.60f, bottomY - h * 0.08f, centerX, bottomY)
            close()
        }
        drawPath(
            path = rightPage,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )

        // Center spine line
        drawLine(
            color = color,
            start = Offset(centerX, topY),
            end = Offset(centerX, bottomY + h * 0.06f),
            strokeWidth = stroke,
            cap = StrokeCap.Square
        )
    }
}

// Lightbulb Icon
@Composable
fun TikoLightbulbIcon(
    modifier: Modifier = Modifier,
    color: Color = TikoText,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val centerX = w * 0.5f

        // Bulb outline
        val bulbPath = Path().apply {
            moveTo(centerX - w * 0.18f, h * 0.62f)
            cubicTo(w * 0.12f, h * 0.45f, w * 0.16f, h * 0.15f, centerX, h * 0.15f)
            cubicTo(w * 0.84f, h * 0.15f, w * 0.88f, h * 0.45f, centerX + w * 0.18f, h * 0.62f)
            lineTo(centerX + w * 0.16f, h * 0.74f)
            lineTo(centerX - w * 0.16f, h * 0.74f)
            close()
        }
        drawPath(
            path = bulbPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )

        // Base screw lines
        drawLine(color, Offset(centerX - w * 0.14f, h * 0.79f), Offset(centerX + w * 0.14f, h * 0.79f), stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(centerX - w * 0.08f, h * 0.84f), Offset(centerX + w * 0.08f, h * 0.84f), stroke, cap = StrokeCap.Square)
    }
}

// ==========================================
// BOTTOM NAVIGATION ICONS (2dp Outlined)
// ==========================================

// Home Icon (pitched roof house, filled with green when active)
@Composable
fun TikoNavHomeIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = TikoGreen,
    strokeColor: Color = TikoOutline,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val housePath = Path().apply {
            moveTo(w * 0.5f, h * 0.14f) // peak
            lineTo(w * 0.88f, h * 0.46f)
            lineTo(w * 0.78f, h * 0.46f)
            lineTo(w * 0.78f, h * 0.86f)
            lineTo(w * 0.22f, h * 0.86f)
            lineTo(w * 0.22f, h * 0.46f)
            lineTo(w * 0.12f, h * 0.46f)
            close()
        }

        if (isSelected) {
            drawPath(path = housePath, color = activeColor, style = Fill)
        }
        drawPath(
            path = housePath,
            color = strokeColor,
            style = Stroke(width = stroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )
    }
}

// Lists Icon (three horizontal lines with square bullets)
@Composable
fun TikoNavListIcon(
    modifier: Modifier = Modifier,
    color: Color = TikoOutline,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val y1 = h * 0.28f
        val y2 = h * 0.50f
        val y3 = h * 0.72f

        val bulletX = w * 0.20f
        val bulletSize = w * 0.10f

        // Bullet squares
        drawRect(color, Offset(bulletX - bulletSize / 2, y1 - bulletSize / 2), Size(bulletSize, bulletSize))
        drawRect(color, Offset(bulletX - bulletSize / 2, y2 - bulletSize / 2), Size(bulletSize, bulletSize))
        drawRect(color, Offset(bulletX - bulletSize / 2, y3 - bulletSize / 2), Size(bulletSize, bulletSize))

        // Horizontal lines
        val lineStartX = w * 0.38f
        val lineEndX = w * 0.84f
        drawLine(color, Offset(lineStartX, y1), Offset(lineEndX, y1), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(lineStartX, y2), Offset(lineEndX, y2), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(lineStartX, y3), Offset(lineEndX, y3), strokeWidth = stroke, cap = StrokeCap.Square)
    }
}

// Calendar Icon (binder rings, grid)
@Composable
fun TikoNavCalendarIcon(
    modifier: Modifier = Modifier,
    color: Color = TikoOutline,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val left = w * 0.16f
        val top = h * 0.24f
        val right = w * 0.84f
        val bottom = h * 0.84f

        // Calendar outer box
        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            style = Stroke(width = stroke)
        )

        // Top horizontal line
        drawLine(color, Offset(left, top + h * 0.18f), Offset(right, top + h * 0.18f), strokeWidth = stroke)

        // Binder ring tabs
        drawLine(color, Offset(w * 0.35f, h * 0.12f), Offset(w * 0.35f, top), strokeWidth = stroke * 1.2f, cap = StrokeCap.Square)
        drawLine(color, Offset(w * 0.65f, h * 0.12f), Offset(w * 0.65f, top), strokeWidth = stroke * 1.2f, cap = StrokeCap.Square)

        // Calendar date dots/squares
        val dotSize = w * 0.08f
        val gridY = top + h * 0.34f
        drawRect(color, Offset(w * 0.30f, gridY), Size(dotSize, dotSize))
        drawRect(color, Offset(w * 0.60f, gridY), Size(dotSize, dotSize))
    }
}

// Settings Icon (gear/cog with center hole)
@Composable
fun TikoNavSettingsIcon(
    modifier: Modifier = Modifier,
    color: Color = TikoOutline,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val outerR = w * 0.34f
        val innerR = w * 0.14f

        // Center circle
        drawCircle(color = color, radius = innerR, center = center, style = Stroke(width = stroke))

        // Outer gear circle
        drawCircle(color = color, radius = outerR, center = center, style = Stroke(width = stroke))

        // 6 gear teeth
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60).toDouble())
            val startX = (center.x + outerR * Math.cos(angle)).toFloat()
            val startY = (center.y + outerR * Math.sin(angle)).toFloat()
            val endX = (center.x + (outerR + w * 0.08f) * Math.cos(angle)).toFloat()
            val endY = (center.y + (outerR + w * 0.08f) * Math.sin(angle)).toFloat()
            drawLine(color, Offset(startX, startY), Offset(endX, endY), strokeWidth = stroke * 1.4f, cap = StrokeCap.Square)
        }
    }
}

// ==========================================
// LARGE CROPPED DOODLES
// ==========================================

@Composable
fun LargeGridCircleDoodle(
    modifier: Modifier = Modifier,
    color: Color = TikoOutline,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Circle perimeter
        drawCircle(
            color = color,
            radius = r,
            center = center,
            style = Stroke(width = stroke)
        )

        // Grid lines inside the circle
        val step = r * 0.5f
        for (i in -1..1) {
            val offset = i * step
            val halfHeight = kotlin.math.sqrt((r * r - offset * offset).coerceAtLeast(0f))
            drawLine(
                color = color,
                start = Offset(center.x + offset, center.y - halfHeight),
                end = Offset(center.x + offset, center.y + halfHeight),
                strokeWidth = stroke
            )
            val halfWidth = kotlin.math.sqrt((r * r - offset * offset).coerceAtLeast(0f))
            drawLine(
                color = color,
                start = Offset(center.x - halfWidth, center.y + offset),
                end = Offset(center.x + halfWidth, center.y + offset),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
fun HollowTriangleDoodle(
    modifier: Modifier = Modifier,
    color: Color = TikoOutline,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, h * 0.5f)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )
    }
}

@Composable
fun SmallUpTriangleDoodle(
    modifier: Modifier = Modifier,
    color: Color = TikoOutline,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )
    }
}

@Composable
fun LargeArcDoodle(
    modifier: Modifier = Modifier,
    color: Color = TikoOutline,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(0f, 0f),
            size = Size(w * 2, h * 2),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun CurlyLoopDoodle(
    modifier: Modifier = Modifier,
    color: Color = TikoOutline,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.90f, 0f)
            cubicTo(
                w * 0.95f, h * 0.40f,
                w * 0.55f, h * 0.15f,
                w * 0.50f, h * 0.40f
            )
            cubicTo(
                w * 0.45f, h * 0.70f,
                w * 0.85f, h * 0.80f,
                w * 0.10f, h * 0.95f
            )
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

// Aliases for compatibility
@Composable
fun NeoDoodleGrid(modifier: Modifier = Modifier) = LargeGridCircleDoodle(modifier)

@Composable
fun NeoDoodleTriangle(modifier: Modifier = Modifier) = HollowTriangleDoodle(modifier)

@Composable
fun NeoDoodleSquiggle(modifier: Modifier = Modifier) = CurlyLoopDoodle(modifier)

@Composable
fun NeoDoodleQuarterCircle(modifier: Modifier = Modifier) = LargeArcDoodle(modifier)
