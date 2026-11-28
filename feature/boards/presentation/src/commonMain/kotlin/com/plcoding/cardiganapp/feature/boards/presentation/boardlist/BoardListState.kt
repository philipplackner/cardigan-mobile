package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

data class BoardListState(
    val content: BoardListContent = BoardListContent.Loading,
    val isRefreshing: Boolean = false,
    val boardCount: Int = 0,
    val accountEmail: String? = null,
    val isMenuOpen: Boolean = false,
)

sealed interface BoardListContent {
    data object Loading : BoardListContent

    data object Error : BoardListContent

    /** An empty [boards] list is the "No boards yet" state. */
    data class Loaded(
        val boards: List<BoardItemUi>,
    ) : BoardListContent
}
