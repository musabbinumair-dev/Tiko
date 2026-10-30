package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.local.UserPreferences
import com.example.ui.theme.OutfitFontFamily
import com.google.firebase.auth.FirebaseAuth

private val AvatarYellow = Color(0xFFFFB92E)
private val AvatarBlack = Color(0xFF151515)

/**
 * Avatar option representing a preset emoji avatar.
 */
data class AvatarOption(
    val id: String,
    val name: String,
    val emoji: String,
    val backgroundColor: Color,
    val borderColor: Color? = null
)

/**
 * The 12 preset avatars cycling through yellow #FFB92E, purple #6B4EFF, lime #BAE136, and dark #2B2B2B.
 */
val PresetAvatars: List<AvatarOption> = listOf(
    AvatarOption("cat", "Cat", "🐱", Color(0xFFFFB92E)),
    AvatarOption("star", "Star", "⭐", Color(0xFF6B4EFF)),
    AvatarOption("sparkle", "Sparkle", "✨", Color(0xFFBAE136)),
    AvatarOption("rocket", "Rocket", "🚀", Color(0xFF2B2B2B), Color(0xFF3A3A3A)),

    AvatarOption("smile", "Smile", "😊", Color(0xFFFFB92E)),
    AvatarOption("cool", "Cool", "😎", Color(0xFF6B4EFF)),
    AvatarOption("fire", "Fire", "🔥", Color(0xFFBAE136)),
    AvatarOption("coffee", "Coffee", "☕", Color(0xFF2B2B2B), Color(0xFF3A3A3A)),

    AvatarOption("heart", "Heart", "❤️", Color(0xFFFFB92E)),
    AvatarOption("music", "Music", "🎵", Color(0xFF6B4EFF)),
    AvatarOption("target", "Target", "🎯", Color(0xFFBAE136)),
    AvatarOption("crown", "Crown", "👑", Color(0xFF2B2B2B), Color(0xFF3A3A3A))
)

fun getAvatarById(id: String?): AvatarOption? {
    if (id.isNullOrBlank()) return null
    return PresetAvatars.find { it.id.equals(id.trim(), ignoreCase = true) }
}

/**
 * Reusable user avatar composable.
 * - When an avatarId is saved/provided, displays that preset avatar (emoji on colored circle).
 * - With no avatarId:
 *   - For Google users: loads their photo (FirebaseAuth.currentUser.photoUrl) via Coil.
 *     Upgrades '=s96-c' URLs to '=s400-c' for crisp high-resolution rendering.
 *     While loading or on failure, falls back to the yellow circle with first initial.
 *   - For Email/Password users: displays the yellow circle with their first initial.
 * - Handles null/blank names, photos, or avatar IDs safely without crashing.
 * - Supports optional onClick or custom modifier with click listener bounded to the circle.
 */
@Composable
fun UserAvatar(
    size: Dp = 52.dp,
    modifier: Modifier = Modifier,
    name: String? = null,
    photoUrl: String? = null,
    avatarId: String? = null,
    fontSize: TextUnit = (size.value * 0.40f).sp,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val preferences = remember(context) { UserPreferences(context) }
    val savedAvatarId by preferences.avatarId.collectAsState()
    val effectiveAvatarId = avatarId ?: savedAvatarId

    val selectedPreset = getAvatarById(effectiveAvatarId)

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    val combinedModifier = Modifier
        .then(clickModifier)
        .then(modifier)

    if (selectedPreset != null) {
        val emojiFontSize = (size.value * 0.47f).sp
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(selectedPreset.backgroundColor)
                .then(
                    if (selectedPreset.borderColor != null) {
                        Modifier.border(1.dp, selectedPreset.borderColor, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .then(combinedModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = selectedPreset.emoji,
                fontSize = emojiFontSize,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Throwable) {
        null
    }

    val effectiveName = name?.ifBlank { null }
        ?: currentUser?.displayName?.ifBlank { null }
        ?: currentUser?.email?.ifBlank { null }
        ?: ""

    val rawPhotoUrl = photoUrl?.ifBlank { null }
        ?: try { currentUser?.photoUrl?.toString()?.ifBlank { null } } catch (_: Throwable) { null }

    val highResPhotoUrl = if (rawPhotoUrl != null) {
        if (rawPhotoUrl.endsWith("=s96-c")) {
            rawPhotoUrl.replace("=s96-c", "=s400-c")
        } else if (rawPhotoUrl.contains("=s96-c")) {
            rawPhotoUrl.replace("=s96-c", "=s400-c")
        } else {
            rawPhotoUrl
        }
    } else {
        null
    }

    val initial = effectiveName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""

    if (highResPhotoUrl.isNullOrBlank()) {
        InitialAvatarFallback(
            size = size,
            initial = initial,
            fontSize = fontSize,
            modifier = combinedModifier
        )
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(highResPhotoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .then(combinedModifier),
            contentScale = ContentScale.Crop,
            loading = {
                InitialAvatarFallback(
                    size = size,
                    initial = initial,
                    fontSize = fontSize
                )
            },
            error = {
                InitialAvatarFallback(
                    size = size,
                    initial = initial,
                    fontSize = fontSize
                )
            }
        )
    }
}

@Composable
private fun InitialAvatarFallback(
    size: Dp,
    initial: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(modifier)
            .background(AvatarYellow),
        contentAlignment = Alignment.Center
    ) {
        if (initial.isNotBlank()) {
            Text(
                text = initial,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = AvatarBlack
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "User Avatar",
                tint = AvatarBlack,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}
