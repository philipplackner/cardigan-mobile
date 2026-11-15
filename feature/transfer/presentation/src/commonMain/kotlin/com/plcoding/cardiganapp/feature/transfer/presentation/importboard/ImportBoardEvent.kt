package com.plcoding.cardiganapp.feature.transfer.presentation.importboard

import com.plcoding.cardiganapp.core.domain.board.Board

sealed interface ImportBoardEvent {
    data object OpenPicker : ImportBoardEvent
    data class Imported(val board: Board) : ImportBoardEvent
    data object BoardListChanged : ImportBoardEvent
    data object Finished : ImportBoardEvent
}
