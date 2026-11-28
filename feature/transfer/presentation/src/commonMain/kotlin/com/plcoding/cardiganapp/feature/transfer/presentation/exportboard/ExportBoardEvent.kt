package com.plcoding.cardiganapp.feature.transfer.presentation.exportboard

import com.plcoding.cardiganapp.feature.transfer.domain.ExportedFile

sealed interface ExportBoardEvent {
    class Share(
        val file: ExportedFile,
    ) : ExportBoardEvent

    data object Finished : ExportBoardEvent
}
