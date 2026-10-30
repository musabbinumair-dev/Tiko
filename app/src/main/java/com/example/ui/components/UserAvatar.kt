package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.OutfitFontFamily
import com.google.firebase.auth.FirebaseAuth

private val AvatarYellow = Color(0xFFFFB92E)
private val AvatarBlack = Color(0xFF151515)

/**
 * Reusable user avatar composable.
 * - For Google users: loads their photo (FirebaseAuth.currentUser.photoUrl) via Coil.
 *   Upgrades '=s96-c' URLs to '=s400-c' for crisp high-resolution rendering.
 *   While loading or on failure, falls back to the yellow circle with first initial.
 * - For Email/Password users: displays the yellow circle with their first initial.
 * - Handles null/blank names or photos safely without crashing.
 */
@Composable
fun UserAvatar(
    size: Dp = 52.dp,
    modifier: Modifier = Modifier,
    name: String? = null,
    photoUrl: String? = null,
    fontSize: TextUnit = (size.value * 0.40f).sp
) {
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
            modifier = modifier
        )
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(highResPhotoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "User Avatar",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
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
        modifier = modifier
            .size(size)
            .clip(CircleShape)
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

