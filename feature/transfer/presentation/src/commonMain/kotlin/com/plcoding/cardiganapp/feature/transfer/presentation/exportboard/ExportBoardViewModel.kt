package com.plcoding.cardiganapp.feature.transfer.presentation.exportboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.export_error_failed
import cardiganapp.feature.transfer.presentation.generated.resources.export_error_not_found
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportError
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Exports the board as soon as it is created. The progress dialog is its whole UI, so it has no state. */
class ExportBoardViewModel(
    boardId: String,
    val boardName: String,
    private val repository: BoardTransferRepository,
    private val toastController: ToastController,
) : ViewModel() {

    private val _events = Channel<ExportBoardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val id = Uuid.parseOrNull(boardId)
            val result = if (id != null) repository.exportBoard(id) else Result.Error(ExportError.Failed)
            when (result) {
                is Result.Success -> _events.send(ExportBoardEvent.Share(result.data))
                is Result.Error -> {
                    val message = when (result.error) {
                        ExportError.BoardNotFound -> Res.string.export_error_not_found
                        ExportError.Failed -> Res.string.export_error_failed
                    }
                    toastController.show(ToastMessage(UiText.Resource(message, arrayOf(boardName)), ToastType.Error))
                    _events.send(ExportBoardEvent.Finished)
                }
            }
        }
    }
}
