package com.plcoding.cardiganapp.feature.boards.presentation.detail

import com.plcoding.cardiganapp.core.domain.board.BoardContent
import com.plcoding.cardiganapp.core.domain.board.Column
import com.plcoding.cardiganapp.core.domain.label.Label
import com.plcoding.cardiganapp.core.domain.text.NameError
import kotlin.uuid.Uuid

data class BoardState(
    val content: BoardLoadState = BoardLoadState.Loading,
    /** Name and key the board list passed in, shown while the board loads or failed to load (D39). */
    val headerHint: BoardHeaderHint? = null,
    val isRefreshing: Boolean = false,
    /** The column whose list was pulled. Its indicator shows while the whole board reloads. */
    val refreshingColumnId: Uuid? = null,
    /** At most one inline field is open (D22). */
    val inlineField: InlineField? = null,
    val dialog: BoardDialog? = null,
) {
    val loaded: BoardLoadState.Loaded? get() = content as? BoardLoadState.Loaded

    /** The header title and key: the loaded board, else the hint, else none. */
    val headerName: String? get() = loaded?.let { it.content.board.name } ?: headerHint?.name
    val headerKey: String? get() = loaded?.let { it.content.board.key } ?: headerHint?.key

    fun column(id: Uuid): Column? = loaded?.content?.columns?.firstOrNull { it.id == id }
}

data class BoardHeaderHint(
    val name: String,
    val key: String,
)

sealed interface BoardLoadState {
    data object Loading : BoardLoadState

    data object Error : BoardLoadState

    data class Loaded(
        val content: BoardContent,
        val labelsById: Map<Uuid, Label>,
    ) : BoardLoadState
}

/** Where an inline field sits. */
sealed interface InlineTarget {
    data class QuickAdd(
        val columnId: Uuid,
    ) : InlineTarget

    data object AddColumn : InlineTarget
}

/**
 * An inline name field. [showErrors] turns true on the first submit; from then on [error] follows
 * the text (D12).
 */
data class InlineField(
    val target: InlineTarget,
    val text: String = "",
    val isSaving: Boolean = false,
    val showErrors: Boolean = false,
    val error: NameError? = null,
) {
    val canSubmit: Boolean get() = !isSaving && error == null
}

sealed interface BoardDialog {
    data class RenameColumn(
        val columnId: Uuid,
        val originalName: String,
        val text: String,
        val isSaving: Boolean = false,
        val showErrors: Boolean = false,
        val error: NameError? = null,
    ) : BoardDialog {
        val canSave: Boolean get() = !isSaving && error == null
    }

    data class DeleteColumn(
        val columnId: Uuid,
        val name: String,
        val cardCount: Int,
        val isSaving: Boolean = false,
    ) : BoardDialog

    /** Asks before the open inline field with text is dropped for [pending] (D41). */
    data class DiscardInlineField(
        val pending: InlineTarget,
    ) : BoardDialog
}
