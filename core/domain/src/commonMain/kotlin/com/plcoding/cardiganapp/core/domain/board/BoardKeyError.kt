package com.plcoding.cardiganapp.core.domain.board

import com.plcoding.cardiganapp.core.domain.util.Error

/**
 * Board key field errors, from [BoardKeyRule] or from the server's `key` field error codes.
 * Server codes other than `REQUIRED`, `TOO_SHORT` and `TOO_LONG` map to [InvalidFormat].
 */
enum class BoardKeyError : Error {
    Required,
    TooShort,
    TooLong,
    InvalidFormat,
}
