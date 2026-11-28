package com.plcoding.cardiganapp.feature.transfer.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ImportBoardRoute

/** [boardName] is shown in the Exporting dialog and the error toasts without loading the board again. */
@Serializable
data class ExportBoardRoute(
    val boardId: String,
    val boardName: String,
)
