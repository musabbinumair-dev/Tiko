package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.CategoryColors
import com.example.data.model.CategoryIcons
import com.example.ui.theme.LocalTikoThemeColors
import com.example.ui.theme.OutfitFontFamily


@Composable
fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onCreateCategory: (name: String, iconKey: String, colorHex: Long) -> Unit,
    titleText: String = "NEW CATEGORY",
    isDark: Boolean = false
) {
    val tikoColors = LocalTikoThemeColors.current
    val effectiveDark = isDark || tikoColors.isDark

    var categoryName by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("work") }
    var selectedColorHex by remember { mutableStateOf(CategoryColors.PALETTE[0]) }

    val cardBg = if (effectiveDark) tikoColors.cardBg else AppCreamBackground
    val cardBorderColor = if (effectiveDark) tikoColors.cardBorder else AppBlack

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(cardBg)
                .border(2.dp, cardBorderColor, RoundedCornerShape(26.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume taps inside */ }
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleText,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = tikoColors.textPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (effectiveDark) Color(0xFF1E1E1E) else Color.White)
                            .border(1.dp, if (effectiveDark) tikoColors.cardBorder else AppBlack, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = tikoColors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name Input
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    placeholder = {
                        Text(
                            text = "e.g. Travel, Finance, Projects",
                            fontFamily = OutfitFontFamily,
                            color = tikoColors.textMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (effectiveDark) Color(0xFF1E1E1E) else Color.White,
                        unfocusedContainerColor = if (effectiveDark) Color(0xFF1E1E1E) else Color.White,
                        focusedBorderColor = if (effectiveDark) tikoColors.textPrimary else AppBlack,
                        unfocusedBorderColor = if (effectiveDark) tikoColors.cardBorder else AppBlack.copy(alpha = 0.3f),
                        focusedTextColor = tikoColors.textPrimary,
                        unfocusedTextColor = tikoColors.textPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Select Icon Label
                Text(
                    text = "SELECT ICON",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = tikoColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Icon Picker Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CategoryIcons.ALL_OPTIONS) { opt ->
                        val isSelected = selectedIconKey == opt.key
                        val unselBg = if (effectiveDark) Color(0xFF1E1E1E) else Color.White
                        val selBg = if (effectiveDark) Color(0xFFF5F1E6) else AppBlack
                        val selBorder = if (effectiveDark) Color(0xFFF5F1E6) else AppBlack
                        val unselBorder = if (effectiveDark) tikoColors.cardBorder else AppBlack.copy(alpha = 0.2f)

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) selBg else unselBg)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) selBorder else unselBorder,
                                    shape = CircleShape
                                )
                                .clickable { selectedIconKey = opt.key }
                                .testTag("icon_pick_${opt.key}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = opt.icon,
                                contentDescription = opt.label,
                                tint = if (isSelected) (if (effectiveDark) Color(0xFF151515) else Color.White) else tikoColors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Color Label
                Text(
                    text = "SELECT COLOR",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = tikoColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryColors.PALETTE.forEach { colorVal ->
                        val isSelected = selectedColorHex == colorVal
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) (if (effectiveDark) Color(0xFFF5F1E6) else AppBlack) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = colorVal }
                                .testTag("color_pick_$colorVal")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(if (effectiveDark) Color(0xFF1E1E1E) else Color.White)
                            .border(1.dp, if (effectiveDark) tikoColors.cardBorder else AppBlack, CircleShape)
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = tikoColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    val createBtnBg = if (effectiveDark) {
                        if (categoryName.isNotBlank()) Color(0xFFF5F1E6) else Color(0xFFF5F1E6).copy(alpha = 0.3f)
                    } else {
                        if (categoryName.isNotBlank()) AppBlack else AppBlack.copy(alpha = 0.3f)
                    }
                    val createBtnTextColor = if (effectiveDark) Color(0xFF151515) else Color.White

                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(createBtnBg)
                            .clickable(enabled = categoryName.isNotBlank()) {
                                onCreateCategory(
                                    categoryName.trim(),
                                    selectedIconKey,
                                    selectedColorHex
                                )
                            }
                            .padding(horizontal = 22.dp)
                            .testTag("create_category_confirm_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = createBtnTextColor
                        )
                    }
                }
            }
        }
    }
}
