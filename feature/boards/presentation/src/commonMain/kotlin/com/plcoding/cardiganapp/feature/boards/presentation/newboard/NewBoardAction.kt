package com.plcoding.cardiganapp.feature.boards.presentation.newboard

sealed interface NewBoardAction {
    data class NameChange(val name: String) : NewBoardAction
    data class KeyChange(val key: String) : NewBoardAction
    data object Create : NewBoardAction
    data object Cancel : NewBoardAction
}
