package com.plcoding.cardiganapp.feature.transfer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun transferDestinationProperties(): DialogProperties =
    DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
        scrimColor = Color.Transparent,
    )

@Composable
internal actual fun TransferDestinationNoDimEffect() = Unit
