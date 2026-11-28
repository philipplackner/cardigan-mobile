package com.plcoding.cardiganapp.feature.auth.presentation.login

import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthInputField

sealed interface LoginAction {
    data class EmailChange(
        val email: String,
    ) : LoginAction

    data class PasswordChange(
        val password: String,
    ) : LoginAction

    data class FocusChange(
        val field: AuthInputField,
        val focused: Boolean,
    ) : LoginAction

    data object Submit : LoginAction

    data object CreateAccountClick : LoginAction
}
