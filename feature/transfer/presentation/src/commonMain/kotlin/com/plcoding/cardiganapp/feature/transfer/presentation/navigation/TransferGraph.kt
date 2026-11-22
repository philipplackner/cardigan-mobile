package com.plcoding.cardiganapp.feature.transfer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.feature.transfer.presentation.exportboard.ExportBoardRoot
import com.plcoding.cardiganapp.feature.transfer.presentation.importboard.ImportBoardRoot
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.FilePicker
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.rememberFilePicker

/**
 * Registers [ImportBoardRoute] and [ExportBoardRoute] as dialog destinations. [onImported] runs
 * after a successful import, [onBoardListChanged] when an import failed and the list may have
 * changed, and [onDone] when the flow ends and the route should pop. [filePicker] is replaced in tests.
 */
fun NavGraphBuilder.transferGraph(
    onImported: (Board) -> Unit,
    onBoardListChanged: () -> Unit,
    onDone: () -> Unit,
    filePicker: @Composable (onPicked: (PickedFile) -> Unit, onCancelled: () -> Unit) -> FilePicker = { onPicked, onCancelled ->
        rememberFilePicker(onPicked, onCancelled)
    },
) {
    dialog<ImportBoardRoute>(dialogProperties = transferDestinationProperties()) {
        TransferDestinationNoDimEffect()
        ImportBoardRoot(
            onImported = onImported,
            onBoardListChanged = onBoardListChanged,
            onDone = onDone,
            filePicker = filePicker,
        )
    }
    dialog<ExportBoardRoute>(dialogProperties = transferDestinationProperties()) { entry ->
        TransferDestinationNoDimEffect()
        val route = entry.toRoute<ExportBoardRoute>()
        ExportBoardRoot(boardId = route.boardId, boardName = route.boardName, onDone = onDone)
    }
}
