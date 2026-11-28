package com.plcoding.cardiganapp.feature.boards.presentation.detail

import kotlin.uuid.Uuid

sealed interface BoardEvent {
    /** The board no longer exists. The toast is already shown. */
    data object BoardMissing : BoardEvent

    /** The board was deleted from its menu. No toast (D34). */
    data object BoardDeleted : BoardEvent

    /** A column was added: scroll the strip to it (D32). */
    data class ScrollToColumn(
        val columnId: Uuid,
    ) : BoardEvent

    /** Scroll the inline field into view and focus it. */
    data class FocusInlineField(
        val target: InlineTarget,
    ) : BoardEvent
}
