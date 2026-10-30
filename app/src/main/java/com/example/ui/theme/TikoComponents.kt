package com.example.ui.theme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun TikoDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
    actions: @Composable () -> Unit
) {
    val tikoColors = LocalTikoThemeColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        BackHandler(onBack = onDismiss)

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(tikoColors.cardBg)
                    .border(1.dp, tikoColors.cardBorder, RoundedCornerShape(32.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume taps inside */ }
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title.uppercase(),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = tikoColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    content()

                    Spacer(modifier = Modifier.height(24.dp))

                    actions()
                }
            }
        }
    }
}

@Composable
fun TikoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val tikoColors = LocalTikoThemeColors.current
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            color = tikoColors.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(tikoColors.cardBg)
                .border(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = if (isFocused) Color(0xFF6B4EFF) else tikoColors.cardBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = {
                        if (placeholder.isNotBlank()) {
                            Text(
                                text = placeholder,
                                fontFamily = OutfitFontFamily,
                                color = tikoColors.textMuted,
                                fontSize = 15.sp
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedTextColor = tikoColors.textPrimary,
                        unfocusedTextColor = tikoColors.textPrimary
                    )
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }
    }
}

@Composable
fun TikoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val tikoColors = LocalTikoThemeColors.current
    val bg = if (tikoColors.isDark) Color(0xFFF5F1E6) else Color(0xFF151515)
    val fg = if (tikoColors.isDark) Color(0xFF151515) else Color.White

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(if (enabled) bg else bg.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = fg
        )
    }
}

@Composable
fun TikoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val tikoColors = LocalTikoThemeColors.current

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(tikoColors.cardBg)
            .border(1.5.dp, tikoColors.cardBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = tikoColors.textPrimary
        )
    }
}

@Composable
fun TikoDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val destructiveColor = Color(0xFFFF3B30)

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(if (enabled) destructiveColor else destructiveColor.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}
