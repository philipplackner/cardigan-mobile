package com.plcoding.cardiganapp.core.designsystem.components.overlays

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

/** Dialog properties without the platform default width, with the Figma scrim where the platform allows it. */
internal expect fun cardiganDialogProperties(dismissible: Boolean, scrimColor: Color): DialogProperties

/** Applies [scrimColor] to the dialog window on platforms where properties can't carry it. Call inside the dialog. */
@Composable
internal expect fun DialogScrimEffect(scrimColor: Color)
