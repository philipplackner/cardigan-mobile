package com.plcoding.cardiganapp.feature.transfer.presentation.importboard

import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile

sealed interface ImportBoardAction {
    data object DropZoneClick : ImportBoardAction
    class FilePicked(val file: PickedFile) : ImportBoardAction
    data object PickerCancelled : ImportBoardAction
    data class KeyChange(val key: String) : ImportBoardAction
    data object ImportWithKey : ImportBoardAction
}
