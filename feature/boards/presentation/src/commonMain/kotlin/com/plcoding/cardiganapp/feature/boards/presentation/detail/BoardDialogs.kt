package com.plcoding.cardiganapp.feature.boards.presentation.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_view_cancel
import cardiganapp.feature.boards.presentation.generated.resources.board_view_delete_column
import cardiganapp.feature.boards.presentation.generated.resources.board_view_delete_column_body
import cardiganapp.feature.boards.presentation.generated.resources.board_view_delete_column_body_empty
import cardiganapp.feature.boards.presentation.generated.resources.board_view_delete_column_confirm
import cardiganapp.feature.boards.presentation.generated.resources.board_view_delete_column_title
import cardiganapp.feature.boards.presentation.generated.resources.board_view_discard
import cardiganapp.feature.boards.presentation.generated.resources.board_view_discard_card_body
import cardiganapp.feature.boards.presentation.generated.resources.board_view_discard_column_body
import cardiganapp.feature.boards.presentation.generated.resources.board_view_discard_title
import cardiganapp.feature.boards.presentation.generated.resources.board_view_keep_editing
import cardiganapp.feature.boards.presentation.generated.resources.board_view_name
import cardiganapp.feature.boards.presentation.generated.resources.board_view_rename_column_title
import cardiganapp.feature.boards.presentation.generated.resources.board_view_save
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.inputs.ErrorMessage
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Field
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Input
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.overlays.CardiganDialog
import com.plcoding.cardiganapp.core.presentation.text.toUiText
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BoardDialogs(
    state: BoardState,
    onAction: (BoardAction) -> Unit,
) {
    when (val dialog = state.dialog) {
        is BoardDialog.RenameColumn -> {
            RenameColumnDialog(dialog, onAction)
        }

        is BoardDialog.DeleteColumn -> {
            DeleteColumnDialog(dialog, onAction)
        }

        is BoardDialog.DiscardInlineField -> {
            val open = state.inlineField?.target ?: return
            val columnName = (open as? InlineTarget.QuickAdd)?.let { state.column(it.columnId)?.name }
            DiscardDialog(columnName, onAction)
        }

        null -> {
            Unit
        }
    }
}

@Composable
private fun RenameColumnDialog(
    dialog: BoardDialog.RenameColumn,
    onAction: (BoardAction) -> Unit,
) {
    val error = dialog.error?.toUiText()?.asString()
    CardiganDialog(
        title = stringResource(Res.string.board_view_rename_column_title),
        onDismissRequest = { onAction(BoardAction.DialogDismiss) },
        content = {
            Field(label = stringResource(Res.string.board_view_name)) {
                Input(
                    value = dialog.text,
                    onValueChange = { onAction(BoardAction.RenameTextChange(it)) },
                    isError = error != null,
                    enabled = !dialog.isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAction(BoardAction.RenameSave) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) ErrorMessage(text = error)
            }
        },
        actions = {
            CardiganButton(
                text = stringResource(Res.string.board_view_cancel),
                onClick = { onAction(BoardAction.DialogDismiss) },
                style = ButtonStyle.Secondary,
                enabled = !dialog.isSaving,
                modifier = if (isTouchSize) Modifier.weight(1f) else Modifier,
            )
            CardiganButton(
                text = stringResource(Res.string.board_view_save),
                onClick = { onAction(BoardAction.RenameSave) },
                enabled = dialog.canSave,
                isLoading = dialog.isSaving,
                modifier = if (isTouchSize) Modifier.weight(1f) else Modifier,
            )
        },
    )
}

@Composable
private fun DeleteColumnDialog(
    dialog: BoardDialog.DeleteColumn,
    onAction: (BoardAction) -> Unit,
) {
    val count = dialog.cardCount
    CardiganDialog(
        title = stringResource(Res.string.board_view_delete_column_title, dialog.name),
        description =
            if (count > 0) {
                pluralStringResource(Res.plurals.board_view_delete_column_body, count, count)
            } else {
                stringResource(Res.string.board_view_delete_column_body_empty)
            },
        onDismissRequest = { onAction(BoardAction.DialogDismiss) },
        actions = {
            CardiganButton(
                text = stringResource(Res.string.board_view_cancel),
                onClick = { onAction(BoardAction.DialogDismiss) },
                style = ButtonStyle.Secondary,
                enabled = !dialog.isSaving,
                modifier = if (isTouchSize) Modifier.weight(1f) else Modifier,
            )
            CardiganButton(
                text =
                    if (count > 0) {
                        pluralStringResource(Res.plurals.board_view_delete_column_confirm, count, count)
                    } else {
                        stringResource(Res.string.board_view_delete_column)
                    },
                onClick = { onAction(BoardAction.DeleteConfirm) },
                style = ButtonStyle.Danger,
                isLoading = dialog.isSaving,
                modifier = if (isTouchSize) Modifier.weight(1f) else Modifier,
            )
        },
    )
}

/** D41, the Card detail "Unsaved changes" pattern. [columnName] is null for the add column field. */
@Composable
private fun DiscardDialog(
    columnName: String?,
    onAction: (BoardAction) -> Unit,
) {
    CardiganDialog(
        title = stringResource(Res.string.board_view_discard_title),
        description =
            if (columnName != null) {
                stringResource(Res.string.board_view_discard_card_body, columnName)
            } else {
                stringResource(Res.string.board_view_discard_column_body)
            },
        onDismissRequest = { onAction(BoardAction.DiscardKeepEditing) },
        actions = {
            CardiganButton(
                text = stringResource(Res.string.board_view_keep_editing),
                onClick = { onAction(BoardAction.DiscardKeepEditing) },
                style = ButtonStyle.Secondary,
                modifier = if (isTouchSize) Modifier.weight(1f) else Modifier,
            )
            CardiganButton(
                text = stringResource(Res.string.board_view_discard),
                onClick = { onAction(BoardAction.DiscardConfirm) },
                style = ButtonStyle.Danger,
                modifier = if (isTouchSize) Modifier.weight(1f) else Modifier,
            )
        },
    )
}
