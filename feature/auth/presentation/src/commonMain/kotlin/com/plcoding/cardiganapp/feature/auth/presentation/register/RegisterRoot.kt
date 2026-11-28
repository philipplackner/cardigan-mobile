package com.plcoding.cardiganapp.feature.auth.presentation.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cardiganapp.feature.auth.presentation.generated.resources.Res
import cardiganapp.feature.auth.presentation.generated.resources.auth_email_label
import cardiganapp.feature.auth.presentation.generated.resources.auth_email_placeholder
import cardiganapp.feature.auth.presentation.generated.resources.auth_log_in
import cardiganapp.feature.auth.presentation.generated.resources.auth_password_label
import cardiganapp.feature.auth.presentation.generated.resources.register_create_account
import cardiganapp.feature.auth.presentation.generated.resources.register_have_account
import cardiganapp.feature.auth.presentation.generated.resources.register_loading
import cardiganapp.feature.auth.presentation.generated.resources.register_password_placeholder
import cardiganapp.feature.auth.presentation.generated.resources.register_repeat_password_label
import cardiganapp.feature.auth.presentation.generated.resources.register_repeat_password_placeholder
import cardiganapp.feature.auth.presentation.generated.resources.register_rule_digit
import cardiganapp.feature.auth.presentation.generated.resources.register_rule_length
import cardiganapp.feature.auth.presentation.generated.resources.register_rule_lowercase
import cardiganapp.feature.auth.presentation.generated.resources.register_rule_uppercase
import cardiganapp.feature.auth.presentation.generated.resources.register_title
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.core.designsystem.components.inputs.PasswordRule
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.feature.auth.domain.validation.PasswordCheck
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthBannerView
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthFieldKind
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthFooterLink
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthInputField
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthLayout
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthTextField
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterRoot(
    onLogIn: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegisterScreen(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
            if (action == RegisterAction.LogInClick) onLogIn()
        },
    )
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
) {
    val editable = !state.isLoading
    AuthLayout(
        title = stringResource(Res.string.register_title),
        footer = {
            AuthFooterLink(
                prompt = stringResource(Res.string.register_have_account),
                link = stringResource(Res.string.auth_log_in),
                onClick = { onAction(RegisterAction.LogInClick) },
                enabled = editable,
            )
        },
    ) {
        state.banner?.let { AuthBannerView(it, modifier = Modifier.fillMaxWidth()) }
        AuthTextField(
            label = stringResource(Res.string.auth_email_label),
            value = state.email,
            onValueChange = { onAction(RegisterAction.EmailChange(it)) },
            kind = AuthFieldKind.Email,
            imeAction = ImeAction.Next,
            onFocusChange = { onAction(RegisterAction.FocusChange(AuthInputField.Email, it)) },
            enabled = editable,
            isError = state.emailError != null,
            placeholder = stringResource(Res.string.auth_email_placeholder),
            error = state.emailError?.let { stringResource(it.resource) },
        )
        AuthTextField(
            label = stringResource(Res.string.auth_password_label),
            value = state.password,
            onValueChange = { onAction(RegisterAction.PasswordChange(it)) },
            kind = AuthFieldKind.NewPassword,
            imeAction = ImeAction.Next,
            onFocusChange = { onAction(RegisterAction.FocusChange(AuthInputField.Password, it)) },
            enabled = editable,
            isError = state.passwordIsError,
            placeholder = stringResource(Res.string.register_password_placeholder),
            error = state.passwordError?.let { stringResource(it.resource) },
            below = { PasswordRules(state.rules) },
        )
        AuthTextField(
            label = stringResource(Res.string.register_repeat_password_label),
            value = state.repeatPassword,
            onValueChange = { onAction(RegisterAction.RepeatPasswordChange(it)) },
            kind = AuthFieldKind.NewPassword,
            imeAction = ImeAction.Done,
            onFocusChange = { onAction(RegisterAction.FocusChange(AuthInputField.RepeatPassword, it)) },
            enabled = editable,
            isError = state.repeatPasswordError != null,
            placeholder = stringResource(Res.string.register_repeat_password_placeholder),
            error = state.repeatPasswordError?.let { stringResource(it.resource) },
            onDone = { onAction(RegisterAction.Submit) },
        )
        CardiganButton(
            text =
                stringResource(
                    if (state.isLoading) Res.string.register_loading else Res.string.register_create_account,
                ),
            onClick = { onAction(RegisterAction.Submit) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The Figma "Password rules" grid: two columns, 12+ characters and Lowercase on top. */
@Composable
private fun PasswordRules(rules: PasswordCheck) {
    val rows =
        listOf(
            listOf(
                Res.string.register_rule_length to rules.hasMinLength,
                Res.string.register_rule_lowercase to rules.hasLowercase,
            ),
            listOf(
                Res.string.register_rule_uppercase to rules.hasUppercase,
                Res.string.register_rule_digit to rules.hasDigit,
            ),
        )
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.space.s4)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.space.s12)) {
                row.forEach { (text: StringResource, met) ->
                    PasswordRule(text = stringResource(text), met = met, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
