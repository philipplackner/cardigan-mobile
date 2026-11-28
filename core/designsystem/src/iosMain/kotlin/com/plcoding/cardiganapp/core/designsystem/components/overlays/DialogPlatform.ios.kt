package com.plcoding.cardiganapp.core.designsystem.components.overlays

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun cardiganDialogProperties(
    dismissible: Boolean,
    scrimColor: Color,
): DialogProperties =
    DialogProperties(
        dismissOnBackPress = dismissible,
        dismissOnClickOutside = dismissible,
        usePlatformDefaultWidth = false,
        scrimColor = scrimColor,
    )

@Composable
internal actual fun DialogScrimEffect(scrimColor: Color) = Unit
