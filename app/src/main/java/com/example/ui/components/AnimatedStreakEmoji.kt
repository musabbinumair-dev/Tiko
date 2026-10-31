package com.example.ui.components

import android.content.Context
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.min

/**
 * Exact SVG paths extracted from emoji.svg
 */
private object EmojiSvgDefaults {
    const val BODY_PATH =
        "M147.64,91.18C147.64,125.97,119.43,154.18,84.64,154.18C49.85,154.18,21.64,125.97,21.64,91.18C21.64,56.39,49.85,28.18,84.64,28.18C119.43,28.18,147.64,56.39,147.64,91.18C147.64,91.18,147.64,91.18,147.64,91.18Z"
    const val FLAME_RIGHT_PATH =
        "M96.64,21.18C105.84,16.78,127.47,43.68,137.14,57.68C137.14,57.68,108.14,82.68,108.14,82.68C100.47,64.01,87.44,25.58,96.64,21.18C96.64,21.18,96.64,21.18,96.64,21.18Z"
    const val FLAME_LEFT_PATH =
        "M19.58,39.26C29.45,36.71,46.63,58.18,52.29,82.87C52.29,82.87,59.14,135.18,59.14,135.18C59.14,135.18,32.47,126,32.47,126C10.97,101.5,9.71,41.81,19.58,39.26C19.58,39.26,19.58,39.26,19.58,39.26Z"
    const val FLAME_TOP_PATH =
        "M30.64,3.18C47.94,-10.01,97.64,19.18,125.64,64.83C125.64,64.83,61.21,107.18,61.21,107.18C44.18,75.56,20.14,11.18,30.64,3.18C30.64,3.18,30.64,3.18,30.64,3.18Z"
    const val EYE_LEFT_PATH =
        "M67.47,61C67.47,61,67.47,61,67.47,61C71.89,61,75.47,64.58,75.47,69C75.47,69,75.47,83,75.47,83C75.47,87.42,71.89,91,67.47,91C67.47,91,67.47,91,67.47,91C63.05,91,59.47,87.42,59.47,83C59.47,83,59.47,69,59.47,69C59.47,64.58,63.05,61,67.47,61C67.47,61,67.47,61,67.47,61Z"
    const val EYE_RIGHT_PATH =
        "M107.47,61C107.47,61,107.47,61,107.47,61C111.89,61,115.47,64.58,115.47,69C115.47,69,115.47,83,115.47,83C115.47,87.42,111.89,91,107.47,91C107.47,91,107.47,91,107.47,91C103.05,91,99.47,87.42,99.47,83C99.47,83,99.47,69,99.47,69C99.47,64.58,103.05,61,107.47,61C107.47,61,107.47,61,107.47,61Z"
}

private class SvgPaths(
    val body: Path,
    val flameRight: Path,
    val flameLeft: Path,
    val flameTop: Path,
    val eyeLeft: Path,
    val eyeRight: Path
)

private fun loadEmojiSvgPaths(context: Context): SvgPaths {
    var bodyStr = EmojiSvgDefaults.BODY_PATH
    var flameRightStr = EmojiSvgDefaults.FLAME_RIGHT_PATH
    var flameLeftStr = EmojiSvgDefaults.FLAME_LEFT_PATH
    var flameTopStr = EmojiSvgDefaults.FLAME_TOP_PATH
    var eyeLeftStr = EmojiSvgDefaults.EYE_LEFT_PATH
    var eyeRightStr = EmojiSvgDefaults.EYE_RIGHT_PATH

    try {
        context.assets.open("emoji.svg").use { stream ->
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(stream)
            val pathNodes = doc.getElementsByTagName("path")
            val extracted = mutableListOf<String>()
            for (i in 0 until pathNodes.length) {
                val element = pathNodes.item(i) as? Element
                val d = element?.getAttribute("d")
                if (!d.isNullOrEmpty()) {
                    extracted.add(d)
                }
            }
            if (extracted.size >= 5) {
                bodyStr = extracted[0]
                flameRightStr = extracted[1]
                flameLeftStr = extracted[2]
                flameTopStr = extracted[3]
                eyeLeftStr = extracted[4]
                eyeRightStr = extracted.last()
            }
        }
    } catch (_: Throwable) {
        // Fallback to embedded constants from emoji.svg
    }

    fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

    return SvgPaths(
        body = parse(bodyStr),
        flameRight = parse(flameRightStr),
        flameLeft = parse(flameLeftStr),
        flameTop = parse(flameTopStr),
        eyeLeft = parse(eyeLeftStr),
        eyeRight = parse(eyeRightStr)
    )
}

/**
 * Linear keyframe interpolation matching SVG SMIL specification
 */
private fun interpolate1D(progress: Float, keyTimes: FloatArray, values: FloatArray): Float {
    if (progress <= keyTimes[0]) return values[0]
    if (progress >= keyTimes.last()) return values.last()

    for (i in 0 until keyTimes.size - 1) {
        val t0 = keyTimes[i]
        val t1 = keyTimes[i + 1]
        if (progress in t0..t1) {
            val ratio = if (t1 > t0) (progress - t0) / (t1 - t0) else 0f
            return values[i] + ratio * (values[i + 1] - values[i])
        }
    }
    return values.last()
}

private fun interpolate2D(progress: Float, keyTimes: FloatArray, points: Array<Offset>): Offset {
    if (progress <= keyTimes[0]) return points[0]
    if (progress >= keyTimes.last()) return points.last()

    for (i in 0 until keyTimes.size - 1) {
        val t0 = keyTimes[i]
        val t1 = keyTimes[i + 1]
        if (progress in t0..t1) {
            val ratio = if (t1 > t0) (progress - t0) / (t1 - t0) else 0f
            val x = points[i].x + ratio * (points[i + 1].x - points[i].x)
            val y = points[i].y + ratio * (points[i + 1].y - points[i].y)
            return Offset(x, y)
        }
    }
    return points.last()
}

// Keyframes from emoji.svg
private val FLAME_KEY_TIMES = floatArrayOf(
    0f, 0.02f, 0.133334f, 0.246666f, 0.373334f, 0.493333f, 0.606667f, 0.693334f, 0.793333f, 1f
)

// Flame 1 (Right): translate deltas relative to (115.308, 51.69)
private val FLAME_RIGHT_DELTAS = arrayOf(
    Offset(0f, 0f),
    Offset(0f, 0f),
    Offset(105.502f - 115.308f, 46.51f - 51.69f),
    Offset(117.439f - 115.308f, 52.479f - 51.69f),
    Offset(112.323f - 115.308f, 46.51f - 51.69f),
    Offset(0f, 0f),
    Offset(105.502f - 115.308f, 46.51f - 51.69f),
    Offset(117.439f - 115.308f, 52.479f - 51.69f),
    Offset(108.486f - 115.308f, 46.51f - 51.69f),
    Offset(108.486f - 115.308f, 46.51f - 51.69f)
)

// Flame 2 (Left): translate deltas relative to (36.395, 87.117)
private val FLAME_LEFT_DELTAS = arrayOf(
    Offset(0f, 0f),
    Offset(0f, 0f),
    Offset(34.263f - 36.395f, 77.5f - 87.117f),
    Offset(38.953f - 36.395f, 91.569f - 87.117f),
    Offset(38.526f - 36.395f, 86.027f - 87.117f),
    Offset(0f, 0f),
    Offset(34.263f - 36.395f, 77.5f - 87.117f),
    Offset(38.953f - 36.395f, 91.569f - 87.117f),
    Offset(38.526f - 36.395f, 86.027f - 87.117f),
    Offset(38.526f - 36.395f, 86.027f - 87.117f)
)

// Flame 3 (Top): translate deltas relative to (76.858, 53.588)
private val FLAME_TOP_DELTAS = arrayOf(
    Offset(0f, 0f),
    Offset(0f, 0f),
    Offset(80.695f - 76.858f, 65.952f - 53.588f),
    Offset(74f - 76.858f, 53.592f - 53.588f),
    Offset(80.821f - 76.858f, 62.972f - 53.588f),
    Offset(0f, 0f),
    Offset(80.695f - 76.858f, 65.952f - 53.588f),
    Offset(74f - 76.858f, 53.592f - 53.588f),
    Offset(80.821f - 76.858f, 62.972f - 53.588f),
    Offset(80.821f - 76.858f, 62.972f - 53.588f)
)

// Eyes shift keytimes and translations
private val EYE_SHIFT_KEY_TIMES = floatArrayOf(
    0f, 0.04f, 0.193333f, 0.24f, 0.473333f, 0.506666f, 0.686667f, 0.713333f, 1f
)
private val EYE_SHIFT_DELTAS = arrayOf(
    Offset(0f, 1.5f),
    Offset(14f, 1.5f),
    Offset(14f, 1.5f),
    Offset(0f, 1.5f),
    Offset(0f, 1.5f),
    Offset(15f, 1.5f),
    Offset(15f, 1.5f),
    Offset(0f, 1.5f),
    Offset(0f, 1.5f)
)

// Eyes blink keytimes and scaleY
private val EYE_BLINK_KEY_TIMES = floatArrayOf(
    0f, 0.026666f, 0.046667f, 0.066667f, 0.313333f, 0.34f, 0.373334f, 0.6f, 0.626667f, 0.653333f, 1f
)
private val EYE_BLINK_SCALE_Y = floatArrayOf(
    1f, 1f, 0.2f, 1f, 1f, 0.2f, 1f, 1f, 0.2f, 1f, 1f
)

/**
 * Animated streak emoji icon directly rendering emoji.svg paths and animations
 * in hardware-accelerated Compose Canvas.
 */
@Composable
fun AnimatedStreakEmoji(
    modifier: Modifier = Modifier.size(20.dp)
) {
    val context = LocalContext.current
    val paths = remember(context) { loadEmojiSvgPaths(context) }

    val transition = rememberInfiniteTransition(label = "emoji_svg_anim")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "svg_progress"
    )

    // Compute animated offsets and scales from emoji.svg keyframes
    val flameRightOffset = remember(progress) {
        interpolate2D(progress, FLAME_KEY_TIMES, FLAME_RIGHT_DELTAS)
    }
    val flameLeftOffset = remember(progress) {
        interpolate2D(progress, FLAME_KEY_TIMES, FLAME_LEFT_DELTAS)
    }
    val flameTopOffset = remember(progress) {
        interpolate2D(progress, FLAME_KEY_TIMES, FLAME_TOP_DELTAS)
    }
    val eyesShiftOffset = remember(progress) {
        interpolate2D(progress, EYE_SHIFT_KEY_TIMES, EYE_SHIFT_DELTAS)
    }
    val eyeScaleY = remember(progress) {
        interpolate1D(progress, EYE_BLINK_KEY_TIMES, EYE_BLINK_SCALE_Y)
    }

    val flameColor = Color(0xFFFFAD3B)
    val eyeColor = Color.White

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val canvasW = size.width
            val canvasH = size.height

            // The original SVG has viewBox="0 0 512 512"
            // Outer group: transform="matrix(2.462, 0, 0, 2.462, 73.806, 65.189)"
            // Inside group: clip-path 148x155
            val scaleFactor = min(canvasW, canvasH) / 512f
            val combinedScale = scaleFactor * 2.462f
            val originX = (canvasW - 512f * scaleFactor) / 2f + scaleFactor * 73.806f
            val originY = (canvasH - 512f * scaleFactor) / 2f + scaleFactor * 65.189f

            translate(left = originX, top = originY) {
                scale(scale = combinedScale, pivot = Offset.Zero) {
                    clipRect(left = 0f, top = 0f, right = 148f, bottom = 155f) {
                        // 1. Base Flame Body (Child 0)
                        drawPath(path = paths.body, color = flameColor)

                        // 2. Right Flame Tendril (Child 1)
                        translate(left = flameRightOffset.x, top = flameRightOffset.y) {
                            drawPath(path = paths.flameRight, color = flameColor)
                        }

                        // 3. Left Flame Tendril (Child 2)
                        translate(left = flameLeftOffset.x, top = flameLeftOffset.y) {
                            drawPath(path = paths.flameLeft, color = flameColor)
                        }

                        // 4. Top/Center Flame Tendril (Child 3)
                        translate(left = flameTopOffset.x, top = flameTopOffset.y) {
                            drawPath(path = paths.flameTop, color = flameColor)
                        }

                        // 5. Animated Eyes (Child 4)
                        translate(left = eyesShiftOffset.x, top = eyesShiftOffset.y) {
                            // Left Eye - pivot at (67.47, 76)
                            scale(
                                scaleX = 1f,
                                scaleY = eyeScaleY,
                                pivot = Offset(67.47f, 76f)
                            ) {
                                drawPath(path = paths.eyeLeft, color = eyeColor)
                            }

                            // Right Eye - pivot at (107.47, 76)
                            scale(
                                scaleX = 1f,
                                scaleY = eyeScaleY,
                                pivot = Offset(107.47f, 76f)
                            ) {
                                drawPath(path = paths.eyeRight, color = eyeColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
