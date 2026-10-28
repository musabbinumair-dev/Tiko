package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musab.tiko.R
import com.example.ui.components.ErrorPageLayout

const val ROUTE_ERROR_NOT_FOUND = "error_not_found"

@Composable
fun NotFoundScreen(
    onBackToHome: () -> Unit,
    onGoBack: () -> Unit,
    onClose: () -> Unit = onBackToHome,
    modifier: Modifier = Modifier
) {
    ErrorPageLayout(
        image = R.drawable.illus_error_not_found,
        imageWidth = 360.dp,
        title = "Page not found",
        caption = "We can't find what you're looking for. It may have been deleted or moved.",
        primaryText = "Back to home",
        onPrimary = onBackToHome,
        linkText = "Go back",
        onLink = onGoBack,
        onClose = onClose,
        onBack = onGoBack,
        modifier = modifier
    )
}
