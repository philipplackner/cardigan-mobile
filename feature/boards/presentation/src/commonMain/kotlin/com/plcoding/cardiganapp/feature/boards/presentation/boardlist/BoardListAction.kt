package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

sealed interface BoardListAction {
    data class OnBoardClick(val boardId: String) : BoardListAction
    data class OnRenameClick(val boardId: String) : BoardListAction
    data class OnDeleteClick(val boardId: String) : BoardListAction
    data object OnNewBoardClick : BoardListAction
    data object OnImportClick : BoardListAction
    data object OnLabelsClick : BoardListAction
    data object OnLogOutClick : BoardListAction
    data object OnMenuClick : BoardListAction
    data object OnMenuDismiss : BoardListAction
    data object OnRefresh : BoardListAction
    data object OnRetry : BoardListAction
}
