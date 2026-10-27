package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OutfitFontFamily

private val LoginYellow = Color(0xFFFFCF4D)
private val LoginCream = Color(0xFFEFEBDD)
private val LoginBlack = Color(0xFF151515)
private val LoginGrayPlaceholder = Color(0xFF8E8E93)
private val LoginDividerLine = Color(0xFFCCCCCC)
private val LoginDividerText = Color(0xFF6B6B6B)

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        0.0f to LoginYellow,
        0.35f to LoginCream,
        1.0f to LoginCream
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // 1. Back button: 48dp white circle, 1dp #151515 outline, back arrow icon
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, LoginBlack, CircleShape)
                    .clickable(onClick = onBack)
                    .testTag("login_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LoginBlack,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Heading "WELCOME BACK" on 2 lines, Outfit Black 48sp, uppercase, black, letter spacing -1sp
            Text(
                text = "WELCOME\nBACK",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 48.sp,
                lineHeight = 48.sp,
                letterSpacing = (-1).sp,
                color = LoginBlack
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Caption "Log in to continue" Outfit Regular 18sp below it
            Text(
                text = "Log in to continue",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = LoginBlack
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Email input: white fill, fully rounded (56dp high), no outline, mail icon 24dp at left, gray placeholder "Email" Outfit Regular 17sp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = LoginBlack,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (email.isEmpty()) {
                            Text(
                                text = "Email",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 17.sp,
                                color = LoginGrayPlaceholder
                            )
                        }
                        BasicTextField(
                            value = email,
                            onValueChange = { email = it },
                            textStyle = TextStyle(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 17.sp,
                                color = LoginBlack
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(LoginBlack),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Password input: same style, lock icon at left, eye icon at right toggling show/hide, placeholder "Password", 12dp below
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = LoginBlack,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (password.isEmpty()) {
                            Text(
                                text = "Password",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 17.sp,
                                color = LoginGrayPlaceholder
                            )
                        }
                        BasicTextField(
                            value = password,
                            onValueChange = { password = it },
                            textStyle = TextStyle(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 17.sp,
                                color = LoginBlack
                            ),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            cursorBrush = SolidColor(LoginBlack),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = LoginBlack,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { passwordVisible = !passwordVisible }
                            .testTag("login_password_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Right-aligned link "Forgot password?" Outfit Medium 15sp, black, 10dp below
            Text(
                text = "Forgot password?",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = LoginBlack,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { /* action */ }
                    .testTag("login_forgot_password")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Full-width black pill button "Log in", 56dp high, Outfit Bold 18sp, white text, 24dp below
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(LoginBlack)
                    .clickable(onClick = { onLoginSuccess(email.ifBlank { "user@example.com" }) })
                    .testTag("login_submit_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log in",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            // 7. Divider: thin gray lines with "or" centered, Outfit Regular 15sp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(LoginDividerLine)
                )
                Text(
                    text = "or",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = LoginDividerText,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(LoginDividerLine)
                )
            }

            // 8. Two full-width pills, 56dp high, white fill, 1dp #151515 outline, centered icon + text Outfit Bold 17sp:
            // "Continue with Google" (multicolor G icon)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, LoginBlack, CircleShape)
                    .clickable(onClick = { onLoginSuccess("google.user@gmail.com") })
                    .testTag("continue_with_google"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GoogleIcon(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = LoginBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "Continue with Apple" (black Apple icon)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, LoginBlack, CircleShape)
                    .clickable(onClick = { onLoginSuccess("apple.user@icloud.com") })
                    .testTag("continue_with_apple"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppleIcon(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Apple",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = LoginBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // 9. Bottom text centered: "New here? " Outfit Regular 16sp + "Sign up" Outfit Bold 16sp, 32dp bottom padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New here? ",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = LoginBlack
                )
                Text(
                    text = "Sign up",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = LoginBlack,
                    modifier = Modifier
                        .clickable(onClick = onSignUp)
                        .testTag("login_signup_link")
                )
            }
        }
    }
}

@Composable
private fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 24f
        val pathBlue = PathParser().parsePathString("M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z").toPath()
        val pathGreen = PathParser().parsePathString("M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z").toPath()
        val pathYellow = PathParser().parsePathString("M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z").toPath()
        val pathRed = PathParser().parsePathString("M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z").toPath()

        scale(scale, pivot = Offset.Zero) {
            drawPath(pathBlue, Color(0xFF4285F4))
            drawPath(pathGreen, Color(0xFF34A853))
            drawPath(pathYellow, Color(0xFFFBBC05))
            drawPath(pathRed, Color(0xFFEA4335))
        }
    }
}

@Composable
private fun AppleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 24f
        val applePath = PathParser().parsePathString("M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M15.97 6.37c.63-.77 1.06-1.85.94-2.92-1 .04-2.2.67-2.9 1.49-.6.7-1.13 1.81-.99 2.86 1.12.09 2.32-.66 2.95-1.43z").toPath()

        scale(scale, pivot = Offset.Zero) {
            drawPath(applePath, LoginBlack)
        }
    }
}
