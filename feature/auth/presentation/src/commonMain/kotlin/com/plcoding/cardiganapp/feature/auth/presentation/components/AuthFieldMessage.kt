package com.plcoding.cardiganapp.feature.auth.presentation.components

import cardiganapp.feature.auth.presentation.generated.resources.Res
import cardiganapp.feature.auth.presentation.generated.resources.auth_error_email_required
import cardiganapp.feature.auth.presentation.generated.resources.auth_error_password_required
import org.jetbrains.compose.resources.StringResource

/** A message shown under an auth field. */
enum class AuthFieldMessage(val resource: StringResource) {
    EmailRequired(Res.string.auth_error_email_required),
    PasswordRequired(Res.string.auth_error_password_required),
}
