package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.ErrorPageLayout

const val ROUTE_ERROR_SAVE_FAILED = "error_save_failed"

@Composable
fun SaveErrorScreen(
    onTryAgain: () -> Unit,
    onClose: () -> Unit,
    onGoHome: () -> Unit,
    onBack: () -> Unit = onTryAgain,
    modifier: Modifier = Modifier
) {
    ErrorPageLayout(
        image = R.drawable.illus_error_save_failed,
        imageWidth = 340.dp,
        title = "We couldn't\nsave your task",
        caption = "Something went wrong while saving. Please try again.",
        primaryText = "Try again",
        onPrimary = onTryAgain,
        linkText = "Go back home",
        onLink = onGoHome,
        onClose = onClose,
        onBack = onBack,
        modifier = modifier
    )
}
