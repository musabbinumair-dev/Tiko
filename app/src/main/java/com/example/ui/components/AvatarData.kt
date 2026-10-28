package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

data class BuiltInAvatar(
    val id: String,
    val name: String,
    val bg: Color,
    val tint: Color,
    val icon: ImageVector
)

val BUILT_IN_AVATARS: List<BuiltInAvatar> = listOf(
    BuiltInAvatar("avatar_cat", "Cat", Color(0xFFFFB92E), Color(0xFF151515), Icons.Outlined.Pets),
    BuiltInAvatar("avatar_star", "Star", Color(0xFF6B4EFF), Color.White, Icons.Outlined.Star),
    BuiltInAvatar("avatar_spark", "Sparkle", Color(0xFFBAE136), Color(0xFF151515), Icons.Outlined.AutoAwesome),
    BuiltInAvatar("avatar_rocket", "Rocket", Color(0xFF151515), Color(0xFFFFB92E), Icons.Outlined.RocketLaunch),
    BuiltInAvatar("avatar_smile", "Smile", Color(0xFFFFB92E), Color(0xFF151515), Icons.Outlined.Mood),
    BuiltInAvatar("avatar_cool", "Cool", Color(0xFF6B4EFF), Color.White, Icons.Outlined.Face),
    BuiltInAvatar("avatar_fire", "Fire", Color(0xFFBAE136), Color(0xFF151515), Icons.Outlined.LocalFireDepartment),
    BuiltInAvatar("avatar_coffee", "Coffee", Color(0xFF151515), Color(0xFFBAE136), Icons.Outlined.Coffee),
    BuiltInAvatar("avatar_heart", "Heart", Color(0xFFFFB92E), Color(0xFF6B4EFF), Icons.Outlined.Favorite),
    BuiltInAvatar("avatar_music", "Music", Color(0xFF6B4EFF), Color(0xFFBAE136), Icons.Outlined.MusicNote),
    BuiltInAvatar("avatar_target", "Target", Color(0xFFBAE136), Color(0xFF6B4EFF), Icons.Outlined.TrackChanges),
    BuiltInAvatar("avatar_crown", "Crown", Color(0xFF151515), Color(0xFFFFB92E), Icons.Outlined.EmojiEvents)
)

fun getBuiltInAvatar(id: String): BuiltInAvatar? {
    return BUILT_IN_AVATARS.firstOrNull { it.id == id }
}

@Composable
fun TikoAvatar(
    name: String,
    avatarId: String?,
    googlePhotoUrl: String?,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(clickableModifier)

    val cleanAvatarId = avatarId?.trim() ?: ""
    val cleanPhotoUrl = googlePhotoUrl?.trim() ?: ""

    when {
        cleanAvatarId == "default" -> {
            UserInitialsAvatar(
                name = name,
                size = size,
                backgroundColor = Color(0xFFFFB92E),
                textColor = Color(0xFF151515),
                modifier = baseModifier
            )
        }
        cleanAvatarId == "google" || (cleanAvatarId.isEmpty() && cleanPhotoUrl.isNotEmpty()) -> {
            if (cleanPhotoUrl.isNotEmpty()) {
                Box(
                    modifier = baseModifier,
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(cleanPhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "User profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            UserInitialsAvatar(
                                name = name,
                                size = size,
                                backgroundColor = Color(0xFFFFB92E),
                                textColor = Color(0xFF151515),
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        error = {
                            UserInitialsAvatar(
                                name = name,
                                size = size,
                                backgroundColor = Color(0xFFFFB92E),
                                textColor = Color(0xFF151515),
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        success = {
                            SubcomposeAsyncImageContent(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )
                }
            } else {
                UserInitialsAvatar(
                    name = name,
                    size = size,
                    backgroundColor = Color(0xFFFFB92E),
                    textColor = Color(0xFF151515),
                    modifier = baseModifier
                )
            }
        }
        else -> {
            val builtIn = getBuiltInAvatar(cleanAvatarId)
            if (builtIn != null) {
                Box(
                    modifier = baseModifier.background(builtIn.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = builtIn.icon,
                        contentDescription = builtIn.name,
                        tint = builtIn.tint,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            } else {
                UserInitialsAvatar(
                    name = name,
                    size = size,
                    backgroundColor = Color(0xFFFFB92E),
                    textColor = Color(0xFF151515),
                    modifier = baseModifier
                )
            }
        }
    }
}
