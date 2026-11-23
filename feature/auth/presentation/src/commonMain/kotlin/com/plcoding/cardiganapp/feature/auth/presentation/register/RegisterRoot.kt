package com.plcoding.cardiganapp.feature.auth.presentation.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
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
            text = stringResource(if (state.isLoading) Res.string.register_loading else Res.string.register_create_account),
            onClick = { onAction(RegisterAction.Submit) },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The Figma "Password rules" grid: two columns, 12+ characters and Lowercase on top. */
@Composable
private fun PasswordRules(rules: PasswordCheck) {
    val rows = listOf(
        listOf(Res.string.register_rule_length to rules.hasMinLength, Res.string.register_rule_lowercase to rules.hasLowercase),
        listOf(Res.string.register_rule_uppercase to rules.hasUppercase, Res.string.register_rule_digit to rules.hasDigit),
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

private val previewStates = listOf(
    RegisterState(),
    RegisterState(
        email = "philipp@example.com",
        password = "Correct horse",
        repeatPassword = "Correct hors",
        passwordBlurred = true,
        repeatPasswordBlurred = true,
    ),
    RegisterState(
        email = "philipp@example.com",
        password = "Correct horse 7",
        repeatPassword = "Correct horse 7",
        emailAlreadyUsed = true,
    ),
    RegisterState(submitAttempted = true),
    RegisterState(
        email = "philipp@example.com",
        password = "Correct horse 7",
        repeatPassword = "Correct horse 7",
        submitAttempted = true,
        isLoading = true,
    ),
)

@Composable
private fun RegisterPreview(index: Int, windowClass: WindowClass, darkTheme: Boolean = false) {
    AppTheme(darkTheme = darkTheme, windowClass = windowClass) {
        RegisterScreen(state = previewStates[index], onAction = {})
    }
}

@Preview(name = "Register · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun RegisterPhonePreview() = RegisterPreview(0, WindowClass.Phone)

@Preview(name = "Register · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun RegisterPhoneDarkPreview() = RegisterPreview(0, WindowClass.Phone, darkTheme = true)

@Preview(name = "Register · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun RegisterDesktopPreview() = RegisterPreview(0, WindowClass.Desktop)

@Preview(name = "Register · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun RegisterDesktopDarkPreview() = RegisterPreview(0, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Live rules and mismatch · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun MismatchPhonePreview() = RegisterPreview(1, WindowClass.Phone)

@Preview(name = "Live rules and mismatch · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun MismatchDesktopDarkPreview() = RegisterPreview(1, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Email already registered · Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun EmailUsedDesktopPreview() = RegisterPreview(2, WindowClass.Desktop)

@Preview(name = "Email already registered · Phone · Dark", widthDp = 390, heightDp = 844)
@Composable
private fun EmailUsedPhoneDarkPreview() = RegisterPreview(2, WindowClass.Phone, darkTheme = true)

@Preview(name = "Empty submit · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun EmptySubmitPhonePreview() = RegisterPreview(3, WindowClass.Phone)

@Preview(name = "Empty submit · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun EmptySubmitDesktopDarkPreview() = RegisterPreview(3, WindowClass.Desktop, darkTheme = true)

@Preview(name = "Creating account · Phone", widthDp = 390, heightDp = 844)
@Composable
private fun CreatingPhonePreview() = RegisterPreview(4, WindowClass.Phone)

@Preview(name = "Creating account · Desktop · Dark", widthDp = 1440, heightDp = 900)
@Composable
private fun CreatingDesktopDarkPreview() = RegisterPreview(4, WindowClass.Desktop, darkTheme = true)
