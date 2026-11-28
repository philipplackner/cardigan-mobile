package com.plcoding.cardiganapp.feature.boards.presentation.renameboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_dialog_cancel
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_key_hint
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_save
import cardiganapp.feature.boards.presentation.generated.resources.rename_board_title
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.overlays.CardiganDialog
import com.plcoding.cardiganapp.core.presentation.text.toUiText
import com.plcoding.cardiganapp.core.presentation.util.ObserveAsEvents
import com.plcoding.cardiganapp.feature.boards.presentation.components.BoardKeyField
import com.plcoding.cardiganapp.feature.boards.presentation.components.BoardNameField
import org.jetbrains.compose.resources.stringResource

/** Rename board dialog. [onRenamed] runs after the dialog asked to close, with the server's name. */
@Composable
fun RenameBoardRoot(
    viewModel: RenameBoardViewModel,
    onClose: () -> Unit,
    onRenamed: (newName: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RenameBoardEvent.Renamed -> {
                onClose()
                onRenamed(event.newName)
            }

            RenameBoardEvent.Close -> {
                onClose()
            }
        }
    }
    RenameBoardScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun RenameBoardScreen(
    state: RenameBoardState,
    onAction: (RenameBoardAction) -> Unit,
) {
    if (state.boardName == null) return
    val touch = isTouchSize
    val editable = !state.isSaving
    CardiganDialog(
        title = stringResource(Res.string.rename_board_title),
        onDismissRequest = { onAction(RenameBoardAction.Cancel) },
        content = {
            BoardNameField(
                value = state.name,
                onValueChange = { onAction(RenameBoardAction.NameChange(it)) },
                error = state.nameError?.toUiText(),
                enabled = editable,
                imeAction = ImeAction.Done,
                onImeAction = { onAction(RenameBoardAction.Save) },
            )
            BoardKeyField(
                value = state.key,
                onValueChange = {},
                hint = stringResource(Res.string.rename_board_key_hint),
                error = null,
                enabled = true,
                readOnly = true,
                onDone = {},
            )
        },
        actions = {
            val buttonModifier = if (touch) Modifier.weight(1f) else Modifier
            CardiganButton(
                text = stringResource(Res.string.board_dialog_cancel),
                onClick = { onAction(RenameBoardAction.Cancel) },
                style = ButtonStyle.Secondary,
                enabled = editable,
                modifier = buttonModifier,
            )
            CardiganButton(
                text = stringResource(Res.string.rename_board_save),
                onClick = { onAction(RenameBoardAction.Save) },
                enabled = state.canSave,
                isLoading = state.isSaving,
                modifier = buttonModifier,
            )
        },
    )
}
