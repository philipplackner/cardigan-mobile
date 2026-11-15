package com.plcoding.cardiganapp.feature.transfer.domain

import com.plcoding.cardiganapp.core.domain.util.Error

sealed interface ExportError : Error {
    data object BoardNotFound : ExportError

    /** Network failure, unknown outcome, 5xx or any unmapped error. */
    data object Failed : ExportError
}

sealed interface ImportError : Error {
    data object FileTooLarge : ImportError
    data object InvalidFile : ImportError

    /** 409 with the field error `key`/`ALREADY_USED`. */
    data object KeyAlreadyUsed : ImportError

    /** 400 with a `key` field error of `TOO_SHORT`, `TOO_LONG` or `INVALID_FORMAT`. */
    data class KeyInvalid(val rule: BoardKeyError) : ImportError

    /** Network failure, unknown outcome, 5xx or any unmapped error. */
    data object Failed : ImportError
}

enum class BoardKeyError {
    TooShort,
    TooLong,
    InvalidFormat,
}
