package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryModel(
    val name: String,
    val iconKey: String = "folder",
    val colorHex: Long = 0xFF6B4EFF
)

object CategoryIcons {
    data class CategoryIconOption(
        val key: String,
        val label: String,
        val icon: ImageVector
    )

    val ALL_OPTIONS = listOf(
        CategoryIconOption("work", "Work", Icons.Outlined.Work),
        CategoryIconOption("health", "Health", Icons.Outlined.FitnessCenter),
        CategoryIconOption("personal", "Personal", Icons.Outlined.Person),
        CategoryIconOption("study", "Study", Icons.AutoMirrored.Outlined.MenuBook),
        CategoryIconOption("shopping", "Shopping", Icons.Outlined.ShoppingCart),
        CategoryIconOption("travel", "Travel", Icons.Outlined.Flight),
        CategoryIconOption("ideas", "Ideas", Icons.Outlined.Lightbulb),
        CategoryIconOption("finance", "Finance", Icons.Outlined.AccountBalanceWallet),
        CategoryIconOption("home", "Home", Icons.Outlined.Home),
        CategoryIconOption("star", "Star", Icons.Outlined.Star),
        CategoryIconOption("music", "Music", Icons.Outlined.MusicNote),
        CategoryIconOption("heart", "Heart", Icons.Outlined.Favorite),
        CategoryIconOption("cafe", "Cafe", Icons.Outlined.LocalCafe),
        CategoryIconOption("folder", "Folder", Icons.Outlined.Folder),
        CategoryIconOption("checklist", "Tasks", Icons.Outlined.Checklist),
        CategoryIconOption("car", "Car", Icons.Outlined.DirectionsCar),
        CategoryIconOption("art", "Art", Icons.Outlined.Palette),
        CategoryIconOption("computer", "Code", Icons.Outlined.Computer)
    )

    fun getIcon(keyOrName: String?): ImageVector {
        if (keyOrName == null) return Icons.Outlined.Folder
        val clean = keyOrName.lowercase().trim()
        val matchOption = ALL_OPTIONS.find { it.key == clean || it.label.lowercase() == clean }
        if (matchOption != null) return matchOption.icon

        return when {
            clean.contains("work") || clean.contains("job") || clean.contains("office") -> Icons.Outlined.Work
            clean.contains("health") || clean.contains("gym") || clean.contains("fit") -> Icons.Outlined.FitnessCenter
            clean.contains("personal") || clean.contains("self") || clean.contains("me") -> Icons.Outlined.Person
            clean.contains("study") || clean.contains("book") || clean.contains("learn") || clean.contains("read") -> Icons.AutoMirrored.Outlined.MenuBook
            clean.contains("shop") || clean.contains("buy") || clean.contains("store") -> Icons.Outlined.ShoppingCart
            clean.contains("travel") || clean.contains("trip") || clean.contains("fly") -> Icons.Outlined.Flight
            clean.contains("idea") || clean.contains("brain") || clean.contains("creative") -> Icons.Outlined.Lightbulb
            clean.contains("finance") || clean.contains("money") || clean.contains("budget") -> Icons.Outlined.AccountBalanceWallet
            clean.contains("home") || clean.contains("house") -> Icons.Outlined.Home
            clean.contains("star") || clean.contains("goal") -> Icons.Outlined.Star
            clean.contains("music") || clean.contains("song") -> Icons.Outlined.MusicNote
            clean.contains("heart") || clean.contains("love") -> Icons.Outlined.Favorite
            clean.contains("cafe") || clean.contains("coffee") || clean.contains("food") -> Icons.Outlined.LocalCafe
            clean.contains("car") || clean.contains("drive") -> Icons.Outlined.DirectionsCar
            clean.contains("art") || clean.contains("design") -> Icons.Outlined.Palette
            clean.contains("code") || clean.contains("tech") || clean.contains("dev") -> Icons.Outlined.Computer
            else -> Icons.Outlined.Folder
        }
    }
}

object CategoryColors {
    val PALETTE = listOf(
        0xFF6B4EFF, // CardPurple
        0xFFFFB92E, // CardYellow
        0xFF151515, // CardBlack
        0xFFC8DB6A, // CardLime
        0xFFD9534A, // Coral Red
        0xFF2CB8A7, // Teal
        0xFF4E93FF, // Sky Blue
        0xFFE056FD  // Neon Pink
    )

    fun getContentColor(colorHex: Long): Color {
        return if (colorHex == 0xFFFFB92E || colorHex == 0xFFC8DB6A) {
            Color(0xFF151515)
        } else {
            Color.White
        }
    }
}

val DefaultCategories = listOf(
    CategoryModel("Work", "work", 0xFF6B4EFF),
    CategoryModel("Health", "health", 0xFFFFB92E),
    CategoryModel("Personal", "personal", 0xFF151515),
    CategoryModel("Study", "study", 0xFFC8DB6A)
)
