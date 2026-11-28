package com.plcoding.cardiganapp.feature.boards.presentation.newboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.new_board_failed
import com.plcoding.cardiganapp.core.domain.board.BoardKeyRule
import com.plcoding.cardiganapp.core.domain.text.normalizeText
import com.plcoding.cardiganapp.core.domain.text.validateName
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.boards.domain.BoardKeyPrefill
import com.plcoding.cardiganapp.feature.boards.domain.BoardRepository
import com.plcoding.cardiganapp.feature.boards.domain.BoardSaveError
import com.plcoding.cardiganapp.feature.boards.presentation.components.BOARD_NAME_MAX_LENGTH
import com.plcoding.cardiganapp.feature.boards.presentation.components.BoardKeyFieldError
import com.plcoding.cardiganapp.feature.boards.presentation.components.toBoardKeyFieldError
import com.plcoding.cardiganapp.feature.boards.presentation.components.toNameError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * New board dialog. The key follows the name until the user edits it (D7). Errors show from the
 * first submit and then update live (D8). A failed create is never repeated on its own.
 */
class NewBoardViewModel(
    private val repository: BoardRepository,
    private val toastController: ToastController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(restore())
    val state: StateFlow<NewBoardState> = _state.asStateFlow()

    private val _events = Channel<NewBoardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var keyEdited: Boolean
        get() = savedStateHandle[KEY_KEY_EDITED] ?: false
        set(value) {
            savedStateHandle[KEY_KEY_EDITED] = value
        }

    fun onAction(action: NewBoardAction) {
        if (_state.value.isSaving) return
        when (action) {
            is NewBoardAction.NameChange -> {
                update {
                    if (keyEdited) {
                        copy(
                            name = action.name,
                        )
                    } else {
                        copy(name = action.name, key = BoardKeyPrefill.fromName(action.name))
                    }
                }
            }

            is NewBoardAction.KeyChange -> {
                keyEdited = true
                update { copy(key = BoardKeyRule.normalize(action.key)) }
            }

            NewBoardAction.Create -> {
                create()
            }

            NewBoardAction.Cancel -> {
                _events.trySend(NewBoardEvent.Close)
            }
        }
    }

    private fun update(change: NewBoardState.() -> NewBoardState) {
        _state.update { current ->
            val changed = current.change()
            if (changed.submitted) changed.validated() else changed
        }
        save()
    }

    private fun create() {
        val validated = _state.value.copy(submitted = true).validated()
        _state.value = validated
        save()
        if (validated.nameError != null || validated.keyError != null) return

        val name = normalizeText(validated.name)
        val key = BoardKeyRule.normalize(validated.key)
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = repository.createBoard(name, key)) {
                is Result.Success -> {
                    val board = result.data
                    _events.send(NewBoardEvent.Created(board.id.toString(), board.name, board.key))
                }

                is Result.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    when (val error = result.error) {
                        is BoardSaveError.InvalidFields -> {
                            _state.update {
                                it.copy(
                                    nameError = error.name?.toNameError(),
                                    keyError = error.key?.toBoardKeyFieldError(key),
                                )
                            }
                        }

                        is BoardSaveError.Network, BoardSaveError.NotFound -> {
                            toastController.show(
                                ToastMessage(UiText.Resource(Res.string.new_board_failed), ToastType.Error),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun NewBoardState.validated(): NewBoardState {
        val key = BoardKeyRule.normalize(key)
        val keyRule = BoardKeyRule.validate(key)
        val keyError =
            when {
                keyRule != null -> BoardKeyFieldError.Rule(keyRule)

                repository.boards.value
                    .orEmpty()
                    .any { it.key == key } -> BoardKeyFieldError.AlreadyUsed(key)

                else -> null
            }
        return copy(nameError = validateName(normalizeText(name), BOARD_NAME_MAX_LENGTH), keyError = keyError)
    }

    private fun save() {
        val state = _state.value
        savedStateHandle[KEY_NAME] = state.name
        savedStateHandle[KEY_KEY] = state.key
        savedStateHandle[KEY_SUBMITTED] = state.submitted
    }

    private fun restore(): NewBoardState {
        val restored =
            NewBoardState(
                name = savedStateHandle[KEY_NAME] ?: "",
                key = savedStateHandle[KEY_KEY] ?: "",
                submitted = savedStateHandle[KEY_SUBMITTED] ?: false,
            )
        return if (restored.submitted) restored.validated() else restored
    }

    private companion object {
        const val KEY_NAME = "new_board_name"
        const val KEY_KEY = "new_board_key"
        const val KEY_KEY_EDITED = "new_board_key_edited"
        const val KEY_SUBMITTED = "new_board_submitted"
    }
}
