package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Native Jetpack Compose animated streak icon mimicking emoji.svg.
 * Fully animated with floating motion and blinking eyes using high-performance Canvas drawing,
 * avoiding WebView renderer crashes.
 */
@Composable
fun AnimatedStreakEmoji(
    modifier: Modifier = Modifier.size(22.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_emoji_anim")

    // Idle floating / bounce animation
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Blink animation for eyes (occurs periodically or smoothly)
    val eyeScaleY by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height

            translate(top = offsetY) {
                // Main yellow flame body (#ffad3b)
                drawCircle(
                    color = Color(0xFFFFAD3B),
                    radius = w * 0.44f,
                    center = Offset(w * 0.5f, h * 0.52f)
                )

                // Top left flame detail
                drawCircle(
                    color = Color(0xFFFFAD3B),
                    radius = w * 0.24f,
                    center = Offset(w * 0.32f, h * 0.28f)
                )

                // Top right flame detail
                drawCircle(
                    color = Color(0xFFFFAD3B),
                    radius = w * 0.26f,
                    center = Offset(w * 0.68f, h * 0.32f)
                )

                // Left Eye (white)
                scale(scaleX = 1.0f, scaleY = eyeScaleY, pivot = Offset(w * 0.39f, h * 0.48f)) {
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(w * 0.33f, h * 0.38f),
                        size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.20f)
                    )
                }

                // Right Eye (white)
                scale(scaleX = 1.0f, scaleY = eyeScaleY, pivot = Offset(w * 0.61f, h * 0.48f)) {
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(w * 0.55f, h * 0.38f),
                        size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.20f)
                    )
                }
            }
        }
    }
}
