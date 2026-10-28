package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.OutfitFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarPickerBottomSheet(
    visible: Boolean,
    currentAvatarId: String,
    googlePhotoUrl: String,
    userName: String,
    onSelectAvatar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    BackHandler { onDismiss() }

    val isGoogleUser = googlePhotoUrl.isNotBlank()
    val isDefaultSelected = currentAvatarId == "default" || (currentAvatarId.isBlank() && !isGoogleUser)
    val isGoogleSelected = currentAvatarId == "google" || (currentAvatarId.isBlank() && isGoogleUser)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1500f)
            .background(Color.Black.copy(alpha = 0.50f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* intercept clicks */ }
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 44.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0DDD2))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Choose Avatar",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = AppBlack
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pick a style or use your profile photo",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Color(0xFF6B6B6B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF2EFE6))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss
                                )
                                .testTag("close_avatar_picker"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = AppBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Account Options (Use Default, Use Google Photo)
                    Text(
                        text = "Standard Options",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // "Use default" Option
                        OptionPillCard(
                            title = "Default initial",
                            isSelected = isDefaultSelected,
                            modifier = Modifier.weight(1f),
                            testTag = "avatar_option_default",
                            avatarContent = {
                                UserInitialsAvatar(
                                    name = userName,
                                    size = 36.dp,
                                    backgroundColor = Color(0xFFFFB92E),
                                    textColor = AppBlack
                                )
                            },
                            onClick = {
                                onSelectAvatar("default")
                                onDismiss()
                            }
                        )

                        // "Use Google photo" Option (only for Google users)
                        if (isGoogleUser) {
                            OptionPillCard(
                                title = "Google photo",
                                isSelected = isGoogleSelected,
                                modifier = Modifier.weight(1f),
                                testTag = "avatar_option_google",
                                avatarContent = {
                                    TikoAvatar(
                                        name = userName,
                                        avatarId = "google",
                                        googlePhotoUrl = googlePhotoUrl,
                                        size = 36.dp
                                    )
                                },
                                onClick = {
                                    onSelectAvatar("google")
                                    onDismiss()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Built-in Avatar Grid Header
                    Text(
                        text = "Built-in Avatars",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 12 Built-in Avatars in a 4-column FlowRow
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        maxItemsInEachRow = 4
                    ) {
                        BUILT_IN_AVATARS.forEach { avatar ->
                            val isSelected = currentAvatarId == avatar.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onSelectAvatar(avatar.id)
                                        onDismiss()
                                    }
                                    .testTag("avatar_item_${avatar.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(avatar.bg)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(3.dp, AppBlack, CircleShape)
                                            } else {
                                                Modifier.border(1.dp, Color(0xFFE8E4D8), CircleShape)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = avatar.icon,
                                        contentDescription = avatar.name,
                                        tint = avatar.tint,
                                        modifier = Modifier.size(30.dp)
                                    )

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(AppBlack),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = avatar.name,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) AppBlack else Color(0xFF6B6B6B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun OptionPillCard(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    avatarContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFFF3EFFD) else Color(0xFFF7F5EE))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) CardPurple else Color(0xFFE2DDD1),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            avatarContent()

            Column {
                Text(
                    text = title,
                    fontFamily = OutfitFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isSelected) CardPurple else AppBlack
                )
                if (isSelected) {
                    Text(
                        text = "Active",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = CardPurple
                    )
                }
            }
        }
    }
}
