package com.plcoding.cardiganapp.feature.auth.presentation.components

import cardiganapp.feature.auth.presentation.generated.resources.Res
import cardiganapp.feature.auth.presentation.generated.resources.auth_error_email_required
import cardiganapp.feature.auth.presentation.generated.resources.auth_error_password_required
import cardiganapp.feature.auth.presentation.generated.resources.register_error_email_invalid
import cardiganapp.feature.auth.presentation.generated.resources.register_error_email_too_long
import cardiganapp.feature.auth.presentation.generated.resources.register_error_email_used
import cardiganapp.feature.auth.presentation.generated.resources.register_error_password_invalid_character
import cardiganapp.feature.auth.presentation.generated.resources.register_error_password_mismatch
import cardiganapp.feature.auth.presentation.generated.resources.register_error_password_too_long
import cardiganapp.feature.auth.presentation.generated.resources.register_error_repeat_required
import org.jetbrains.compose.resources.StringResource

/** A message shown under an auth field. */
enum class AuthFieldMessage(val resource: StringResource) {
    EmailRequired(Res.string.auth_error_email_required),
    EmailInvalid(Res.string.register_error_email_invalid),
    EmailTooLong(Res.string.register_error_email_too_long),
    EmailAlreadyUsed(Res.string.register_error_email_used),
    PasswordRequired(Res.string.auth_error_password_required),
    PasswordTooLong(Res.string.register_error_password_too_long),
    PasswordInvalidCharacter(Res.string.register_error_password_invalid_character),
    RepeatRequired(Res.string.register_error_repeat_required),
    PasswordMismatch(Res.string.register_error_password_mismatch),
}
