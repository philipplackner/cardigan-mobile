package com.plcoding.cardiganapp.feature.auth.presentation.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.auth.domain.AuthError
import com.plcoding.cardiganapp.feature.auth.domain.AuthField
import com.plcoding.cardiganapp.feature.auth.domain.AuthFieldError
import com.plcoding.cardiganapp.feature.auth.domain.AuthRepository
import com.plcoding.cardiganapp.feature.auth.domain.FieldErrorCode
import com.plcoding.cardiganapp.feature.auth.domain.validation.RegisterForm
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthBanner
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthFieldMessage
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthInputField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Register. Checks the shared email and password rules before sending (D5). On success it stays
 * loading: the session gate navigates once the session is signed in. Passwords never go into the
 * saved state.
 */
class RegisterViewModel(
    private val repository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    private val focusedOnce = mutableSetOf<AuthInputField>()

    fun onAction(action: RegisterAction) {
        if (_state.value.isLoading) return
        when (action) {
            is RegisterAction.EmailChange -> update {
                copy(email = action.email, emailAlreadyUsed = false, emailServerError = null)
            }
            is RegisterAction.PasswordChange -> update { copy(password = action.password, passwordServerError = null) }
            is RegisterAction.RepeatPasswordChange -> update { copy(repeatPassword = action.repeatPassword) }
            is RegisterAction.FocusChange -> onFocusChange(action.field, action.focused)
            RegisterAction.Submit -> submit()
            RegisterAction.LogInClick -> Unit
        }
    }

    private fun onFocusChange(field: AuthInputField, focused: Boolean) {
        if (focused) {
            focusedOnce += field
            return
        }
        if (field !in focusedOnce) return
        val state = _state.value
        when (field) {
            AuthInputField.Email -> if (!state.emailBlurred) update { copy(emailBlurred = true) }
            AuthInputField.Password -> if (!state.passwordBlurred) update { copy(passwordBlurred = true) }
            AuthInputField.RepeatPassword -> if (!state.repeatPasswordBlurred) update { copy(repeatPasswordBlurred = true) }
        }
    }

    private fun submit() {
        update { copy(submitAttempted = true, banner = null) }
        val current = _state.value
        if (!RegisterForm.check(current.email, current.password, current.repeatPassword).isValid) return
        update { copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.register(current.email, current.password)
            if (result is Result.Error) update { copy(isLoading = false).withError(result.error) }
        }
    }

    private fun RegisterState.withError(error: AuthError): RegisterState = when (error) {
        AuthError.EmailAlreadyUsed -> copy(emailAlreadyUsed = true)
        is AuthError.RateLimited -> copy(
            banner = error.retryAfterSeconds?.takeIf { it >= 1 }?.let { AuthBanner.RateLimited(it) } ?: AuthBanner.Unavailable,
        )
        is AuthError.InvalidFields -> {
            val mapped = error.fields.mapNotNull { fieldError -> fieldError.toMessage()?.let { fieldError.field to it } }
            copy(
                emailServerError = mapped.firstOrNull { it.first == AuthField.Email }?.second,
                passwordServerError = mapped.firstOrNull { it.first == AuthField.Password }?.second,
                banner = if (mapped.size < error.fields.size || error.fields.isEmpty()) AuthBanner.Unavailable else null,
            )
        }
        AuthError.InvalidCredentials, AuthError.Unavailable -> copy(banner = AuthBanner.Unavailable)
    }

    /** The server field errors Register maps to the local check messages (spec "Server field errors"). */
    private fun AuthFieldError.toMessage(): AuthFieldMessage? = when (field) {
        AuthField.Email -> when (code) {
            FieldErrorCode.Required -> AuthFieldMessage.EmailRequired
            FieldErrorCode.TooLong -> AuthFieldMessage.EmailTooLong
            FieldErrorCode.InvalidFormat, FieldErrorCode.InvalidCharacter -> AuthFieldMessage.EmailInvalid
            else -> null
        }
        AuthField.Password -> when (code) {
            FieldErrorCode.Required -> AuthFieldMessage.PasswordRequired
            FieldErrorCode.TooLong -> AuthFieldMessage.PasswordTooLong
            FieldErrorCode.InvalidCharacter -> AuthFieldMessage.PasswordInvalidCharacter
            else -> null
        }
        AuthField.Other -> null
    }

    private fun update(change: RegisterState.() -> RegisterState) {
        _state.update(change)
        save()
    }

    private fun save() {
        val state = _state.value
        savedStateHandle[KEY_EMAIL] = state.email
        savedStateHandle[KEY_EMAIL_BLURRED] = state.emailBlurred
        savedStateHandle[KEY_PASSWORD_BLURRED] = state.passwordBlurred
        savedStateHandle[KEY_REPEAT_BLURRED] = state.repeatPasswordBlurred
    }

    private fun restore(): RegisterState = RegisterState(
        email = savedStateHandle[KEY_EMAIL] ?: "",
        emailBlurred = savedStateHandle[KEY_EMAIL_BLURRED] ?: false,
        passwordBlurred = savedStateHandle[KEY_PASSWORD_BLURRED] ?: false,
        repeatPasswordBlurred = savedStateHandle[KEY_REPEAT_BLURRED] ?: false,
    )

    private companion object {
        const val KEY_EMAIL = "register_email"
        const val KEY_EMAIL_BLURRED = "register_email_blurred"
        const val KEY_PASSWORD_BLURRED = "register_password_blurred"
        const val KEY_REPEAT_BLURRED = "register_repeat_blurred"
    }
}
