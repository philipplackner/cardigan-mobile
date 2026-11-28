package com.plcoding.cardiganapp.feature.boards.domain

import com.plcoding.cardiganapp.core.domain.board.BoardKeyError
import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Error

/** Why a create, rename or delete failed. */
sealed interface BoardSaveError : Error {
    /** No network, a 5xx response or an error code without its own handling. */
    data class Network(
        val error: DataError.Network,
    ) : BoardSaveError

    /** Server field errors. A field without an error is null. */
    data class InvalidFields(
        val name: NameFieldError? = null,
        val key: KeyFieldError? = null,
    ) : BoardSaveError

    data object NotFound : BoardSaveError
}

/** A server field error for the board name. [Invalid] covers `INVALID_CHARACTER` and unknown codes. */
sealed interface NameFieldError {
    data object Required : NameFieldError

    data class TooLong(
        val max: Int,
    ) : NameFieldError

    data object Invalid : NameFieldError
}

/** A server field error for the board key. [Invalid] covers every code without its own text. */
sealed interface KeyFieldError {
    data class Rule(
        val error: BoardKeyError,
    ) : KeyFieldError

    data object AlreadyUsed : KeyFieldError

    data object Invalid : KeyFieldError
}
