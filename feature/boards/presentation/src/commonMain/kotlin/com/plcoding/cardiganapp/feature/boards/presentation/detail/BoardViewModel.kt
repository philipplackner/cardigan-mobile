package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.cardiganapp.core.domain.board.BoardContent
import com.plcoding.cardiganapp.core.domain.label.LabelSource
import com.plcoding.cardiganapp.core.domain.text.NameError
import com.plcoding.cardiganapp.core.domain.text.normalizeText
import com.plcoding.cardiganapp.core.domain.text.validateName
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.core.presentation.toast.ToastController
import com.plcoding.cardiganapp.core.presentation.toast.ToastMessage
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardDetailRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.BoardLoadError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateError
import com.plcoding.cardiganapp.feature.boards.domain.detail.CardCreateRepository
import com.plcoding.cardiganapp.feature.boards.domain.detail.ColumnWriteError
import kotlin.uuid.Uuid
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Column name limit from cardigan-shared-specs `domain-column`. */
const val COLUMN_NAME_MAX_LENGTH = 30

/** Card title limit from cardigan-shared-specs `domain-card`. */
const val CARD_TITLE_MAX_LENGTH = 200

/**
 * One board. Every write waits for the server (D21). Writes and reloads run one at a time through
 * [writeLock], in the order they arrive, and each response is applied to the board the previous
 * one left. A failed create or move reloads the board in the background, so a lost response can't
 * lead to a duplicate (`api-conventions` "Retries after a lost response").
 */
class BoardViewModel(
    private val boardRepository: BoardDetailRepository,
    private val cardRepository: CardCreateRepository,
    private val labelSource: LabelSource,
    private val toastController: ToastController,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val boardId: Uuid = Uuid.parse(requireNotNull(savedStateHandle.get<String>(ARG_BOARD_ID)))

    private val _state = MutableStateFlow(BoardState(headerHint = readHint(), inlineField = restoreInlineField()))
    val state: StateFlow<BoardState> = _state.asStateFlow()

    private val _events = Channel<BoardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val writeLock = Mutex()

    /** A rename or delete dialog saved before process death. It needs the loaded column to reopen. */
    private var pendingDialog: SavedDialog? = restoreDialog()
    private var missingReported = false

    init {
        viewModelScope.launch { _state.collect(::persist) }
        loadFirst()
    }

    fun onAction(action: BoardAction) {
        when (action) {
            BoardAction.Retry -> if (_state.value.content == BoardLoadState.Error) {
                _state.update { it.copy(content = BoardLoadState.Loading) }
                loadFirst()
            }
            is BoardAction.Refresh -> refresh(action.columnId)
            is BoardAction.BoardRenamed -> renameBoardLocally(action.newName)
            BoardAction.BoardDeleted -> _events.trySend(BoardEvent.BoardDeleted)

            is BoardAction.OpenQuickAdd -> openInline(InlineTarget.QuickAdd(action.columnId))
            BoardAction.OpenAddColumn -> openInline(InlineTarget.AddColumn)
            is BoardAction.InlineTextChange -> changeInlineText(action.text)
            BoardAction.InlineSubmit -> submitInline()
            BoardAction.InlineCancel -> updateField { if (it.isSaving) it else null }
            BoardAction.InlineOutsideTap -> updateField { field ->
                val closes = !field.isSaving && normalizeText(field.text).isEmpty()
                if (closes) null else field
            }
            BoardAction.DiscardKeepEditing -> keepEditing()
            BoardAction.DiscardConfirm -> discardAndSwitch()

            is BoardAction.MoveColumnLeft -> moveColumn(action.columnId, -1)
            is BoardAction.MoveColumnRight -> moveColumn(action.columnId, 1)
            is BoardAction.RenameColumnClick -> openRenameColumn(action.columnId)
            is BoardAction.DeleteColumnClick -> openDeleteColumn(action.columnId)
            is BoardAction.RenameTextChange -> changeRenameText(action.text)
            BoardAction.RenameSave -> saveRename()
            BoardAction.DeleteConfirm -> confirmDelete()
            BoardAction.DialogDismiss -> dismissDialog()

            BoardAction.BackClick,
            is BoardAction.CardClick,
            BoardAction.RenameBoardClick,
            BoardAction.DeleteBoardClick,
            BoardAction.ExportClick -> Unit
        }
    }

    // region Loading

    private fun loadFirst() {
        viewModelScope.launch {
            writeLock.withLock {
                when (val result = fetch()) {
                    is Result.Success -> applyLoaded(result.data)
                    is Result.Error -> when (result.error) {
                        BoardLoadError.BoardNotFound -> reportBoardMissing()
                        BoardLoadError.Failed -> _state.update { it.copy(content = BoardLoadState.Error) }
                    }
                }
            }
        }
    }

    private fun refresh(columnId: Uuid?) {
        val current = _state.value
        if (current.isRefreshing || current.loaded == null) return
        _state.update { it.copy(isRefreshing = true, refreshingColumnId = columnId) }
        viewModelScope.launch {
            try {
                writeLock.withLock {
                    when (val result = fetch()) {
                        is Result.Success -> applyLoaded(result.data)
                        is Result.Error -> when (result.error) {
                            BoardLoadError.BoardNotFound -> reportBoardMissing()
                            BoardLoadError.Failed -> toast(BoardViewMessages.refreshFailed())
                        }
                    }
                }
            } finally {
                _state.update { it.copy(isRefreshing = false, refreshingColumnId = null) }
            }
        }
    }

    /** Reload after a failed write. A failure keeps the board as shown and shows nothing. Call inside [writeLock]. */
    private suspend fun reloadInBackground() {
        when (val result = fetch()) {
            is Result.Success -> applyLoaded(result.data)
            is Result.Error -> if (result.error == BoardLoadError.BoardNotFound) reportBoardMissing()
        }
    }

    /** The board and the labels load together. Either failing fails the load (review A2). */
    private suspend fun fetch(): Result<BoardLoadState.Loaded, BoardLoadError> = coroutineScope {
        val board = async { boardRepository.loadBoard(boardId) }
        val labels = async { labelSource.labels() }
        val boardResult = board.await()
        val labelsResult = labels.await()
        when {
            boardResult is Result.Error -> boardResult
            labelsResult is Result.Error -> Result.Error(BoardLoadError.Failed)
            else -> Result.Success(
                BoardLoadState.Loaded(
                    content = (boardResult as Result.Success).data,
                    labelsById = (labelsResult as Result.Success).data.associateBy { it.id },
                ),
            )
        }
    }

    private fun applyLoaded(loaded: BoardLoadState.Loaded) {
        _state.update { it.copy(content = loaded).reconciled() }
        pendingDialog?.let { saved ->
            pendingDialog = null
            _state.update { it.copy(dialog = it.dialog ?: saved.reopen(it)) }
        }
    }

    /** Closes a field or dialog whose column is gone after a load (D37). Others keep their text. */
    private fun BoardState.reconciled(): BoardState {
        val field = inlineField?.takeUnless { field ->
            val target = field.target
            target is InlineTarget.QuickAdd && column(target.columnId) == null
        }
        val dialog = when (val dialog = dialog) {
            is BoardDialog.RenameColumn -> dialog.takeIf { column(it.columnId) != null }
            is BoardDialog.DeleteColumn -> dialog.takeIf { column(it.columnId) != null }
            is BoardDialog.DiscardInlineField -> dialog.takeIf { discard ->
                val pending = discard.pending
                field != null && (pending !is InlineTarget.QuickAdd || column(pending.columnId) != null)
            }
            null -> null
        }
        return copy(inlineField = field, dialog = dialog)
    }

    private suspend fun reportBoardMissing() {
        if (missingReported) return
        missingReported = true
        toast(BoardViewMessages.boardGone())
        _events.send(BoardEvent.BoardMissing)
    }

    private fun renameBoardLocally(newName: String) {
        _state.update { state ->
            val loaded = state.loaded
            state.copy(
                content = loaded?.copy(content = loaded.content.copy(board = loaded.content.board.copy(name = newName)))
                    ?: state.content,
                headerHint = state.headerHint?.copy(name = newName),
            )
        }
    }

    // endregion

    // region Inline fields

    private fun openInline(target: InlineTarget) {
        val current = _state.value
        if (current.loaded == null || current.dialog != null) return
        val open = current.inlineField
        when {
            open == null || normalizeText(open.text).isEmpty() && !open.isSaving -> {
                if (open?.target != target) _state.update { it.copy(inlineField = InlineField(target)) }
                _events.trySend(BoardEvent.FocusInlineField(target))
            }
            open.target == target -> _events.trySend(BoardEvent.FocusInlineField(target))
            open.isSaving -> Unit
            else -> _state.update { it.copy(dialog = BoardDialog.DiscardInlineField(target)) }
        }
    }

    private fun keepEditing() {
        val current = _state.value
        if (current.dialog !is BoardDialog.DiscardInlineField) return
        _state.update { it.copy(dialog = null) }
        current.inlineField?.let { _events.trySend(BoardEvent.FocusInlineField(it.target)) }
    }

    private fun discardAndSwitch() {
        val dialog = _state.value.dialog as? BoardDialog.DiscardInlineField ?: return
        _state.update { it.copy(dialog = null, inlineField = InlineField(dialog.pending)) }
        _events.trySend(BoardEvent.FocusInlineField(dialog.pending))
    }

    private fun changeInlineText(text: String) = updateField { field ->
        if (field.isSaving) field else field.copy(text = text).revalidated()
    }

    private fun submitInline() {
        val field = _state.value.inlineField ?: return
        if (field.isSaving) return
        val normalized = normalizeText(field.text)
        if (field.target is InlineTarget.QuickAdd && normalized.isEmpty()) return
        val checked = field.copy(showErrors = true).revalidated()
        if (checked.error != null) {
            _state.update { it.copy(inlineField = checked) }
            return
        }
        _state.update { it.copy(inlineField = checked.copy(isSaving = true)) }
        when (val target = field.target) {
            InlineTarget.AddColumn -> write { addColumn(normalized) }
            is InlineTarget.QuickAdd -> write { createCard(target.columnId, normalized) }
        }
    }

    private suspend fun addColumn(name: String) {
        when (val result = boardRepository.addColumn(boardId, name)) {
            is Result.Success -> {
                val column = result.data
                updateContent { it.copy(columns = it.columns + column) }
                _state.update { it.copy(inlineField = null) }
                _events.send(BoardEvent.ScrollToColumn(column.id))
            }
            is Result.Error -> {
                updateField { it.copy(isSaving = false) }
                when (val error = result.error) {
                    is ColumnWriteError.InvalidName -> showFieldError(error.rule)
                    ColumnWriteError.BoardNotFound -> reportBoardMissing()
                    ColumnWriteError.ColumnNotFound,
                    ColumnWriteError.Stale,
                    ColumnWriteError.Failed -> {
                        toast(BoardViewMessages.addColumnFailed())
                        reloadInBackground()
                    }
                }
            }
        }
    }

    private suspend fun createCard(columnId: Uuid, title: String) {
        val columnName = _state.value.column(columnId)?.name.orEmpty()
        when (val result = cardRepository.createCard(boardId, columnId, title)) {
            is Result.Success -> {
                val card = result.data
                updateContent { content ->
                    content.copy(columns = content.columns.map { if (it.id == columnId) it.copy(cards = it.cards + card) else it })
                }
                updateField { InlineField(it.target) }
            }
            is Result.Error -> {
                updateField { it.copy(isSaving = false) }
                when (val error = result.error) {
                    is CardCreateError.InvalidTitle -> showFieldError(error.rule)
                    CardCreateError.BoardNotFound -> reportBoardMissing()
                    CardCreateError.ColumnNotFound -> {
                        _state.update { it.copy(inlineField = null) }
                        reloadInBackground()
                        toast(BoardViewMessages.columnGone(columnName))
                    }
                    CardCreateError.Failed -> {
                        toast(BoardViewMessages.addCardFailed())
                        reloadInBackground()
                    }
                }
            }
        }
    }

    private fun showFieldError(error: NameError) =
        updateField { it.copy(showErrors = true, error = error) }

    private fun InlineField.revalidated(): InlineField {
        if (!showErrors) return copy(error = null)
        val normalized = normalizeText(text)
        val error = when (target) {
            InlineTarget.AddColumn -> validateName(normalized, COLUMN_NAME_MAX_LENGTH)
            // Empty quick add does nothing (D22), so it never shows "Enter a name."
            is InlineTarget.QuickAdd -> if (normalized.isEmpty()) null else validateName(normalized, CARD_TITLE_MAX_LENGTH)
        }
        return copy(error = error)
    }

    // endregion

    // region Column menu

    private fun moveColumn(columnId: Uuid, delta: Int) {
        if (_state.value.loaded == null) return
        write {
            val columns = _state.value.loaded?.content?.columns ?: return@write
            val index = columns.indexOfFirst { it.id == columnId }
            val target = index + delta
            if (index < 0 || target !in columns.indices) return@write
            val name = columns[index].name
            when (val result = boardRepository.moveColumn(boardId, columnId, target)) {
                is Result.Success -> applyColumnOrder(result.data)
                is Result.Error -> when (result.error) {
                    ColumnWriteError.Stale -> {
                        reloadInBackground()
                        toast(BoardViewMessages.moveStale(name))
                    }
                    ColumnWriteError.ColumnNotFound -> {
                        reloadInBackground()
                        toast(BoardViewMessages.columnGone(name))
                    }
                    ColumnWriteError.BoardNotFound -> reportBoardMissing()
                    is ColumnWriteError.InvalidName,
                    ColumnWriteError.Failed -> {
                        toast(BoardViewMessages.moveColumnFailed(name))
                        reloadInBackground()
                    }
                }
            }
        }
    }

    /** Reorders by the server's IDs. When they don't match the shown columns, the board reloads. */
    private suspend fun applyColumnOrder(ids: List<Uuid>) {
        val columns = _state.value.loaded?.content?.columns ?: return
        val byId = columns.associateBy { it.id }
        if (ids.size != columns.size || ids.toSet() != byId.keys) {
            reloadInBackground()
            return
        }
        updateContent { it.copy(columns = ids.map(byId::getValue)) }
    }

    private fun openRenameColumn(columnId: Uuid) {
        val column = _state.value.column(columnId) ?: return
        if (_state.value.dialog != null) return
        _state.update { it.copy(dialog = BoardDialog.RenameColumn(columnId, column.name, column.name)) }
    }

    private fun openDeleteColumn(columnId: Uuid) {
        val column = _state.value.column(columnId) ?: return
        if (_state.value.dialog != null) return
        _state.update { it.copy(dialog = BoardDialog.DeleteColumn(columnId, column.name, column.cards.size)) }
    }

    private fun changeRenameText(text: String) = updateRename { dialog ->
        if (dialog.isSaving) dialog else dialog.copy(text = text).revalidated()
    }

    private fun saveRename() {
        val dialog = _state.value.dialog as? BoardDialog.RenameColumn ?: return
        if (dialog.isSaving) return
        val checked = dialog.copy(showErrors = true).revalidated()
        if (checked.error != null) {
            _state.update { it.copy(dialog = checked) }
            return
        }
        val name = normalizeText(checked.text)
        if (name == normalizeText(dialog.originalName)) {
            _state.update { it.copy(dialog = null) }
            return
        }
        _state.update { it.copy(dialog = checked.copy(isSaving = true)) }
        write {
            when (val result = boardRepository.renameColumn(boardId, dialog.columnId, name)) {
                is Result.Success -> {
                    val renamed = result.data
                    updateContent { content ->
                        content.copy(columns = content.columns.map { if (it.id == dialog.columnId) it.copy(name = renamed.name) else it })
                    }
                    _state.update { it.copy(dialog = null) }
                }
                is Result.Error -> {
                    updateRename { it.copy(isSaving = false) }
                    when (val error = result.error) {
                        is ColumnWriteError.InvalidName -> updateRename { it.copy(showErrors = true, error = error.rule) }
                        ColumnWriteError.ColumnNotFound -> {
                            _state.update { it.copy(dialog = null) }
                            reloadInBackground()
                            toast(BoardViewMessages.columnGone(dialog.originalName))
                        }
                        ColumnWriteError.BoardNotFound -> reportBoardMissing()
                        ColumnWriteError.Stale,
                        ColumnWriteError.Failed -> toast(BoardViewMessages.renameColumnFailed(dialog.originalName))
                    }
                }
            }
        }
    }

    private fun BoardDialog.RenameColumn.revalidated(): BoardDialog.RenameColumn =
        copy(error = if (showErrors) validateName(normalizeText(text), COLUMN_NAME_MAX_LENGTH) else null)

    private fun confirmDelete() {
        val dialog = _state.value.dialog as? BoardDialog.DeleteColumn ?: return
        if (dialog.isSaving) return
        _state.update { it.copy(dialog = dialog.copy(isSaving = true)) }
        write {
            val result = boardRepository.deleteColumn(boardId, dialog.columnId)
            val error = (result as? Result.Error)?.error
            when (error) {
                null, ColumnWriteError.ColumnNotFound -> {
                    updateContent { content -> content.copy(columns = content.columns.filterNot { it.id == dialog.columnId }) }
                    _state.update { it.copy(dialog = null).reconciled() }
                    if (error != null) reloadInBackground()
                }
                ColumnWriteError.BoardNotFound -> {
                    updateDelete { it.copy(isSaving = false) }
                    reportBoardMissing()
                }
                else -> {
                    updateDelete { it.copy(isSaving = false) }
                    toast(BoardViewMessages.deleteColumnFailed(dialog.name))
                }
            }
        }
    }

    private fun dismissDialog() {
        when (val dialog = _state.value.dialog) {
            is BoardDialog.RenameColumn -> if (!dialog.isSaving) _state.update { it.copy(dialog = null) }
            is BoardDialog.DeleteColumn -> if (!dialog.isSaving) _state.update { it.copy(dialog = null) }
            is BoardDialog.DiscardInlineField -> keepEditing()
            null -> Unit
        }
    }

    // endregion

    // region Helpers

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { writeLock.withLock { block() } }
    }

    private fun updateContent(transform: (BoardContent) -> BoardContent) = _state.update { state ->
        val loaded = state.loaded ?: return@update state
        state.copy(content = loaded.copy(content = transform(loaded.content)))
    }

    private fun updateField(transform: (InlineField) -> InlineField?) = _state.update { state ->
        val field = state.inlineField ?: return@update state
        state.copy(inlineField = transform(field))
    }

    private fun updateRename(transform: (BoardDialog.RenameColumn) -> BoardDialog.RenameColumn) = _state.update { state ->
        val dialog = state.dialog as? BoardDialog.RenameColumn ?: return@update state
        state.copy(dialog = transform(dialog))
    }

    private fun updateDelete(transform: (BoardDialog.DeleteColumn) -> BoardDialog.DeleteColumn) = _state.update { state ->
        val dialog = state.dialog as? BoardDialog.DeleteColumn ?: return@update state
        state.copy(dialog = transform(dialog))
    }

    private fun toast(message: ToastMessage) = toastController.show(message)

    // endregion

    // region Saved state

    private fun readHint(): BoardHeaderHint? {
        val name = savedStateHandle.get<String>(ARG_NAME) ?: return null
        val key = savedStateHandle.get<String>(ARG_KEY) ?: return null
        return BoardHeaderHint(name, key)
    }

    private fun restoreInlineField(): InlineField? {
        val target = when (savedStateHandle.get<String>(KEY_INLINE_KIND)) {
            KIND_ADD_COLUMN -> InlineTarget.AddColumn
            KIND_QUICK_ADD -> savedStateHandle.get<String>(KEY_INLINE_COLUMN)?.let { InlineTarget.QuickAdd(Uuid.parse(it)) }
            else -> null
        } ?: return null
        return InlineField(
            target = target,
            text = savedStateHandle.get<String>(KEY_INLINE_TEXT).orEmpty(),
            showErrors = savedStateHandle.get<Boolean>(KEY_INLINE_SHOW_ERRORS) ?: false,
        ).revalidated()
    }

    private fun restoreDialog(): SavedDialog? {
        val kind = savedStateHandle.get<String>(KEY_DIALOG_KIND) ?: return null
        val columnId = savedStateHandle.get<String>(KEY_DIALOG_COLUMN)?.let(Uuid::parse) ?: return null
        return SavedDialog(
            kind = kind,
            columnId = columnId,
            text = savedStateHandle.get<String>(KEY_DIALOG_TEXT),
            showErrors = savedStateHandle.get<Boolean>(KEY_DIALOG_SHOW_ERRORS) ?: false,
        )
    }

    private fun SavedDialog.reopen(state: BoardState): BoardDialog? {
        val column = state.column(columnId) ?: return null
        return when (kind) {
            KIND_RENAME -> BoardDialog.RenameColumn(columnId, column.name, text ?: column.name, showErrors = showErrors).revalidated()
            KIND_DELETE -> BoardDialog.DeleteColumn(columnId, column.name, column.cards.size)
            else -> null
        }
    }

    private fun persist(state: BoardState) {
        val field = state.inlineField
        savedStateHandle[KEY_INLINE_KIND] = when (field?.target) {
            InlineTarget.AddColumn -> KIND_ADD_COLUMN
            is InlineTarget.QuickAdd -> KIND_QUICK_ADD
            null -> null
        }
        savedStateHandle[KEY_INLINE_COLUMN] = (field?.target as? InlineTarget.QuickAdd)?.columnId?.toString()
        savedStateHandle[KEY_INLINE_TEXT] = field?.text
        savedStateHandle[KEY_INLINE_SHOW_ERRORS] = field?.showErrors
        if (pendingDialog != null) return
        val dialog = state.dialog
        savedStateHandle[KEY_DIALOG_KIND] = when (dialog) {
            is BoardDialog.RenameColumn -> KIND_RENAME
            is BoardDialog.DeleteColumn -> KIND_DELETE
            else -> null
        }
        savedStateHandle[KEY_DIALOG_COLUMN] = when (dialog) {
            is BoardDialog.RenameColumn -> dialog.columnId.toString()
            is BoardDialog.DeleteColumn -> dialog.columnId.toString()
            else -> null
        }
        savedStateHandle[KEY_DIALOG_TEXT] = (dialog as? BoardDialog.RenameColumn)?.text
        savedStateHandle[KEY_DIALOG_SHOW_ERRORS] = (dialog as? BoardDialog.RenameColumn)?.showErrors
    }

    private data class SavedDialog(val kind: String, val columnId: Uuid, val text: String?, val showErrors: Boolean)

    companion object {
        /** Route argument names of `BoardRoute`. Navigation puts them in the [SavedStateHandle]. */
        const val ARG_BOARD_ID = "boardId"
        const val ARG_NAME = "name"
        const val ARG_KEY = "key"

        private const val KEY_INLINE_KIND = "board_inline_kind"
        private const val KEY_INLINE_COLUMN = "board_inline_column"
        private const val KEY_INLINE_TEXT = "board_inline_text"
        private const val KEY_INLINE_SHOW_ERRORS = "board_inline_show_errors"
        private const val KEY_DIALOG_KIND = "board_dialog_kind"
        private const val KEY_DIALOG_COLUMN = "board_dialog_column"
        private const val KEY_DIALOG_TEXT = "board_dialog_text"
        private const val KEY_DIALOG_SHOW_ERRORS = "board_dialog_show_errors"
        private const val KIND_ADD_COLUMN = "add_column"
        private const val KIND_QUICK_ADD = "quick_add"
        private const val KIND_RENAME = "rename"
        private const val KIND_DELETE = "delete"
    }
}
