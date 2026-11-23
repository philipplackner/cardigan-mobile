package com.plcoding.cardiganapp.feature.auth.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.plcoding.cardiganapp.core.designsystem.components.inputs.ErrorMessage
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Field
import com.plcoding.cardiganapp.core.designsystem.components.inputs.Input

/** What a field holds: decides the keyboard, masking and the autofill hint (D12). */
enum class AuthFieldKind { Email, Password, NewPassword }

/**
 * A labelled auth field. Passwords are always masked. With [ImeAction.Next] the keyboard moves
 * focus to the next field; with [ImeAction.Done] it calls [onDone].
 */
@Composable
internal fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    kind: AuthFieldKind,
    imeAction: ImeAction,
    onFocusChange: (Boolean) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    onDone: () -> Unit = {},
    below: @Composable ColumnScope.() -> Unit = {},
) {
    val isPassword = kind != AuthFieldKind.Email
    Field(label = label, modifier = modifier) {
        Input(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) }
                .semantics {
                    contentType = when (kind) {
                        AuthFieldKind.Email -> ContentType.EmailAddress
                        AuthFieldKind.Password -> ContentType.Password
                        AuthFieldKind.NewPassword -> ContentType.NewPassword
                    }
                },
            placeholder = placeholder,
            isError = isError,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
                imeAction = imeAction,
            ),
            keyboardActions = if (imeAction == ImeAction.Done) KeyboardActions(onDone = { onDone() }) else KeyboardActions.Default,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        )
        below()
        if (error != null) ErrorMessage(text = error)
    }
}
