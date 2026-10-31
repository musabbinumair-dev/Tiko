package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.LocalTikoThemeColors
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TikoDialog
import com.example.ui.theme.TikoPrimaryButton
import com.example.ui.theme.TikoSecondaryButton
import com.example.ui.theme.TikoDestructiveButton
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.musab.tiko.R

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

    data class Logout(
        val title: String = "Log out?",
        val message: String = "Are you sure you want to log out\nfrom your account?",
        val confirmText: String = "Log out",
        val cancelText: String = "Cancel",
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit = {}
    ) : DialogData()

    data class DeleteAccount(
        val title: String = "DELETE ACCOUNT?",
        val message: String = "This action is permanent and cannot be undone. Please type \"DELETE\" to confirm.",
        val confirmText: String = "Delete Account",
        val cancelText: String = "Cancel",
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

    fun showLogout(
        title: String = "Log out?",
        message: String = "Are you sure you want to log out\nfrom your account?",
        confirmText: String = "Log out",
        cancelText: String = "Cancel",
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        currentDialog = DialogData.Logout(
            title = title,
            message = message,
            confirmText = confirmText,
            cancelText = cancelText,
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

    fun showDeleteAccount(
        title: String = "DELETE ACCOUNT?",
        message: String = "This action is permanent and cannot be undone. Please type \"DELETE\" to confirm.",
        confirmText: String = "Delete Account",
        cancelText: String = "Cancel",
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        currentDialog = DialogData.DeleteAccount(
            title = title,
            message = message,
            confirmText = confirmText,
            cancelText = cancelText,
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
            is DialogData.Logout -> dialog.onCancel()
            is DialogData.DeleteAccount -> dialog.onCancel()
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
    val bottomSheetDialog = dialog?.asBottomSheet()

    // Keep the last bottom sheet in composition long enough to play its exit animation.
    var sheetData by remember { mutableStateOf<DialogData?>(null) }
    var sheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(bottomSheetDialog) {
        if (bottomSheetDialog != null) {
            sheetData = bottomSheetDialog
            sheetVisible = true
        } else if (sheetData != null) {
            sheetVisible = false
            delay(SheetExitDurationMillis + 20L)
            sheetData = null
        }
    }

    BackHandler(enabled = dialog != null) {
        AppDialogController.dismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(9999f)
    ) {
        // 1. Bottom sheet dialogs (draggable, animated, dismiss on outside tap)
        when (val data = sheetData) {
            is DialogData.DeleteAccount -> {
                DeleteAccountBottomSheet(data = data, visible = sheetVisible)
            }
            is DialogData.Logout -> {
                LogoutBottomSheet(data = data, visible = sheetVisible)
            }
            else -> Unit
        }

        // 2. Centered dialogs
        if (dialog != null && bottomSheetDialog == null) {
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
                else -> Unit
            }
        }

        // 3. Toast Host (slides in from the top)
        AppToastHost()
    }
}

private fun DialogData.asBottomSheet(): DialogData? = when (this) {
    is DialogData.DeleteAccount -> this
    is DialogData.Logout -> this
    is DialogData.Confirm -> when {
        title.contains("delete account", ignoreCase = true) -> DialogData.DeleteAccount(
            onConfirm = onConfirm,
            onCancel = onCancel
        )
        title.contains("log out", ignoreCase = true) || title.contains("logout", ignoreCase = true) -> DialogData.Logout(
            title = "Log out?",
            message = "Are you sure you want to log out\nfrom your account?",
            confirmText = confirmText,
            cancelText = cancelText,
            onConfirm = onConfirm,
            onCancel = onCancel
        )
        else -> null
    }
    else -> null
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

private const val SheetExitDurationMillis = 260L

@Composable
private fun TikoBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalTikoThemeColors.current.isDark
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 96.dp.toPx() }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = if (isDragging) {
            tween(0)
        } else {
            spring(dampingRatio = 0.82f, stiffness = 520f)
        },
        label = "bottomSheetDragOffset"
    )

    // Start hidden so the enter animation always plays the first time the sheet appears.
    val backdropVisibility = remember { MutableTransitionState(false) }
    backdropVisibility.targetState = visible
    val sheetVisibility = remember { MutableTransitionState(false) }
    sheetVisibility.targetState = visible

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed backdrop — fades in and out with the sheet.
        AnimatedVisibility(
            visibleState = backdropVisibility,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(SheetExitDurationMillis.toInt())),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = visible
                    ) {
                        onDismiss()
                    }
            )
        }

        // Sheet — slides in/out and follows the drag gesture.
        AnimatedVisibility(
            visibleState = sheetVisibility,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(SheetExitDurationMillis.toInt(), easing = FastOutLinearInEasing)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .offset { IntOffset(0, animatedDragOffset.roundToInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                if (dragOffset > dismissThresholdPx) {
                                    onDismiss()
                                } else {
                                    dragOffset = 0f
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffset = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    }
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume taps inside the sheet */ }
                    .testTag(testTag)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SheetDragHandle(isDark: Boolean) {
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (isDark) Color(0xFF383838) else Color(0xFFD6D6D6))
    )
}

@Composable
private fun LogoutBottomSheet(data: DialogData.Logout, visible: Boolean) {
    val isDark = LocalTikoThemeColors.current.isDark

    TikoBottomSheet(
        visible = visible,
        onDismiss = { AppDialogController.dismiss() },
        testTag = "logout_modal"
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        SheetDragHandle(isDark = isDark)

        Spacer(modifier = Modifier.height(28.dp))

        // Logout graphic from root Logout.png
        Image(
            painter = painterResource(id = R.drawable.logout_graphic),
            contentDescription = "Log out graphic",
            modifier = Modifier
                .size(112.dp)
                .testTag("logout_graphic"),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Title: "Log out?"
        Text(
            text = data.title,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("logout_dialog_title")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle / message
        Text(
            text = data.message,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = if (isDark) Color(0xFFA8A39A) else Color(0xFF5B5B5B),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("logout_dialog_message")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons Row: Cancel (light pill) & Log out (black pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("logout_dialog_actions"),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button (light neutral pill)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFF0EFEB))
                    .clickable {
                        data.onCancel()
                    }
                    .testTag("dialog_secondary_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = data.cancelText,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515)
                )
            }

            // Log out button (black pill)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515))
                    .clickable {
                        data.onConfirm()
                    }
                    .testTag("dialog_primary_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = data.confirmText,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color(0xFF151515) else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun DeleteAccountBottomSheet(data: DialogData.DeleteAccount, visible: Boolean) {
    val isDark = LocalTikoThemeColors.current.isDark
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    var confirmInput by remember { mutableStateOf("") }
    val isConfirmed = confirmInput.trim().equals("DELETE", ignoreCase = true)

    TikoBottomSheet(
        visible = visible,
        onDismiss = { AppDialogController.dismiss() },
        testTag = "delete_account_modal"
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        SheetDragHandle(isDark = isDark)

        Spacer(modifier = Modifier.height(22.dp))

        // Collapse the illustration while the keyboard is open so the whole
        // confirmation form stays visible above the IME.
        AnimatedVisibility(
            visible = !imeVisible,
            enter = fadeIn(animationSpec = tween(160)) + expandVertically(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(140)) + shrinkVertically(animationSpec = tween(220))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(6.dp))

                // Delete graphic from root Delete.png
                Image(
                    painter = painterResource(id = R.drawable.delete_graphic),
                    contentDescription = "Delete account graphic",
                    modifier = Modifier
                        .size(112.dp)
                        .testTag("delete_account_graphic"),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Title: "DELETE ACCOUNT?"
        Text(
            text = data.title,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("delete_account_dialog_title")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle / message
        Text(
            text = data.message,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = if (isDark) Color(0xFFA8A39A) else Color(0xFF5B5B5B),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_account_dialog_message")
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Pill confirmation input: "Type DELETE"
        BasicTextField(
            value = confirmInput,
            onValueChange = { confirmInput = it },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515)
            ),
            cursorBrush = SolidColor(if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515)),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF2A2A2A) else Color(0xFFF0EFEB))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (confirmInput.isEmpty()) {
                        Text(
                            text = "Type DELETE",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = if (isDark) Color(0xFF777777) else Color(0xFF8E8E93)
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_confirm_input")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Row: Cancel (light pill) & Delete Account (black pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_account_dialog_actions"),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button (light neutral pill)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFF0EFEB))
                    .clickable {
                        data.onCancel()
                    }
                    .testTag("dialog_secondary_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = data.cancelText,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515)
                )
            }

            // Delete Account button (black pill)
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConfirmed) {
                            if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515)
                        } else {
                            if (isDark) Color(0xFF333333) else Color(0xFF151515).copy(alpha = 0.5f)
                        }
                    )
                    .clickable {
                        if (isConfirmed) {
                            data.onConfirm()
                        } else {
                            AppToastController.show("Please type DELETE to confirm", ToastType.Warning)
                        }
                    }
                    .testTag("dialog_primary_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = data.confirmText,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isConfirmed) {
                        if (isDark) Color(0xFF151515) else Color.White
                    } else {
                        if (isDark) Color(0xFF777777) else Color.White.copy(alpha = 0.6f)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}


