package com.plcoding.cardiganapp.feature.boards.presentation.newboard

sealed interface NewBoardEvent {
    data class Created(val boardId: String, val name: String, val key: String) : NewBoardEvent
    data object Close : NewBoardEvent
}
