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


import com.example.ui.theme.LocalTikoThemeColors
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TikoDialog
import com.example.ui.theme.TikoTextField
import com.example.ui.theme.TikoPrimaryButton
import com.example.ui.theme.TikoSecondaryButton
import androidx.compose.ui.graphics.Brush


@Composable
fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onCreateCategory: (name: String, iconKey: String, colorHex: Long) -> Unit,
    titleText: String = "NEW LIST",
    isDark: Boolean = false
) {
    val tikoColors = LocalTikoThemeColors.current
    val effectiveDark = isDark || tikoColors.isDark

    var categoryName by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("work") }
    var selectedColorHex by remember { mutableStateOf(CategoryColors.PALETTE[0]) }

    TikoDialog(
        title = titleText,
        onDismiss = onDismiss,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Name Input using TikoTextField
                TikoTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = "Category Name",
                    placeholder = "e.g. Travel, Finance, Projects",
                    modifier = Modifier.testTag("category_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select Icon Label
                Text(
                    text = "SELECT ICON",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = tikoColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Icon Picker Grid (6 columns of 48dp circles with scroll and bottom fade)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CategoryIcons.ALL_OPTIONS) { opt ->
                            val isSelected = selectedIconKey == opt.key
                            val unselBg = if (effectiveDark) Color(0xFF1E1E1E) else Color.White
                            val selBg = if (effectiveDark) Color(0xFFF5F1E6) else AppBlack
                            val selBorder = if (effectiveDark) Color(0xFFF5F1E6) else AppBlack
                            val unselBorder = if (effectiveDark) tikoColors.cardBorder else AppBlack.copy(alpha = 0.2f)

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
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
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    // Subtle fade at the bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, tikoColors.cardBg)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select Color Label
                Text(
                    text = "SELECT COLOR",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = tikoColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color Picker Row (40dp swatches)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CategoryColors.PALETTE.forEach { colorVal ->
                        val isSelected = selectedColorHex == colorVal
                        val isBlackSwatch = colorVal == 0xFF000000L || colorVal == 0xFF151515L
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .then(
                                    if (effectiveDark && isBlackSwatch) {
                                        Modifier.border(1.dp, Color(0xFF3A3A3A), CircleShape)
                                    } else Modifier
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) tikoColors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = colorVal }
                                .testTag("color_pick_$colorVal")
                        )
                    }
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TikoSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                TikoPrimaryButton(
                    text = "Create",
                    onClick = {
                        onCreateCategory(
                            categoryName.trim(),
                            selectedIconKey,
                            selectedColorHex
                        )
                    },
                    enabled = categoryName.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_category_confirm_button")
                )
            }
        }
    )
}
