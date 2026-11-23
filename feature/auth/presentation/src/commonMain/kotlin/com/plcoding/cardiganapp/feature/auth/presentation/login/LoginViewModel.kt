package com.plcoding.cardiganapp.feature.auth.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.auth.domain.AuthError
import com.plcoding.cardiganapp.feature.auth.domain.AuthField
import com.plcoding.cardiganapp.feature.auth.domain.AuthRepository
import com.plcoding.cardiganapp.feature.auth.domain.FieldErrorCode
import com.plcoding.cardiganapp.feature.auth.domain.validation.LoginForm
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthBanner
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthInputField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Log in. Checks only for empty fields (D5). On success it stays loading: the session gate
 * navigates once the session is signed in. Passwords never go into the saved state.
 *
 * [sessionExpired] is `LoginRoute.sessionExpired`. It is read once; after a process restart the
 * saved banner state wins, so a banner the user already cleared doesn't come back.
 */
class LoginViewModel(
    private val sessionExpired: Boolean,
    private val repository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val focusedOnce = mutableSetOf<AuthInputField>()

    fun onAction(action: LoginAction) {
        if (_state.value.isLoading) return
        when (action) {
            is LoginAction.EmailChange -> update {
                copy(email = action.email, emailServerRequired = false, banner = banner.clearedByEdit())
            }
            is LoginAction.PasswordChange -> update {
                copy(password = action.password, passwordServerRequired = false, banner = banner.clearedByEdit())
            }
            is LoginAction.FocusChange -> onFocusChange(action.field, action.focused)
            LoginAction.Submit -> submit()
            LoginAction.CreateAccountClick -> update {
                copy(banner = banner.takeUnless { it == AuthBanner.SessionExpired })
            }
        }
    }

    private fun onFocusChange(field: AuthInputField, focused: Boolean) {
        if (focused) {
            focusedOnce += field
            return
        }
        if (field !in focusedOnce) return
        when (field) {
            AuthInputField.Email -> if (!_state.value.emailBlurred) update { copy(emailBlurred = true) }
            AuthInputField.Password -> if (!_state.value.passwordBlurred) update { copy(passwordBlurred = true) }
            AuthInputField.RepeatPassword -> Unit
        }
    }

    private fun submit() {
        update { copy(submitAttempted = true, banner = null) }
        val current = _state.value
        if (!LoginForm.check(current.email, current.password).isValid) return
        update { copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.login(current.email, current.password)
            if (result is Result.Error) update { copy(isLoading = false).withError(result.error) }
        }
    }

    private fun LoginState.withError(error: AuthError): LoginState = when (error) {
        AuthError.InvalidCredentials -> copy(banner = AuthBanner.WrongCredentials)
        is AuthError.RateLimited -> copy(
            banner = error.retryAfterSeconds?.takeIf { it >= 1 }?.let { AuthBanner.RateLimited(it) } ?: AuthBanner.Unavailable,
        )
        is AuthError.InvalidFields -> {
            val emailRequired = error.fields.any { it.field == AuthField.Email && it.code == FieldErrorCode.Required }
            val passwordRequired = error.fields.any { it.field == AuthField.Password && it.code == FieldErrorCode.Required }
            val unmapped = error.fields.any { it.code != FieldErrorCode.Required || it.field == AuthField.Other }
            copy(
                emailServerRequired = emailRequired,
                passwordServerRequired = passwordRequired,
                banner = if (unmapped || error.fields.isEmpty()) AuthBanner.Unavailable else null,
            )
        }
        AuthError.EmailAlreadyUsed, AuthError.Unavailable -> copy(banner = AuthBanner.Unavailable)
    }

    private fun AuthBanner?.clearedByEdit(): AuthBanner? = takeUnless { it == AuthBanner.WrongCredentials }

    private fun update(change: LoginState.() -> LoginState) {
        _state.update(change)
        save()
    }

    private fun save() {
        val state = _state.value
        savedStateHandle[KEY_EMAIL] = state.email
        savedStateHandle[KEY_EMAIL_BLURRED] = state.emailBlurred
        savedStateHandle[KEY_PASSWORD_BLURRED] = state.passwordBlurred
        savedStateHandle[KEY_SESSION_EXPIRED] = state.banner == AuthBanner.SessionExpired
    }

    private fun restore(): LoginState {
        val sessionExpired = savedStateHandle.get<Boolean>(KEY_SESSION_EXPIRED) ?: sessionExpired
        return LoginState(
            email = savedStateHandle[KEY_EMAIL] ?: "",
            emailBlurred = savedStateHandle[KEY_EMAIL_BLURRED] ?: false,
            passwordBlurred = savedStateHandle[KEY_PASSWORD_BLURRED] ?: false,
            banner = if (sessionExpired) AuthBanner.SessionExpired else null,
        )
    }

    private companion object {
        const val KEY_EMAIL = "login_email"
        const val KEY_EMAIL_BLURRED = "login_email_blurred"
        const val KEY_PASSWORD_BLURRED = "login_password_blurred"
        const val KEY_SESSION_EXPIRED = "login_session_expired"
    }
}
