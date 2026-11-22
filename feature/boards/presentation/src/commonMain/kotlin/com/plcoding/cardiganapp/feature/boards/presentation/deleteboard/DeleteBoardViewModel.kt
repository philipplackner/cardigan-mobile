package com.plcoding.cardiganapp.feature.boards.presentation.deleteboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.delete_board_failed
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.presentation.components.findBoard
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Delete board confirmation. A board that is already gone on the server counts as deleted. */
class DeleteBoardViewModel(
    private val boardId: String,
    private val repository: BoardRepository,
    private val toastController: ToastController,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteBoardState())
    val state: StateFlow<DeleteBoardState> = _state.asStateFlow()

    private val _events = Channel<DeleteBoardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val board = repository.findBoard(boardId)
            if (board == null) {
                _events.send(DeleteBoardEvent.Close)
            } else {
                _state.value = DeleteBoardState(boardName = board.name, cardCount = board.cardCount)
            }
        }
    }

    fun onAction(action: DeleteBoardAction) {
        val current = _state.value
        if (current.isDeleting || current.boardName == null) return
        when (action) {
            DeleteBoardAction.Delete -> delete(current.boardName)
            DeleteBoardAction.Cancel -> _events.trySend(DeleteBoardEvent.Close)
        }
    }

    private fun delete(boardName: String) {
        _state.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            when (repository.deleteBoard(Uuid.parse(boardId))) {
                is Result.Success -> _events.send(DeleteBoardEvent.Deleted)
                is Result.Error -> {
                    _state.update { it.copy(isDeleting = false) }
                    toastController.show(
                        ToastMessage(UiText.Resource(Res.string.delete_board_failed, arrayOf(boardName)), ToastType.Error),
                    )
                }
            }
        }
    }
}
