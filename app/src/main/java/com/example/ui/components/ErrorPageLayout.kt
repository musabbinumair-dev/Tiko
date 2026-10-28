package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OutfitFontFamily

@Composable
fun ErrorPageLayout(
    @DrawableRes image: Int,
    title: String,
    caption: String,
    primaryText: String,
    onPrimary: () -> Unit,
    linkText: String,
    onLink: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    imageWidth: Dp = 340.dp,
    onBack: () -> Unit = onLink
) {
    BackHandler {
        onBack()
    }

    val animStarted = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animStarted.value = true
    }

    val density = LocalDensity.current
    val slideOffsetPx = remember(density) { with(density) { 20.dp.roundToPx() } }

    val creamBackground = Color(0xFFEFEBDD)
    val textBlack = Color(0xFF151515)
    val captionGray = Color(0xFF5B5F66)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(creamBackground)
    ) {
        // Top section: Close button, Illustration, Heading, Caption
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button: 44dp white circle with a black X icon, top left, 20dp from the side
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onClose)
                        .testTag("error_page_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = textBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Illustration: centered, imageWidth, ContentScale.Fit, 28dp below close button
            // Entrance: fades and slides up 20dp over 350ms
            AnimatedVisibility(
                visible = animStarted.value,
                enter = fadeIn(animationSpec = tween(durationMillis = 350)) +
                        slideInVertically(
                            initialOffsetY = { slideOffsetPx },
                            animationSpec = tween(durationMillis = 350)
                        )
            ) {
                Image(
                    painter = painterResource(id = image),
                    contentDescription = title,
                    modifier = Modifier
                        .width(imageWidth)
                        .wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Heading & Caption: entrance fades in after the illustration
            AnimatedVisibility(
                visible = animStarted.value,
                enter = fadeIn(animationSpec = tween(durationMillis = 350, delayMillis = 200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = title,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 44.sp,
                        lineHeight = 46.sp,
                        letterSpacing = (-1).sp,
                        color = textBlack,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = caption,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = captionGray,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        // Bottom pinned actions: Primary button + text link
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(textBlack)
                    .clickable(onClick = onPrimary)
                    .testTag("error_page_primary_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = primaryText,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = linkText,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = textBlack,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable(onClick = onLink)
                    .testTag("error_page_link_button")
            )
        }
    }
}
