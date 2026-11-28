package com.plcoding.cardiganapp.feature.boards.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import cardiganapp.feature.boards.presentation.generated.resources.Res
import cardiganapp.feature.boards.presentation.generated.resources.board_dialog_key
import cardiganapp.feature.boards.presentation.generated.resources.board_dialog_name
import com.plcoding.cardiganapp.core.designsystem.components.inputs.ErrorMessage
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Field
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Input
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.presentation.util.UiText
import org.jetbrains.compose.resources.stringResource

/** The Name field of the board dialogs, with [error] below the input. */
@Composable
internal fun BoardNameField(
    value: String,
    onValueChange: (String) -> Unit,
    error: UiText?,
    enabled: Boolean,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Field(label = stringResource(Res.string.board_dialog_name), modifier = modifier.fillMaxWidth()) {
        Input(
            value = value,
            onValueChange = onValueChange,
            isError = error != null,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) ErrorMessage(text = error.asString())
    }
}

/** The Key field of the board dialogs. A shown [error] takes the place of [hint]. */
@Composable
internal fun BoardKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    error: UiText?,
    enabled: Boolean,
    readOnly: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Field(label = stringResource(Res.string.board_dialog_key), modifier = modifier.fillMaxWidth()) {
        Input(
            value = value,
            onValueChange = onValueChange,
            isError = error != null,
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            ErrorMessage(text = error.asString())
        } else {
            Text(
                text = hint,
                modifier = Modifier.fillMaxWidth(),
                style = AppTheme.textStyles.sans12Regular145,
                color = AppTheme.colors.text.muted,
            )
        }
    }
}
