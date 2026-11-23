package com.plcoding.cardiganapp.feature.auth.domain

import com.plcoding.cardiganapp.core.domain.util.Error

/** Failures of [AuthRepository.login] and [AuthRepository.register]. */
sealed interface AuthError : Error {
    /** 401 `INVALID_CREDENTIALS`. Unknown email and wrong password look the same. */
    data object InvalidCredentials : AuthError

    /** 409 `CONFLICT` with the field error `email`/`ALREADY_USED`. */
    data object EmailAlreadyUsed : AuthError

    /** 429 `RATE_LIMITED`. [retryAfterSeconds] is null when `Retry-After` was missing or unusable. */
    data class RateLimited(val retryAfterSeconds: Int?) : AuthError

    /** 400 `VALIDATION_FAILED` with the reported field errors. */
    data class InvalidFields(val fields: List<AuthFieldError>) : AuthError

    /** No connection, a 5xx, or any error not covered above. */
    data object Unavailable : AuthError
}

data class AuthFieldError(val field: AuthField, val code: FieldErrorCode)

enum class AuthField { Email, Password, Other }

enum class FieldErrorCode { Required, TooShort, TooLong, InvalidCharacter, InvalidFormat, PasswordTooWeak, Other }
