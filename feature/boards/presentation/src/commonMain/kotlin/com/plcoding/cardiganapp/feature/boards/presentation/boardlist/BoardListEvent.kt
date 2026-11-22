package com.plcoding.cardiganapp.feature.boards.presentation.boardlist

sealed interface BoardListEvent {
    data class OpenBoard(val boardId: String, val name: String, val key: String) : BoardListEvent
}
