package com.plcoding.cardiganapp.feature.transfer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/**
 * Properties of the dialog destination window that hosts the transfer dialogs. It can't be
 * dismissed and draws no scrim: back, Escape, scrim taps and the scrim all belong to the
 * design-system dialog drawn inside it.
 */
internal expect fun transferDestinationProperties(): DialogProperties

/** Removes the window dim where [DialogProperties] can't. Call inside the destination. */
@Composable
internal expect fun TransferDestinationNoDimEffect()
