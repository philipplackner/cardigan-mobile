package com.plcoding.cardiganapp.feature.auth.presentation.login

import com.plcoding.cardiganapp.feature.auth.domain.validation.LoginForm
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthBanner
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthFieldMessage

data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailBlurred: Boolean = false,
    val passwordBlurred: Boolean = false,
    val submitAttempted: Boolean = false,
    val isLoading: Boolean = false,
    val banner: AuthBanner? = null,
    val emailServerRequired: Boolean = false,
    val passwordServerRequired: Boolean = false,
) {
    private val check get() = LoginForm.check(email, password)

    /** Shown after the first blur or a submit (D4), or when the server reported the field empty. */
    val emailError: AuthFieldMessage?
        get() = when {
            emailServerRequired -> AuthFieldMessage.EmailRequired
            (emailBlurred || submitAttempted) && check.email != null -> AuthFieldMessage.EmailRequired
            else -> null
        }

    val passwordError: AuthFieldMessage?
        get() = when {
            passwordServerRequired -> AuthFieldMessage.PasswordRequired
            (passwordBlurred || submitAttempted) && check.password != null -> AuthFieldMessage.PasswordRequired
            else -> null
        }

    /** Wrong credentials put only the password field in its error state (D10). */
    val passwordIsError: Boolean get() = passwordError != null || banner == AuthBanner.WrongCredentials
}
