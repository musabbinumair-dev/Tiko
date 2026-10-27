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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

enum class ToastType {
    Error,
    Success,
    Warning,
    Info
}

data class ToastData(
    val id: Long,
    val message: String,
    val type: ToastType
)

object AppToastController {
    var currentToast by mutableStateOf<ToastData?>(null)
        private set

    private var nextId = 0L

    fun show(message: String, type: ToastType = ToastType.Info) {
        currentToast = ToastData(
            id = ++nextId,
            message = message,
            type = type
        )
    }

    fun dismiss() {
        currentToast = null
    }
}

@Composable
fun AppToastHost(modifier: Modifier = Modifier) {
    val toast = AppToastController.currentToast

    LaunchedEffect(toast?.id) {
        if (toast != null) {
            val duration = if (toast.type == ToastType.Error) 4000L else 3000L
            delay(duration)
            if (AppToastController.currentToast?.id == toast.id) {
                AppToastController.dismiss()
            }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.75f)
            ) { -it } + fadeIn(animationSpec = spring(dampingRatio = 0.75f)),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.75f)
            ) { -it } + fadeOut(animationSpec = spring(dampingRatio = 0.75f))
        ) {
            if (toast != null) {
                val (bgColor, textColor, typeIcon) = when (toast.type) {
                    ToastType.Error -> Triple(Color(0xFFD9534A), Color.White, Icons.Outlined.ErrorOutline)
                    ToastType.Success -> Triple(Color(0xFFC8DB6A), Color(0xFF151515), Icons.Outlined.CheckCircle)
                    ToastType.Warning -> Triple(Color(0xFFFFB92E), Color(0xFF151515), Icons.Outlined.WarningAmber)
                    ToastType.Info -> Triple(Color(0xFF6B4EFF), Color.White, Icons.Outlined.Info)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                        .padding(horizontal = 20.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = Color.Black.copy(alpha = 0.10f),
                            ambientColor = Color.Black.copy(alpha = 0.10f)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(bgColor)
                        .pointerInput(toast.id) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -8f) {
                                    AppToastController.dismiss()
                                }
                            }
                        }
                        .padding(16.dp)
                        .testTag("app_toast")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // White pill tag top-left: 32dp high, 18dp icon + type name, Outfit Medium 14sp, black
                            Row(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFF151515), CircleShape)
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    tint = Color(0xFF151515),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = toast.type.name,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color(0xFF151515)
                                )
                            }

                            // 40dp white circle with black X at top-right to dismiss
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFF151515), CircleShape)
                                    .clickable { AppToastController.dismiss() }
                                    .testTag("app_toast_dismiss"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFF151515),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Message in Outfit Bold 18sp (max 2 lines)
                        Text(
                            text = toast.message,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
