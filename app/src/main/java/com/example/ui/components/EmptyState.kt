package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OutfitFontFamily

/**
 * Reusable EmptyState component matching exact design specs:
 * - Centered column, 24dp side padding
 * - Image 220dp wide, ContentScale.Fit
 * - Title: Outfit ExtraBold 24sp, #151515, centered, 16dp below
 * - Caption: Outfit Regular 16sp, #5B5F66, centered, max 2 lines, 8dp below
 * - Optional black pill button (52dp high, Outfit Bold 17sp, white text), 24dp below
 * - Fade + slide-up entrance, about 300ms
 */
@Composable
fun EmptyState(
    @DrawableRes image: Int,
    title: String,
    caption: String,
    buttonText: String? = null,
    onButton: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    onSecondaryButton: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(durationMillis = 300)
                ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = image),
                contentDescription = title,
                modifier = Modifier
                    .width(220.dp)
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = caption,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = if (isDark) Color(0xFFA8A39A) else Color(0xFF5B5F66),
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            if (buttonText != null && onButton != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFFF5F1E6) else Color(0xFF151515))
                        .clickable(onClick = onButton)
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isDark) Color(0xFF151515) else Color.White
                    )
                }
            }

            if (secondaryButtonText != null && onSecondaryButton != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSecondaryButton)
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = secondaryButtonText,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = if (isDark) Color(0xFFA8A39A) else Color(0xFF5B5F66)
                    )
                }
            }
        }
    }
}
