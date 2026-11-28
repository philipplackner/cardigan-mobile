package com.plcoding.cardiganapp.feature.auth.presentation.register

import com.plcoding.cardiganapp.feature.auth.domain.validation.EmailProblem
import com.plcoding.cardiganapp.feature.auth.domain.validation.PasswordCheck
import com.plcoding.cardiganapp.feature.auth.domain.validation.PasswordProblem
import com.plcoding.cardiganapp.feature.auth.domain.validation.RegisterForm
import com.plcoding.cardiganapp.feature.auth.domain.validation.RepeatPasswordProblem
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthBanner
import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthFieldMessage

data class RegisterState(
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val emailBlurred: Boolean = false,
    val passwordBlurred: Boolean = false,
    val repeatPasswordBlurred: Boolean = false,
    val submitAttempted: Boolean = false,
    val isLoading: Boolean = false,
    val banner: AuthBanner? = null,
    val emailAlreadyUsed: Boolean = false,
    val emailServerError: AuthFieldMessage? = null,
    val passwordServerError: AuthFieldMessage? = null,
) {
    private val check get() = RegisterForm.check(email, password, repeatPassword)

    /** The four live rules. They always update, independent of blur (D4). */
    val rules: PasswordCheck get() = check.rules

    val emailError: AuthFieldMessage?
        get() =
            when {
                emailAlreadyUsed -> {
                    AuthFieldMessage.EmailAlreadyUsed
                }

                emailServerError != null -> {
                    emailServerError
                }

                emailBlurred || submitAttempted -> {
                    when (check.email) {
                        EmailProblem.Required -> AuthFieldMessage.EmailRequired
                        EmailProblem.TooLong -> AuthFieldMessage.EmailTooLong
                        EmailProblem.InvalidFormat -> AuthFieldMessage.EmailInvalid
                        null -> null
                    }
                }

                else -> {
                    null
                }
            }

    /** Unmet rules have no message of their own; the field still shows its error state. */
    val passwordError: AuthFieldMessage?
        get() =
            when {
                passwordServerError != null -> {
                    passwordServerError
                }

                passwordBlurred || submitAttempted -> {
                    when (check.password) {
                        PasswordProblem.Required -> AuthFieldMessage.PasswordRequired
                        PasswordProblem.InvalidCharacter -> AuthFieldMessage.PasswordInvalidCharacter
                        PasswordProblem.TooLong -> AuthFieldMessage.PasswordTooLong
                        PasswordProblem.RulesUnmet, null -> null
                    }
                }

                else -> {
                    null
                }
            }

    val passwordIsError: Boolean
        get() = passwordError != null || ((passwordBlurred || submitAttempted) && check.password != null)

    val repeatPasswordError: AuthFieldMessage?
        get() =
            if (!repeatPasswordBlurred && !submitAttempted) {
                null
            } else {
                when (check.repeatPassword) {
                    RepeatPasswordProblem.Required -> AuthFieldMessage.RepeatRequired
                    RepeatPasswordProblem.Mismatch -> AuthFieldMessage.PasswordMismatch
                    null -> null
                }
            }
}
