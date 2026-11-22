package com.plcoding.cardiganapp.feature.boards.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/**
 * Properties of the dialog destination window that hosts the board dialogs. It can't be dismissed
 * and draws no scrim: back, Escape, scrim taps and the scrim all belong to the design-system
 * dialog drawn inside it. Same setup as the transfer dialogs.
 */
internal expect fun boardDialogDestinationProperties(): DialogProperties

/** Removes the window dim where [DialogProperties] can't. Call inside the destination. */
@Composable
internal expect fun BoardDialogDestinationNoDimEffect()
