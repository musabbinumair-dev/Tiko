package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.musab.tiko.R
import com.example.ui.components.ArrowOutwardIcon
import com.example.ui.theme.OutfitFontFamily

private val WelcomeCream = Color(0xFFEFEBDD)
private val WelcomeBlack = Color(0xFF151515)
private val CardPurple = Color(0xFF6B4EFF)
private val CardYellow = Color(0xFFFFB92E)
private val CardLime = Color(0xFFC8DB6A)

// Dark theme colors matching dark mockup
private val WelcomeDarkBg = Color(0xFF121212)
private val WelcomeDarkCream = Color(0xFFF0EADC)
private val WelcomeDarkSkipBg = Color(0xFF242424)
private val WelcomeDarkCardPillBg = Color(0xFF1E1E1E)
private val WelcomeDarkCaption = Color(0xFFD4CFBF)

@Composable
fun WelcomeScreen(
    onNavigateHome: () -> Unit,
    onNavigateLogin: () -> Unit = onNavigateHome,
    onNavigateSignUp: () -> Unit = onNavigateHome,
    isDark: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    DisposableEffect(isDark) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val original = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = !isDark
            onDispose {
                controller.isAppearanceLightStatusBars = original
            }
        } else {
            onDispose {}
        }
    }

    val screenBg = if (isDark) WelcomeDarkBg else WelcomeCream
    val logoColor = if (isDark) WelcomeDarkCream else WelcomeBlack
    val headlineColor = if (isDark) WelcomeDarkCream else WelcomeBlack
    val skipBg = if (isDark) WelcomeDarkSkipBg else Color.White
    val skipTextColor = if (isDark) WelcomeDarkCream else WelcomeBlack
    val captionColor = if (isDark) WelcomeDarkCaption else WelcomeBlack

    val getStartedBg = if (isDark) WelcomeDarkCream else WelcomeBlack
    val getStartedTextColor = if (isDark) WelcomeBlack else Color.White

    val secondaryBg = if (isDark) Color.Transparent else WelcomeCream
    val secondaryBorder = if (isDark) WelcomeDarkCream else WelcomeBlack
    val secondaryTextColor = if (isDark) WelcomeDarkCream else WelcomeBlack

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Top row: "Tiko" Outfit Black 40sp; at right pill "Skip" (40dp high, Outfit Bold 16sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tiko",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 40.sp,
                        letterSpacing = (-1).sp,
                        color = logoColor
                    )

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(skipBg)
                            .clickable(onClick = onNavigateHome)
                            .padding(horizontal = 20.dp)
                            .testTag("welcome_skip_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = skipTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Heading "GET THINGS DONE" on 3 lines, Outfit Black 72sp, uppercase, line height 0.95, letter spacing -2sp
                Text(
                    text = "GET\nTHINGS\nDONE",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 72.sp,
                    lineHeight = 68.4.sp,
                    letterSpacing = (-2).sp,
                    color = headlineColor
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3. 3 stacked overlapping cards, 28dp radius, each overlapping the previous by 18dp, rotated slightly:
                // purple #6B4EFF rotate 0.5deg, yellow #FFB92E rotate -1.5deg, lime #C8DB6A rotate 1deg
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy((-18).dp)
                ) {
                    // Card 1: Purple, calendar icon, tag "Work", "Plan your day"
                    WelcomeCard(
                        backgroundColor = CardPurple,
                        rotation = 0.5f,
                        icon = Icons.Outlined.CalendarToday,
                        tag = "Work",
                        title = "Plan your day",
                        titleColor = Color.White,
                        isDark = isDark
                    )

                    // Card 2: Yellow, flame icon, tag "Health", "Track your streak"
                    WelcomeCard(
                        backgroundColor = CardYellow,
                        rotation = -1.5f,
                        icon = Icons.Outlined.LocalFireDepartment,
                        tag = "Health",
                        title = "Track your streak",
                        titleColor = WelcomeBlack,
                        isDark = isDark
                    )

                    // Card 3: Lime, clock icon, tag "Personal", "Stay on time"
                    WelcomeCard(
                        backgroundColor = CardLime,
                        rotation = 1f,
                        icon = Icons.Outlined.Schedule,
                        tag = "Personal",
                        title = "Stay on time",
                        titleColor = WelcomeBlack,
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Caption "Simple tasks. Big results." Outfit Regular 18sp, 24dp below the cards
                Text(
                    text = "Simple tasks. Big results.",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = captionColor
                )
            }

            // 5. Bottom buttons: "Get started" and "I already have an account"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
            ) {
                // "Get started" 56dp high, Outfit Bold 18sp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(getStartedBg)
                        .clickable(onClick = onNavigateSignUp)
                        .testTag("welcome_get_started_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Get started",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = getStartedTextColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // "I already have an account" 56dp high, Outfit Bold 17sp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(secondaryBg)
                        .border(1.dp, secondaryBorder, CircleShape)
                        .clickable(onClick = onNavigateLogin)
                        .testTag("welcome_login_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "I already have an account",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = secondaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(
    backgroundColor: Color,
    rotation: Float,
    icon: ImageVector,
    tag: String,
    title: String,
    titleColor: Color,
    isDark: Boolean = false
) {
    val pillBg = if (isDark) WelcomeDarkCardPillBg else Color.White
    val pillFg = if (isDark) WelcomeDarkCream else WelcomeBlack
    val arrowColor = if (isDark) WelcomeDarkCream else WelcomeBlack

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(rotation)
            .height(110.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Pill tag + 44dp circle with arrow_outward
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill tag (32dp high, icon 18dp + Outfit Medium 14sp)
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(pillBg)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = pillFg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tag,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = pillFg
                    )
                }

                // 44dp circle with arrow_outward icon at top right
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(pillBg),
                    contentAlignment = Alignment.Center
                ) {
                    ArrowOutwardIcon(size = 20.dp, color = arrowColor)
                }
            }

            // Title Outfit Bold 28sp
            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = titleColor
            )
        }
    }
}
