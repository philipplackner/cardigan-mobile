package com.plcoding.cardiganapp.feature.auth.presentation.login

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
import cardiganapp.feature.auth.presentation.generated.resources.login_create_one
import cardiganapp.feature.auth.presentation.generated.resources.login_loading
import cardiganapp.feature.auth.presentation.generated.resources.login_no_account
import cardiganapp.feature.auth.presentation.generated.resources.login_password_placeholder
import cardiganapp.feature.auth.presentation.generated.resources.login_title
import com.plcoding.cardiganapp.core.designsystem.components.actions.CardiganButton
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthBannerView
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthFieldKind
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthFooterLink
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthInputField
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthLayout
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthTextField
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoot(
    onCreateAccount: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
            if (action == LoginAction.CreateAccountClick) onCreateAccount()
        },
    )
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    val editable = !state.isLoading
    AuthLayout(
        title = stringResource(Res.string.login_title),
        footer = {
            AuthFooterLink(
                prompt = stringResource(Res.string.login_no_account),
                link = stringResource(Res.string.login_create_one),
                onClick = { onAction(LoginAction.CreateAccountClick) },
                enabled = editable,
            )
        },
    ) {
        state.banner?.let { AuthBannerView(it, modifier = Modifier.fillMaxWidth()) }
        AuthTextField(
            label = stringResource(Res.string.auth_email_label),
            value = state.email,
            onValueChange = { onAction(LoginAction.EmailChange(it)) },
            kind = AuthFieldKind.Email,
            imeAction = ImeAction.Next,
            onFocusChange = { onAction(LoginAction.FocusChange(AuthInputField.Email, it)) },
            enabled = editable,
            isError = state.emailError != null,
            placeholder = stringResource(Res.string.auth_email_placeholder),
            error = state.emailError?.let { stringResource(it.resource) },
        )
        AuthTextField(
            label = stringResource(Res.string.auth_password_label),
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChange(it)) },
            kind = AuthFieldKind.Password,
            imeAction = ImeAction.Done,
            onFocusChange = { onAction(LoginAction.FocusChange(AuthInputField.Password, it)) },
            enabled = editable,
            isError = state.passwordIsError,
            placeholder = stringResource(Res.string.login_password_placeholder),
            error = state.passwordError?.let { stringResource(it.resource) },
            onDone = { onAction(LoginAction.Submit) },
        )
        CardiganButton(
            text = stringResource(if (state.isLoading) Res.string.login_loading else Res.string.auth_log_in),
            onClick = { onAction(LoginAction.Submit) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
