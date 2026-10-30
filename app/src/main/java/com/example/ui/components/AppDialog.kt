package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.LocalTikoThemeColors
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TikoDialog
import com.example.ui.theme.TikoPrimaryButton
import com.example.ui.theme.TikoSecondaryButton
import com.example.ui.theme.TikoDestructiveButton

sealed class DialogData {
    data class Confirm(
        val title: String,
        val message: String,
        val confirmText: String,
        val cancelText: String = "Cancel",
        val destructive: Boolean = false,
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit = {}
    ) : DialogData()

    data class Alert(
        val title: String,
        val message: String,
        val buttonText: String = "OK",
        val onDismiss: () -> Unit = {}
    ) : DialogData()

    data class Custom(
        val onDismiss: () -> Unit,
        val content: @Composable () -> Unit
    ) : DialogData()
}

object AppDialogController {
    var currentDialog by mutableStateOf<DialogData?>(null)
        private set

    fun confirm(
        title: String,
        message: String,
        confirmText: String,
        cancelText: String = "Cancel",
        destructive: Boolean = false,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        currentDialog = DialogData.Confirm(
            title = title,
            message = message,
            confirmText = confirmText,
            cancelText = cancelText,
            destructive = destructive,
            onConfirm = {
                currentDialog = null
                onConfirm()
            },
            onCancel = {
                onCancel()
                currentDialog = null
            }
        )
    }

    fun alert(
        title: String,
        message: String,
        buttonText: String = "OK",
        onDismiss: () -> Unit = {}
    ) {
        currentDialog = DialogData.Alert(
            title = title,
            message = message,
            buttonText = buttonText,
            onDismiss = {
                currentDialog = null
                onDismiss()
            }
        )
    }

    fun showCustom(
        onDismiss: () -> Unit,
        content: @Composable () -> Unit
    ) {
        currentDialog = DialogData.Custom(
            onDismiss = {
                currentDialog = null
                onDismiss()
            },
            content = content
        )
    }

    fun dismiss() {
        val dialog = currentDialog
        currentDialog = null
        when (dialog) {
            is DialogData.Confirm -> dialog.onCancel()
            is DialogData.Alert -> dialog.onDismiss()
            is DialogData.Custom -> dialog.onDismiss()
            null -> {}
        }
    }
}

/**
 * AppOverlayHost:
 * Placed once at the app root, above content and above the bottom navigation.
 * Renders both AppDialogHost and AppToastHost.
 */
@Composable
fun AppOverlayHost(modifier: Modifier = Modifier) {
    val dialog = AppDialogController.currentDialog

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(9999f)
    ) {
        // 1. Dialog Host
        if (dialog != null) {
            BackHandler {
                AppDialogController.dismiss()
            }
            when (dialog) {
                is DialogData.Confirm -> {
                    ConfirmDialogCard(data = dialog)
                }
                is DialogData.Alert -> {
                    AlertPopupCard(data = dialog)
                }
                is DialogData.Custom -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.40f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                AppDialogController.dismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable(
                                     interactionSource = remember { MutableInteractionSource() },
                                     indication = null
                                ) { /* Consume click */ }
                        ) {
                            dialog.content()
                        }
                    }
                }
            }
        }

        // 2. Toast Host (slides in from the top)
        AppToastHost()
    }
}

@Composable
private fun ConfirmDialogCard(data: DialogData.Confirm) {
    val tikoColors = LocalTikoThemeColors.current
    val isDark = tikoColors.isDark

    val icon = when {
        data.destructive -> Icons.Outlined.DeleteOutline
        data.title.contains("remind", ignoreCase = true) || data.title.contains("alert", ignoreCase = true) -> Icons.Outlined.WarningAmber
        else -> Icons.Outlined.HelpOutline
    }
    val pillLabel = if (data.destructive) "PERMANENT ACTION" else "CONFIRM"

    TikoDialog(
        title = data.title,
        onDismiss = data.onCancel,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Pill tag with icon
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(if (data.destructive) (if (isDark) Color(0xFF3B1E1E) else Color(0xFFFFEEEE)) else (if (isDark) Color(0xFF1E1E1E) else Color(0xFFF2F0E8)))
                        .border(1.dp, if (data.destructive) Color(0xFFFF3B30) else (if (isDark) tikoColors.cardBorder else Color(0xFF151515)), CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (data.destructive) Color(0xFFFF3B30) else tikoColors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pillLabel,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (data.destructive) Color(0xFFFF3B30) else tikoColors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Message in Outfit Regular 16sp
                Text(
                    text = data.message,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = tikoColors.textSecondary,
                    lineHeight = 22.sp
                )
            }
        },
        actions = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_confirm_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (data.destructive) {
                    TikoDestructiveButton(
                        text = data.confirmText,
                        onClick = data.onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_primary_button")
                    )
                } else {
                    TikoPrimaryButton(
                        text = data.confirmText,
                        onClick = data.onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_primary_button")
                    )
                }
                TikoSecondaryButton(
                    text = data.cancelText,
                    onClick = data.onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_secondary_button")
                )
            }
        }
    )
}

@Composable
private fun AlertPopupCard(data: DialogData.Alert) {
    val tikoColors = LocalTikoThemeColors.current
    val isDark = tikoColors.isDark

    TikoDialog(
        title = data.title,
        onDismiss = data.onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_alert_dialog")
            ) {
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFF2F0E8))
                        .border(1.dp, if (isDark) tikoColors.cardBorder else Color(0xFF151515), CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = tikoColors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NOTICE",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = tikoColors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = data.message,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = tikoColors.textSecondary,
                    lineHeight = 22.sp
                )
            }
        },
        actions = {
            TikoPrimaryButton(
                text = data.buttonText,
                onClick = data.onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_primary_button")
            )
        }
    )
}
