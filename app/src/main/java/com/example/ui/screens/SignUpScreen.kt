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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.example.ui.components.AppToastController
import com.example.ui.components.ToastType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ArrowOutwardIcon
import com.example.ui.theme.OutfitFontFamily

private val SignUpYellow = Color(0xFFFFCF4D)
private val SignUpCream = Color(0xFFEFEBDD)
private val SignUpBlack = Color(0xFF151515)
private val CardPurple = Color(0xFF6B4EFF)
private val SignUpGrayPlaceholder = Color(0xFF8E8E93)
private val SignUpDividerLine = Color(0xFFCCCCCC)
private val SignUpDividerText = Color(0xFF6B6B6B)

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onCreateAccountSuccess: (String, String) -> Unit,
    onNavigateLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authRepository = remember(context) { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = fullName.isNotBlank() &&
        email.isNotBlank() &&
        password.isNotBlank() &&
        agreedToTerms &&
        !isLoading

    val backgroundGradient = Brush.verticalGradient(
        0.0f to SignUpYellow,
        0.35f to SignUpCream,
        1.0f to SignUpCream
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // 1. Back button (same as Log in)
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, SignUpBlack, CircleShape)
                    .clickable(onClick = onBack)
                    .testTag("signup_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SignUpBlack,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Heading "CREATE ACCOUNT" on 2 lines, same style as "WELCOME BACK"
            Text(
                text = "CREATE\nACCOUNT",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 48.sp,
                lineHeight = 48.sp,
                letterSpacing = (-1).sp,
                color = SignUpBlack
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Caption "Start organizing your day"
            Text(
                text = "Start organizing your day",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = SignUpBlack
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Purple #6B4EFF card, 28dp radius, 16dp padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardPurple)
                    .padding(16.dp)
            ) {
                Column {
                    // Top row: pill tag "Your details" + 44dp white circle with arrow_outward
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // White pill tag "Your details" (32dp high, person icon, Outfit Medium 14sp)
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = SignUpBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Your details",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = SignUpBlack
                            )
                        }

                        // 44dp white circle with arrow_outward icon at top right
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            ArrowOutwardIcon(size = 20.dp, color = SignUpBlack)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 stacked white inputs with 10dp gaps
                    // Input 1: Full name (person icon)
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
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = SignUpBlack,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (fullName.isEmpty()) {
                                    Text(
                                        text = "Full name",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 17.sp,
                                        color = SignUpGrayPlaceholder
                                    )
                                }
                                BasicTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    textStyle = TextStyle(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 17.sp,
                                        color = SignUpBlack
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(SignUpBlack),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_fullname_input")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input 2: Email (mail icon)
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
                                tint = SignUpBlack,
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
                                        color = SignUpGrayPlaceholder
                                    )
                                }
                                BasicTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    textStyle = TextStyle(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 17.sp,
                                        color = SignUpBlack
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(SignUpBlack),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_email_input")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input 3: Password (lock icon + eye toggle)
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
                                tint = SignUpBlack,
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
                                        color = SignUpGrayPlaceholder
                                    )
                                }
                                BasicTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    textStyle = TextStyle(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 17.sp,
                                        color = SignUpBlack
                                    ),
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    cursorBrush = SolidColor(SignUpBlack),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_password_input")
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = SignUpBlack,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { passwordVisible = !passwordVisible }
                                    .testTag("signup_password_toggle")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Row below card: square checkbox (24dp, 1.5dp black outline, black fill with white check when selected)
            // + text "I agree to the Terms and Privacy Policy"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { agreedToTerms = !agreedToTerms }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (agreedToTerms) SignUpBlack else Color.Transparent)
                        .border(1.5.dp, SignUpBlack, RoundedCornerShape(4.dp))
                        .testTag("signup_terms_checkbox"),
                    contentAlignment = Alignment.Center
                ) {
                    if (agreedToTerms) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Checked",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                val termsText = buildAnnotatedString {
                    append("I agree to the ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Terms")
                    }
                    append(" and ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Privacy Policy")
                    }
                }

                Text(
                    text = termsText,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = SignUpBlack
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Full-width black pill button "Create account", 56dp high
            // (disabled look at 40% opacity until all fields are filled and the checkbox is checked)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(SignUpBlack)
                    .alpha(if (isFormValid) 1.0f else 0.4f)
                    .clickable(enabled = isFormValid) {
                        val trimmedName = fullName.trim()
                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()
                        
                        when {
                            trimmedName.isEmpty() -> {
                                AppToastController.show("Please enter your full name", ToastType.Error)
                            }
                            trimmedEmail.isEmpty() -> {
                                AppToastController.show("Please enter your email address", ToastType.Error)
                            }
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                                AppToastController.show("Please enter a valid email address", ToastType.Error)
                            }
                            trimmedPassword.length < 8 -> {
                                AppToastController.show("Use at least 8 characters.", ToastType.Error)
                            }
                            !agreedToTerms -> {
                                AppToastController.show("Please agree to the Terms and Privacy Policy", ToastType.Warning)
                            }
                            else -> {
                                isLoading = true
                                scope.launch {
                                    when (val result = authRepository.signUp(trimmedName, trimmedEmail, trimmedPassword)) {
                                        is AuthResult.Success -> {
                                            AppToastController.show("Account created successfully!", ToastType.Success)
                                            onCreateAccountSuccess(trimmedName, trimmedEmail)
                                        }
                                        is AuthResult.Error -> {
                                            AppToastController.show(result.message, ToastType.Error)
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .testTag("signup_submit_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Create account",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            // 6. Divider: "or"
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
                        .background(SignUpDividerLine)
                )
                Text(
                    text = "or",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = SignUpDividerText,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(SignUpDividerLine)
                )
            }

            // 7. One outlined pill "Continue with Google"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, SignUpBlack, CircleShape)
                    .clickable(onClick = { 
                        AppToastController.show("Coming soon", ToastType.Info)
                    })
                    .testTag("signup_continue_with_google"),
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
                        color = SignUpBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 8. Bottom text centered: "Already have an account? " Outfit Regular 16sp + "Log in" Outfit Bold 16sp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = SignUpBlack
                )
                Text(
                    text = "Log in",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = SignUpBlack,
                    modifier = Modifier
                        .clickable(onClick = onNavigateLogin)
                        .testTag("signup_login_link")
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
