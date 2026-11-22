package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

internal actual fun boardDialogDestinationProperties(): DialogProperties =
    DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
    )

@Composable
internal actual fun BoardDialogDestinationNoDimEffect() {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}
