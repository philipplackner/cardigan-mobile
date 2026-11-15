package com.plcoding.cardiganapp.core.designsystem.components.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

internal actual fun cardiganDialogProperties(dismissible: Boolean, scrimColor: Color): DialogProperties =
    DialogProperties(
        dismissOnBackPress = dismissible,
        dismissOnClickOutside = dismissible,
        usePlatformDefaultWidth = false,
    )

// Android dims dialog windows with black only, so the scrim alpha becomes the dim amount.
@Composable
internal actual fun DialogScrimEffect(scrimColor: Color) {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.setDimAmount(scrimColor.alpha)
    }
}
