package com.plcoding.cardiganapp.feature.boards.presentation.newboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_dialog_cancel
import cardiganapp.feature.boards.presentation.generated.resources.new_board_columns_note
import cardiganapp.feature.boards.presentation.generated.resources.new_board_create
import cardiganapp.feature.boards.presentation.generated.resources.new_board_key_hint
import cardiganapp.feature.boards.presentation.generated.resources.new_board_key_hint_placeholder
import cardiganapp.feature.boards.presentation.generated.resources.new_board_title
import com.plcoding.cardiganapp.core.designsystem.components.actions.ButtonStyle
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.interaction.isTouchSize
import com.plcoding.cardiganapp.core.designsystem.components.overlays.CardiganDialog
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.presentation.text.toUiText
import com.plcoding.cardiganapp.core.presentation.util.ObserveAsEvents
import com.plcoding.cardiganapp.feature.boards.presentation.components.BoardKeyField
import com.plcoding.cardiganapp.feature.boards.presentation.components.BoardNameField
import com.plcoding.cardiganapp.feature.boards.presentation.components.toUiText
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** New board dialog. [onCreated] runs after the dialog asked to close, with the server's board. */
@Composable
fun NewBoardRoot(
    onClose: () -> Unit,
    onCreated: (boardId: String, name: String, key: String) -> Unit,
    viewModel: NewBoardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NewBoardEvent.Created -> {
                onClose()
                onCreated(event.boardId, event.name, event.key)
            }

            NewBoardEvent.Close -> {
                onClose()
            }
        }
    }
    NewBoardScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun NewBoardScreen(
    state: NewBoardState,
    onAction: (NewBoardAction) -> Unit,
) {
    val touch = isTouchSize
    val editable = !state.isSaving
    CardiganDialog(
        title = stringResource(Res.string.new_board_title),
        onDismissRequest = { onAction(NewBoardAction.Cancel) },
        content = {
            BoardNameField(
                value = state.name,
                onValueChange = { onAction(NewBoardAction.NameChange(it)) },
                error = state.nameError?.toUiText(),
                enabled = editable,
                imeAction = ImeAction.Next,
                onImeAction = {},
            )
            BoardKeyField(
                value = state.key,
                onValueChange = { onAction(NewBoardAction.KeyChange(it)) },
                hint =
                    stringResource(
                        Res.string.new_board_key_hint,
                        state.key.ifEmpty { stringResource(Res.string.new_board_key_hint_placeholder) },
                    ),
                error = state.keyError?.toUiText(),
                enabled = editable,
                readOnly = false,
                onDone = { onAction(NewBoardAction.Create) },
            )
            Text(
                text = stringResource(Res.string.new_board_columns_note),
                modifier = Modifier.fillMaxWidth(),
                style = AppTheme.textStyles.sans13_5Regular145,
                color = AppTheme.colors.text.muted,
            )
        },
        actions = {
            val buttonModifier = if (touch) Modifier.weight(1f) else Modifier
            CardiganButton(
                text = stringResource(Res.string.board_dialog_cancel),
                onClick = { onAction(NewBoardAction.Cancel) },
                style = ButtonStyle.Secondary,
                enabled = editable,
                modifier = buttonModifier,
            )
            CardiganButton(
                text = stringResource(Res.string.new_board_create),
                onClick = { onAction(NewBoardAction.Create) },
                enabled = state.canCreate,
                isLoading = state.isSaving,
                modifier = buttonModifier,
            )
        },
    )
}
