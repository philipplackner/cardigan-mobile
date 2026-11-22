package com.plcoding.cardiganapp.feature.boards.presentation.deleteboard

/** [boardName] is null until the board is found; the dialog draws nothing until then. */
data class DeleteBoardState(
    val boardName: String? = null,
    val cardCount: Int = 0,
    val isDeleting: Boolean = false,
)

sealed interface DeleteBoardAction {
    data object Delete : DeleteBoardAction
    data object Cancel : DeleteBoardAction
}

sealed interface DeleteBoardEvent {
    data object Deleted : DeleteBoardEvent
    data object Close : DeleteBoardEvent
}
