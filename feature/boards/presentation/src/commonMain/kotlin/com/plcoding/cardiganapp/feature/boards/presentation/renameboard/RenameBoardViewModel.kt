package com.plcoding.cardiganapp.feature.boards.presentation.renameboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_no_longer_exists
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_failed
import com.plcoding.cardiganapp.core.domain.text.normalizeText
import com.plcoding.cardiganapp.core.domain.text.validateName
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.presentation.components.BOARD_NAME_MAX_LENGTH
import com.plcoding.cardiganapp.feature.boards.presentation.components.findBoard
import com.plcoding.cardiganapp.feature.boards.presentation.components.toNameError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/** Rename board dialog. An unchanged normalized name closes it without a request (D15). */
class RenameBoardViewModel(
    private val boardId: String,
    private val repository: BoardRepository,
    private val toastController: ToastController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(RenameBoardState())
    val state: StateFlow<RenameBoardState> = _state.asStateFlow()

    private val _events = Channel<RenameBoardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val board = repository.findBoard(boardId)
            if (board == null) {
                _events.send(RenameBoardEvent.Close)
                return@launch
            }
            val submitted = savedStateHandle[KEY_SUBMITTED] ?: false
            val restored =
                RenameBoardState(
                    boardName = board.name,
                    key = board.key,
                    name = savedStateHandle[KEY_NAME] ?: board.name,
                    submitted = submitted,
                )
            _state.value = if (submitted) restored.validated() else restored
        }
    }

    fun onAction(action: RenameBoardAction) {
        val current = _state.value
        if (current.isSaving || current.boardName == null) return
        when (action) {
            is RenameBoardAction.NameChange -> {
                _state.update {
                    it.copy(name = action.name).let { changed ->
                        if (changed.submitted) changed.validated() else changed
                    }
                }
                savedStateHandle[KEY_NAME] = action.name
            }

            RenameBoardAction.Save -> {
                save(current.boardName)
            }

            RenameBoardAction.Cancel -> {
                _events.trySend(RenameBoardEvent.Close)
            }
        }
    }

    private fun save(boardName: String) {
        val validated = _state.value.copy(submitted = true).validated()
        _state.value = validated
        savedStateHandle[KEY_SUBMITTED] = true
        if (validated.nameError != null) return

        val name = normalizeText(validated.name)
        if (name == boardName) {
            _events.trySend(RenameBoardEvent.Close)
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = repository.renameBoard(Uuid.parse(boardId), name)) {
                is Result.Success -> {
                    _events.send(RenameBoardEvent.Renamed(result.data.name))
                }

                is Result.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    when (val error = result.error) {
                        BoardSaveError.NotFound -> {
                            toast(UiText.Resource(Res.string.board_no_longer_exists, arrayOf(boardName)))
                            _events.send(RenameBoardEvent.Close)
                        }

                        is BoardSaveError.InvalidFields -> {
                            _state.update { it.copy(nameError = error.name?.toNameError()) }
                        }

                        is BoardSaveError.Network -> {
                            toast(UiText.Resource(Res.string.rename_board_failed, arrayOf(boardName)))
                        }
                    }
                }
            }
        }
    }

    private fun toast(text: UiText) = toastController.show(ToastMessage(text, ToastType.Error))

    private fun RenameBoardState.validated(): RenameBoardState =
        copy(nameError = validateName(normalizeText(name), BOARD_NAME_MAX_LENGTH))

    private companion object {
        const val KEY_NAME = "rename_board_name"
        const val KEY_SUBMITTED = "rename_board_submitted"
    }
}
