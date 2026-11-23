package com.plcoding.cardiganapp.feature.auth.presentation.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
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
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthBanner
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

private val previewStates = listOf(
    LoginState(),
    LoginState(email = "philipp@example.com", password = "wrong password", banner = AuthBanner.WrongCredentials),
    LoginState(banner = AuthBanner.SessionExpired),
    LoginState(email = "philipp@example.com", password = "secret", banner = AuthBanner.RateLimited(12)),
    LoginState(email = "philipp@example.com", password = "secret", banner = AuthBanner.Unavailable),
    LoginState(email = "philipp@example.com", password = "correct horse", isLoading = true, submitAttempted = true),
    LoginState(submitAttempted = true),
)

@Composable
private fun LoginPreview(index: Int, windowClass: WindowClass, darkTheme: Boolean = false) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        LoginScreen(state = previewStates[index], onAction = {})
    }
}

@Preview(name = "Log in · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun LoginPhonePreview() = LoginPreview(0, WindowClass.Phone)

@Preview(name = "Log in · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun LoginPhoneDarkPreview() = LoginPreview(0, WindowClass.Phone, darkTheme = true)

@Preview(name = "Log in · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun LoginDesktopPreview() = LoginPreview(0, WindowClass.Desktop)

@Preview(name = "Log in · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun LoginDesktopDarkPreview() = LoginPreview(0, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Wrong credentials · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun WrongCredentialsPhonePreview() = LoginPreview(1, WindowClass.Phone)

@Preview(name = "Wrong credentials · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun WrongCredentialsDesktopDarkPreview() = LoginPreview(1, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Session expired · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun SessionExpiredPhonePreview() = LoginPreview(2, WindowClass.Phone)

@Preview(name = "Session expired · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun SessionExpiredDesktopPreview() = LoginPreview(2, WindowClass.Desktop)

@Preview(name = "Rate limited · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun RateLimitedPhoneDarkPreview() = LoginPreview(3, WindowClass.Phone, darkTheme = true)

@Preview(name = "Rate limited · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun RateLimitedDesktopPreview() = LoginPreview(3, WindowClass.Desktop)

@Preview(name = "Unavailable · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun UnavailablePhonePreview() = LoginPreview(4, WindowClass.Phone)

@Preview(name = "Unavailable · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun UnavailableDesktopDarkPreview() = LoginPreview(4, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Logging in · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun LoggingInPhonePreview() = LoginPreview(5, WindowClass.Phone)

@Preview(name = "Logging in · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun LoggingInDesktopDarkPreview() = LoginPreview(5, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Empty submit · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun EmptySubmitPhonePreview() = LoginPreview(6, WindowClass.Phone)
