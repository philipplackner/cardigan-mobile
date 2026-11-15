package com.plcoding.cardiganapp.feature.transfer.presentation.importboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plcoding.cardiganapp.core.domain.board.Board
import com.plcoding.cardiganapp.core.presentation.util.ObserveAsEvents
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.FilePicker
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.rememberFilePicker
import org.koin.compose.viewmodel.koinViewModel

/**
 * Import flow. Opens the system file picker as soon as it is shown and draws a dialog only for
 * errors, progress and a key clash. [filePicker] is replaced in tests.
 */
@Composable
fun ImportBoardRoot(
    onImported: (Board) -> Unit,
    onBoardListChanged: () -> Unit,
    onDone: () -> Unit,
    viewModel: ImportBoardViewModel = koinViewModel(),
    filePicker: @Composable (onPicked: (PickedFile) -> Unit, onCancelled: () -> Unit) -> FilePicker = { onPicked, onCancelled ->
        rememberFilePicker(onPicked, onCancelled)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = filePicker(
        { viewModel.onAction(ImportBoardAction.FilePicked(it)) },
        { viewModel.onAction(ImportBoardAction.PickerCancelled) },
    )
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ImportBoardEvent.OpenPicker -> picker.pickJson()
            is ImportBoardEvent.Imported -> {
                onImported(event.board)
                onDone()
            }
            ImportBoardEvent.BoardListChanged -> onBoardListChanged()
            ImportBoardEvent.Finished -> onDone()
        }
    }
    ImportBoardScreen(state = state, onAction = viewModel::onAction, onCancel = onDone)
}
