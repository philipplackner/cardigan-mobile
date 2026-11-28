package com.plcoding.cardiganapp.feature.auth.presentation.register

import com.plcoding.cardiganapp.feature.auth.presentation.components.AuthInputField

sealed interface RegisterAction {
    data class EmailChange(
        val email: String,
    ) : RegisterAction

    data class PasswordChange(
        val password: String,
    ) : RegisterAction

    data class RepeatPasswordChange(
        val repeatPassword: String,
    ) : RegisterAction

    data class FocusChange(
        val field: AuthInputField,
        val focused: Boolean,
    ) : RegisterAction

    data object Submit : RegisterAction

    data object LogInClick : RegisterAction
}
