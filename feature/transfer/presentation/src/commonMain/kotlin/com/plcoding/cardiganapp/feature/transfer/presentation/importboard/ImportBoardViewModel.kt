package com.plcoding.cardiganapp.feature.transfer.presentation.importboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cardiganapp.feature.transfer.presentation.generated.resources.Res
import cardiganapp.feature.transfer.presentation.generated.resources.import_success
import com.plcoding.cardiganapp.core.domain.board.BoardKeyRule
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.core.presentation.toast.ToastType
import com.plcoding.cardiganapp.core.presentation.util.UiText
import com.plcoding.cardiganapp.feature.transfer.domain.BoardKeyOwnerLookup
import com.plcoding.cardiganapp.feature.transfer.domain.BoardTransferRepository
import com.plcoding.cardiganapp.feature.transfer.domain.ExportFileChecker
import com.plcoding.cardiganapp.feature.transfer.domain.ImportError
import com.plcoding.cardiganapp.feature.transfer.domain.ImportFileRules
import com.plcoding.cardiganapp.feature.transfer.presentation.platform.PickedFile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ImportBoardViewModel(
    private val repository: BoardTransferRepository,
    private val checker: ExportFileChecker,
    private val keyOwnerLookup: BoardKeyOwnerLookup,
    private val toastController: ToastController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _events = Channel<ImportBoardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _state = MutableStateFlow(ImportBoardState(phase = restorePhase()))
    val state: StateFlow<ImportBoardState> = _state.asStateFlow()

    private var pickedFile: PickedFileInfo? = null
    private var pickedBytes: ByteArray? = null
    private var fileKey: String? = null

    private var pickerOpened: Boolean
        get() = savedStateHandle[KEY_PICKER_OPENED] ?: false
        set(value) {
            savedStateHandle[KEY_PICKER_OPENED] = value
        }

    init {
        if (!savedStateHandle.contains(KEY_PHASE)) {
            setPhase(ImportPhase.Picking)
            openPicker()
        }
    }

    fun onAction(action: ImportBoardAction) {
        when (action) {
            ImportBoardAction.DropZoneClick -> onDropZoneClick()
            is ImportBoardAction.FilePicked -> onFilePicked(action.file)
            ImportBoardAction.PickerCancelled -> onPickerCancelled()
            is ImportBoardAction.KeyChange -> onKeyChange(action.key)
            ImportBoardAction.ImportWithKey -> onImportWithKey()
        }
    }

    private fun onDropZoneClick() {
        if (_state.value.phase is ImportPhase.ErrorShown && !pickerOpened) openPicker()
    }

    private fun openPicker() {
        pickerOpened = true
        _events.trySend(ImportBoardEvent.OpenPicker)
    }

    private fun onPickerCancelled() {
        pickerOpened = false
        if (_state.value.phase is ImportPhase.Picking) finish()
    }

    private fun onFilePicked(file: PickedFile) {
        pickerOpened = false
        val phase = _state.value.phase
        if (phase !is ImportPhase.Picking && phase !is ImportPhase.ErrorShown) return
        val info = PickedFileInfo(file.name, file.sizeBytes)
        val bytes = file.bytes
        if (bytes == null || ImportFileRules.isTooLarge(file.sizeBytes)) {
            setPhase(ImportPhase.ErrorShown(info, ImportDialogError.TooLarge))
            return
        }
        val checked = checker.check(bytes)
        if (checked == null) {
            setPhase(ImportPhase.ErrorShown(info, ImportDialogError.InvalidFile))
            return
        }
        pickedFile = info
        pickedBytes = bytes
        fileKey = checked.key?.let(BoardKeyRule::normalize)
        upload(key = null, retryFrom = null)
    }

    private fun onKeyChange(key: String) {
        val phase = _state.value.phase as? ImportPhase.ChoosingKey ?: return
        if (key == phase.key) return
        setPhase(phase.copy(key = key, fieldError = null))
    }

    private fun onImportWithKey() {
        val phase = _state.value.phase as? ImportPhase.ChoosingKey ?: return
        val key = BoardKeyRule.normalize(phase.key)
        if (key in phase.usedKeys) return
        val rule = BoardKeyRule.validate(key)
        if (rule != null) {
            setPhase(phase.copy(fieldError = KeyFieldError.Rule(rule)))
            return
        }
        upload(key = key, retryFrom = phase)
    }

    /** [key] is null for the first upload. [retryFrom] is the key dialog a retry was started from. */
    private fun upload(
        key: String?,
        retryFrom: ImportPhase.ChoosingKey?,
    ) {
        val file = pickedFile ?: return
        val bytes = pickedBytes ?: return
        setPhase(ImportPhase.Uploading(file.name))
        viewModelScope.launch {
            when (val result = repository.importBoard(bytes, key)) {
                is Result.Success -> {
                    val board = result.data
                    toastController.show(
                        ToastMessage(
                            UiText.Resource(Res.string.import_success, arrayOf(board.name, board.key)),
                            ToastType.Success,
                        ),
                    )
                    setPhase(ImportPhase.Done)
                    _events.send(ImportBoardEvent.Imported(board))
                }

                is Result.Error -> {
                    onUploadFailed(result.error, file, key, retryFrom)
                }
            }
        }
    }

    private suspend fun onUploadFailed(
        error: ImportError,
        file: PickedFileInfo,
        key: String?,
        retryFrom: ImportPhase.ChoosingKey?,
    ) {
        val clash = key ?: fileKey
        when {
            error == ImportError.FileTooLarge -> {
                setPhase(ImportPhase.ErrorShown(file, ImportDialogError.TooLarge))
            }

            error == ImportError.InvalidFile -> {
                setPhase(ImportPhase.ErrorShown(file, ImportDialogError.InvalidFile))
            }

            error == ImportError.KeyAlreadyUsed && clash != null -> {
                setPhase(
                    ImportPhase.ChoosingKey(
                        key = clash,
                        clashKey = clash,
                        boardName = keyOwnerLookup.boardNameForKey(clash),
                        usedKeys = retryFrom?.usedKeys.orEmpty() + clash,
                        fieldError = KeyFieldError.AlreadyUsed(clash),
                    ),
                )
            }

            error is ImportError.KeyInvalid && key != null && retryFrom != null -> {
                setPhase(
                    retryFrom.copy(key = key, fieldError = KeyFieldError.Rule(error.rule)),
                )
            }

            else -> {
                setPhase(ImportPhase.ErrorShown(file, ImportDialogError.Failed))
                _events.send(ImportBoardEvent.BoardListChanged)
            }
        }
    }

    private fun finish() {
        setPhase(ImportPhase.Done)
        _events.trySend(ImportBoardEvent.Finished)
    }

    private fun setPhase(phase: ImportPhase) {
        _state.update { it.copy(phase = phase) }
        savedStateHandle[KEY_PHASE] = json.encodeToString(ImportPhase.serializer(), phase)
    }

    /**
     * The picked bytes aren't saved, and the outcome of an upload that was running is unknown, so
     * a restored upload or key clash shows the import-failed error and reloads the list.
     */
    private fun restorePhase(): ImportPhase {
        val saved =
            savedStateHandle
                .get<String>(KEY_PHASE)
                ?.let { runCatching { json.decodeFromString(ImportPhase.serializer(), it) }.getOrNull() }
                ?: return ImportPhase.Picking
        return when (saved) {
            is ImportPhase.Uploading, is ImportPhase.ChoosingKey -> {
                val restored = ImportPhase.ErrorShown(file = null, error = ImportDialogError.Failed)
                savedStateHandle[KEY_PHASE] = json.encodeToString(ImportPhase.serializer(), restored)
                _events.trySend(ImportBoardEvent.BoardListChanged)
                restored
            }

            else -> {
                saved
            }
        }
    }

    private companion object {
        const val KEY_PHASE = "import_phase"
        const val KEY_PICKER_OPENED = "import_picker_opened"
        val json = Json { ignoreUnknownKeys = true }
    }
}
