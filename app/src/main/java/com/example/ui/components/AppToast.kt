package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OutfitFontFamily
import kotlinx.coroutines.delay

import com.example.ui.theme.LocalTikoThemeColors
import com.example.ui.theme.OutfitFontFamily
import kotlinx.coroutines.delay

enum class ToastType {
    Error,
    Success,
    Warning,
    Info
}

data class ToastData(
    val id: Long,
    val message: String,
    val type: ToastType,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

object AppToastController {
    var currentToast by mutableStateOf<ToastData?>(null)
        private set

    private var nextId = 0L

    fun show(
        message: String,
        type: ToastType = ToastType.Info,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        currentToast = ToastData(
            id = ++nextId,
            message = message,
            type = type,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    fun dismiss() {
        currentToast = null
    }
}

@Composable
fun AppToastHost(modifier: Modifier = Modifier) {
    val toast = AppToastController.currentToast
    val tikoColors = LocalTikoThemeColors.current
    val isDark = tikoColors.isDark

    LaunchedEffect(toast?.id) {
        if (toast != null) {
            val duration = if (toast.onAction != null) 6000L else 3000L
            delay(duration)
            if (AppToastController.currentToast?.id == toast.id) {
                AppToastController.dismiss()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 80.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)
            ) { it / 2 } + fadeIn(animationSpec = androidx.compose.animation.core.tween(180)),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)
            ) { it / 2 } + fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
        ) {
            if (toast != null) {
                val accentColor = when (toast.type) {
                    ToastType.Error -> Color(0xFFD9534A)
                    ToastType.Success -> Color(0xFF34C759)
                    ToastType.Warning -> Color(0xFFFF9500)
                    ToastType.Info -> Color(0xFF6B4EFF)
                }
                val typeIcon = when (toast.type) {
                    ToastType.Error -> Icons.Outlined.ErrorOutline
                    ToastType.Success -> Icons.Outlined.CheckCircle
                    ToastType.Warning -> Icons.Outlined.WarningAmber
                    ToastType.Info -> Icons.Outlined.Info
                }

                val bg = if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515)
                val textColor = if (isDark) Color(0xFF151515) else Color.White
                val borderColor = if (isDark) tikoColors.cardBorder else Color.Transparent

                // Floating pill: cream in dark mode, black in light mode, 16sp medium
                Row(
                    modifier = Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = Color.Black.copy(alpha = 0.25f),
                            ambientColor = Color.Black.copy(alpha = 0.15f)
                        )
                        .clip(CircleShape)
                        .background(bg)
                        .then(if (isDark) Modifier.border(1.dp, borderColor, CircleShape) else Modifier)
                        .clickable { AppToastController.dismiss() }
                        .pointerInput(toast.id) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 8f || dragAmount < -8f) {
                                    AppToastController.dismiss()
                                }
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .testTag("app_toast"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = if (toast.type == ToastType.Error) Color(0xFFD9534A) else accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = toast.message,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (toast.actionLabel != null && toast.onAction != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF151515).copy(alpha = 0.1f) else Color(0xFF2C2C2E))
                                .clickable {
                                    val act = toast.onAction
                                    AppToastController.dismiss()
                                    act?.invoke()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = toast.actionLabel,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) Color(0xFF6B4EFF) else Color(0xFFFFB92E)
                            )
                        }
                    }
                }
            }
        }
    }
}
