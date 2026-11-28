package com.plcoding.cardiganapp.feature.boards.presentation.detail

import kotlin.uuid.Uuid

sealed interface BoardAction {
    data object Retry : BoardAction

    data class Refresh(
        val columnId: Uuid?,
    ) : BoardAction

    data object BackClick : BoardAction

    data class CardClick(
        val cardId: Uuid,
    ) : BoardAction

    data object RenameBoardClick : BoardAction

    data object DeleteBoardClick : BoardAction

    data object ExportClick : BoardAction

    data class BoardRenamed(
        val newName: String,
    ) : BoardAction

    data object BoardDeleted : BoardAction

    data class OpenQuickAdd(
        val columnId: Uuid,
    ) : BoardAction

    data object OpenAddColumn : BoardAction

    data class InlineTextChange(
        val text: String,
    ) : BoardAction

    data object InlineSubmit : BoardAction

    data object InlineCancel : BoardAction

    data object InlineOutsideTap : BoardAction

    data object DiscardKeepEditing : BoardAction

    data object DiscardConfirm : BoardAction

    data class MoveColumnLeft(
        val columnId: Uuid,
    ) : BoardAction

    data class MoveColumnRight(
        val columnId: Uuid,
    ) : BoardAction

    data class RenameColumnClick(
        val columnId: Uuid,
    ) : BoardAction

    data class DeleteColumnClick(
        val columnId: Uuid,
    ) : BoardAction

    data class RenameTextChange(
        val text: String,
    ) : BoardAction

    data object RenameSave : BoardAction

    data object DeleteConfirm : BoardAction

    data object DialogDismiss : BoardAction
}
